package org.example.features.taxreturn.retention;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.entity.BusinessProfile;
import org.example.entity.EvidenceFile;
import org.example.repository.EvidenceFileRepository;
import org.example.entity.FiscalYear;
import org.example.entity.JournalEntry;
import org.example.entity.RetentionRecord;
import org.example.features.entry.EntryNotFoundException;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.RetentionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetentionServiceImpl implements RetentionService {
    private static final Set<String> RECORD_KINDS = Set.of("TRANSACTION", "BOOK", "EVIDENCE");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Logger AUDIT = LoggerFactory.getLogger("audit.retention");

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final RetentionRecordRepository retentionRecordRepository;
    private final EvidenceFileRepository evidenceFileRepository;

    public RetentionServiceImpl(BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository, JournalEntryRepository journalEntryRepository,
            RetentionRecordRepository retentionRecordRepository, EvidenceFileRepository evidenceFileRepository) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.retentionRecordRepository = retentionRecordRepository;
        this.evidenceFileRepository = evidenceFileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RetentionPageData load(Long appUserId, Integer taxYear) {
        return search(appUserId, taxYear, RetentionSearch.all(), LocalDate.now(TOKYO));
    }

    @Override
    @Transactional(readOnly = true)
    public RetentionPageData search(Long appUserId, Integer taxYear, RetentionSearch search, LocalDate today) {
        FiscalYear fiscalYear = resolveOwnedFiscalYear(appUserId, taxYear);
        int year = fiscalYear.getTaxYear();
        List<JournalEntry> journalEntries = journalEntryRepository
                .findAllWithLinesByFiscalYearId(fiscalYear.getId());
        List<RetentionRecord> all = retentionRecordRepository
                .findByFiscalYearIdOrderByRetentionDeadlineAscIdAsc(fiscalYear.getId());
        Map<Long, EvidenceFile> files = all.isEmpty() ? Map.of()
                : evidenceFileRepository.findByRetentionRecordIdIn(all.stream().map(RetentionRecord::getId).toList())
                        .stream().collect(Collectors.toMap(f -> f.getRetentionRecord().getId(), f -> f));
        List<RetentionRecordView> views = all.stream()
                .map(record -> {
                    JournalEntry entry = record.getJournalEntry();
                    EvidenceFile file = files.get(record.getId());
                    LocalDate effective = RetentionRules.effectiveDeadline(record.getRetentionDeadline(), year);
                    RetentionRecordView.Guidance guidance = new RetentionRecordView.Guidance(
                            record.getRetentionRuleCode(), record.getDocumentDate(), record.isConsumptionTaxApplies(),
                            record.isLegalHold(), record.getLegalHoldReason(), record.getLegalHoldAt(), effective,
                            ChronoUnit.DAYS.between(today, effective));
                    return new RetentionRecordView(record.getId(), record.getRecordKind(),
                            record.getDocumentType(), record.getTitle(), entry == null ? null : entry.getId(),
                            entry == null ? null : entry.getSummary(),
                            record.getRetentionStartDate(), record.getRetentionDeadline(), record.getLegalBasis(),
                            today.isAfter(effective),
                            file == null ? null : new RetentionRecordView.FileInfo(file.getOriginalFileName(),
                                    file.getMimeType(), file.getFileSize(), file.getUploadedAt(),
                                    file.getVerificationStatus(), file.getLastVerifiedAt()),
                            guidance);
                })
                .toList();
        List<RetentionRecordView> records = views.stream().filter(view -> matches(view, search)).toList();
        int expired = (int) views.stream().filter(RetentionRecordView::isExpired).count();
        int alerts = (int) views.stream().filter(v -> v.getFile() != null && v.getFile().isAlert()).count();
        List<RetentionJournalOption> options = journalEntries.stream()
                .map(entry -> new RetentionJournalOption(entry.getId(), entry.getEntryDate(), entry.getSummary()))
                .toList();
        RetentionPageData data = new RetentionPageData(records, options, search, views.size(), expired, alerts);
        data.setGuidance((int) views.stream().filter(v -> "DUE_SOON".equals(v.getDeadlineStatus())).count(),
                (int) views.stream().filter(v -> v.getGuidance().legalHold()).count(),
                (int) views.stream().filter(v -> !v.getGuidance().isRuleApplied()).count(),
                RetentionRules.isBlue(fiscalYear.getBusinessProfile().getFilingType()),
                RetentionRules.supportsYear(year), RetentionRules.minimumDeadline(year));
        return data;
    }

    private boolean matches(RetentionRecordView view, RetentionSearch search) {
        if (!search.getRecordKind().isEmpty() && !search.getRecordKind().equals(view.getRecordKind())) {
            return false;
        }
        boolean statusMatches = switch (search.getStatus()) {
            case "ACTIVE" -> !view.isExpired();
            case "EXPIRED" -> view.isExpired();
            case "DUE_SOON" -> "DUE_SOON".equals(view.getDeadlineStatus());
            case "HELD" -> view.getGuidance().legalHold();
            case "MANUAL_RULE" -> !view.getGuidance().isRuleApplied();
            case "WITH_FILE" -> view.isHasFile();
            case "WITHOUT_FILE" -> !view.isHasFile();
            case "FILE_ALERT" -> view.isHasFile() && view.getFile().isAlert();
            default -> true;
        };
        return statusMatches && search.matchesText(view.getDocumentType(), view.getTitle(), view.getLegalBasis(),
                view.getJournalSummary(), view.isHasFile() ? view.getFile().getOriginalFileName() : null,
                view.getGuidance().legalHoldReason());
    }

    @Override
    @Transactional
    public List<String> add(Long appUserId, Integer taxYear, RetentionRecordForm form, LocalDate today) {
        List<String> errors = validate(form);
        if (!errors.isEmpty()) {
            return errors;
        }

        FiscalYear fiscalYear = resolveOwnedFiscalYear(appUserId, taxYear);
        JournalEntry journalEntry = null;
        if ("TRANSACTION".equals(form.getRecordKind()) || "EVIDENCE".equals(form.getRecordKind())) {
            journalEntry = journalEntryRepository.findAllWithLinesByFiscalYearId(fiscalYear.getId()).stream()
                    .filter(entry -> entry.getId().equals(form.getJournalEntryId()))
                    .findFirst()
                    .orElse(null);
            if (journalEntry == null) {
                return List.of("対象年度の仕訳を選択してください。");
            }
        } else if (form.getJournalEntryId() != null) {
            return List.of("帳簿全体の記録には個別仕訳を指定できません。");
        }

        int year = fiscalYear.getTaxYear();
        RetentionRecord record = new RetentionRecord();
        String ruleCode = ruleCode(form);
        if (RetentionRules.MANUAL.equals(ruleCode)) {
            LocalDate deadline = parseDate(form.getRetentionDeadline());
            LocalDate floor = RetentionRules.minimumDeadline(year);
            if (deadline.isBefore(floor)) {
                return List.of("保存期限が対象年分の最低保存期限（" + floor + "）より前です。"
                        + "法令上の期限を公式資料で確認し、最低保存期限以降の日付を入力してください。");
            }
            record.setRetentionStartDate(parseDate(form.getRetentionStartDate()));
            record.setRetentionDeadline(deadline);
            record.setLegalBasis(form.getLegalBasis().trim());
            record.setConsumptionTaxApplies(form.isConsumptionTax());
        } else {
            RetentionRules.Category category = RetentionRules.category(ruleCode).orElseThrow();
            if (!RetentionRules.supportsYear(year)) {
                return List.of(year + "年分は保存期限ルールの確認対象外です（対象：" + RetentionRules.MIN_TAX_YEAR + "〜"
                        + RetentionRules.MAX_TAX_YEAR + "年分）。手入力で公式資料に基づく期限を登録してください。");
            }
            if (category.isBook() != "BOOK".equals(form.getRecordKind())) {
                return List.of(category.isBook() ? "帳簿のルールは記録区分「帳簿」でのみ選択できます。"
                        : "書類のルールは記録区分「取引」「証憑」でのみ選択できます。");
            }
            LocalDate documentDate = null;
            if (!category.isBook()) {
                documentDate = parseDateOrNull(form.getDocumentDate());
                if (!RetentionRules.validDocumentDate(documentDate, today)) {
                    return List.of("書類の作成・受領日を正しく入力してください（2000年1月1日以降、本日以前）。");
                }
            }
            RetentionRules.Result result = RetentionRules.calculate(category,
                    RetentionRules.isBlue(fiscalYear.getBusinessProfile().getFilingType()), year, documentDate,
                    form.isConsumptionTax());
            record.setRetentionStartDate(result.startDate());
            record.setRetentionDeadline(result.deadline());
            record.setLegalBasis(result.legalBasis());
            record.setDocumentDate(documentDate);
            record.setConsumptionTaxApplies(form.isConsumptionTax());
            record.setRuleVersion(RetentionRules.RULE_VERSION);
        }
        record.setRetentionRuleCode(ruleCode);
        record.setFiscalYear(fiscalYear);
        record.setJournalEntry(journalEntry);
        record.setRecordKind(form.getRecordKind());
        record.setDocumentType(form.getDocumentType().trim());
        record.setTitle(form.getTitle().trim());
        retentionRecordRepository.save(record);
        return List.of();
    }

    @Override
    @Transactional
    public List<String> placeLegalHold(Long appUserId, Long recordId, String reason) {
        RetentionRecord record = ownedRecord(appUserId, recordId);
        String trimmed = reason == null ? "" : reason.strip();
        if (trimmed.isEmpty() || trimmed.length() > 200) {
            return List.of("保全の理由を200文字以内で入力してください（例：税務調査対応、更正の請求期間中）。");
        }
        if (record.isLegalHold()) {
            return List.of("この記録はすでに保全中です。");
        }
        record.setLegalHold(true);
        record.setLegalHoldReason(trimmed);
        record.setLegalHoldAt(LocalDateTime.now(TOKYO));
        audit("hold", appUserId, recordId);
        return List.of();
    }

    @Override
    @Transactional
    public List<String> releaseLegalHold(Long appUserId, Long recordId) {
        RetentionRecord record = ownedRecord(appUserId, recordId);
        if (!record.isLegalHold()) {
            return List.of("この記録は保全されていません。");
        }
        record.setLegalHold(false);
        record.setLegalHoldReason(null);
        record.setLegalHoldAt(null);
        audit("release", appUserId, recordId);
        return List.of();
    }

    private RetentionRecord ownedRecord(Long appUserId, Long recordId) {
        Optional<RetentionRecord> record = recordId == null ? Optional.empty()
                : retentionRecordRepository.findOwned(recordId, appUserId);
        return record.orElseThrow(() -> new EntryNotFoundException("台帳記録が見つかりません。"));
    }

    private void audit(String action, Long appUserId, Long recordId) {
        AUDIT.info("retention action={} user={} record={} result=success", action, appUserId, recordId);
    }

    private static String ruleCode(RetentionRecordForm form) {
        return form.getRuleCode() == null || form.getRuleCode().isBlank() ? RetentionRules.MANUAL : form.getRuleCode();
    }

    private FiscalYear resolveOwnedFiscalYear(Long appUserId, Integer taxYear) {
        BusinessProfile profile = businessProfileRepository.findByAppUserId(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("対象年度が見つかりません。"));
        return fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId()).stream()
                .filter(year -> taxYear == null || year.getTaxYear().equals(taxYear))
                .findFirst()
                .orElseThrow(() -> new EntryNotFoundException("対象年度が見つかりません。"));
    }

    private List<String> validate(RetentionRecordForm form) {
        if (form == null || form.getRecordKind() == null || !RECORD_KINDS.contains(form.getRecordKind())) {
            return List.of("保存対象の区分が不正です。");
        }
        String ruleCode = ruleCode(form);
        boolean manual = RetentionRules.MANUAL.equals(ruleCode);
        if (!manual && RetentionRules.category(ruleCode).isEmpty()) {
            return List.of("保存期限ルールの区分が不正です。");
        }
        if (blankOrTooLong(form.getDocumentType(), 100) || blankOrTooLong(form.getTitle(), 200)
                || (manual && blankOrTooLong(form.getLegalBasis(), 500))) {
            return List.of("書類種別・名称・保存根拠を入力してください。各項目の文字数上限を超えています。");
        }
        if (("TRANSACTION".equals(form.getRecordKind()) || "EVIDENCE".equals(form.getRecordKind()))
                && form.getJournalEntryId() == null) {
            return List.of("取引または証憑の記録には、対象年度の仕訳を指定してください。");
        }
        if (manual) {
            LocalDate start = parseDateOrNull(form.getRetentionStartDate());
            LocalDate deadline = parseDateOrNull(form.getRetentionDeadline());
            if (start == null || deadline == null || deadline.isBefore(start)) {
                return List.of("保存起算日と期限を正しく入力してください。期限は起算日以降にしてください。");
            }
        }
        return List.of();
    }

    private boolean blankOrTooLong(String value, int maxLength) {
        return value == null || value.isBlank() || value.trim().length() > maxLength;
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value);
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
