package org.example.features.taxreturn.closing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.entity.FiscalYearClosingEvent;
import org.example.entity.ReportOutputRecord;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.export.TaxReturnPdfRenderer;
import org.example.features.taxreturn.incometax.BlueReturnEdition;
import org.example.features.taxreturn.service.TaxReturnService;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearClosingEventRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.ReportOutputRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 年度の締め・締めの取消（訂正のための再オープン）・再締め時の訂正理由と、出力帳票の生成記録を管理する。
 * 締め時点の会計データ指紋を保存し、確定済み年度の変化や出力済み帳票と現データの不一致を検知できるようにする。
 */
@Service
public class YearClosingService {
    public static final int MAX_REASON_LENGTH = 200;
    public static final int MAX_OUTPUTS_SHOWN = 30;
    private static final String OPEN = "open";
    private static final String CLOSED = "closed";
    private static final Logger AUDIT = LoggerFactory.getLogger("audit.year-closing");
    private static final Map<String, String> OUTPUT_LABELS = Map.ofEntries(
            Map.entry("pdf:return", "青色申告決算書PDF"),
            Map.entry("pdf:balance-sheet", "貸借対照表PDF"),
            Map.entry("pdf:profit-loss", "損益計算書PDF"),
            Map.entry("pdf:journal", "仕訳帳PDF"),
            Map.entry("pdf:ledger", "総勘定元帳PDF"),
            Map.entry("pdf:final-return", "確定申告書PDF"),
            Map.entry("pdf:consumption", "消費税計算書PDF"),
            Map.entry("pdf:all", "PDF一括（ZIP）"),
            Map.entry("csv:etax", "e-Tax転記補助CSV"),
            Map.entry("csv:books", "帳簿一括CSV"),
            Map.entry("csv:fixed-assets", "固定資産台帳CSV"));

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalYearClosingEventRepository eventRepository;
    private final ReportOutputRecordRepository outputRepository;
    private final TaxReturnService taxReturnService;

    public YearClosingService(BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository, FiscalYearClosingEventRepository eventRepository,
            ReportOutputRecordRepository outputRepository, TaxReturnService taxReturnService) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.eventRepository = eventRepository;
        this.outputRepository = outputRepository;
        this.taxReturnService = taxReturnService;
    }

    public static boolean isKnownOutputType(String type) {
        return OUTPUT_LABELS.containsKey(type);
    }

    /** 年度を締める。過去に締めたことがある年度（訂正後の再締め）は訂正理由を必須とする。 */
    @Transactional
    public List<String> close(Long appUserId, Integer year, boolean warningsConfirmed, String reason,
            LocalDateTime now) {
        FiscalYear fiscalYear = lockOwnedYear(appUserId, year);
        if (CLOSED.equals(fiscalYear.getStatus())) {
            return List.of("この年度は既に締めています。");
        }
        List<String> openPrior = siblings(fiscalYear).stream()
                .filter(y -> y.getTaxYear() < fiscalYear.getTaxYear() && !CLOSED.equals(y.getStatus()))
                .map(y -> y.getTaxYear() + "年分").toList();
        if (!openPrior.isEmpty()) {
            return List.of("前年以前の未締め年度（" + String.join("・", openPrior) + "）を先に締めてください。"
                    + "前年の残高が翌年の期首残高に繰り越されるためです。");
        }
        List<FiscalYearClosingEvent> history = eventRepository.findByFiscalYearIdOrderByIdAsc(fiscalYear.getId());
        boolean correction = history.stream().anyMatch(e -> FiscalYearClosingEvent.CLOSE.equals(e.getEventType()));
        String normalized = normalize(reason);
        if (normalized != null && normalized.length() > MAX_REASON_LENGTH) {
            return List.of("理由は" + MAX_REASON_LENGTH + "文字以内で入力してください。");
        }
        if (correction && normalized == null) {
            return List.of("訂正後の再締めでは訂正理由（" + MAX_REASON_LENGTH + "文字以内）を入力してください。");
        }
        TaxReturnPageData page = taxReturnService.load(appUserId, fiscalYear.getTaxYear(), null);
        if (!page.getErrors().isEmpty()) {
            List<String> messages = new ArrayList<>();
            messages.add("エラーがあるため締められません。エラーチェックの内容をご確認ください。");
            messages.addAll(page.getErrors());
            audit(appUserId, fiscalYear, "close", null, "rejected-errors");
            return messages;
        }
        if (!page.getWarnings().isEmpty() && !warningsConfirmed) {
            audit(appUserId, fiscalYear, "close", null, "confirmation-required");
            return List.of("警告があります。内容を確認し「警告を確認した」にチェックしてから締めてください。");
        }
        int version = nextVersion(history);
        eventRepository.save(event(fiscalYear, appUserId, FiscalYearClosingEvent.CLOSE, version, normalized,
                YearSnapshot.of(page), now));
        fiscalYear.setStatus(CLOSED);
        fiscalYear.setClosedAt(now);
        audit(appUserId, fiscalYear, "close", version, correction ? "closed-correction" : "closed");
        return List.of();
    }

    /** 締めを取消して年度を再び編集可能にする（訂正のための再オープン）。理由を必須とする。 */
    @Transactional
    public List<String> reopen(Long appUserId, Integer year, String reason, LocalDateTime now) {
        FiscalYear fiscalYear = lockOwnedYear(appUserId, year);
        if (!CLOSED.equals(fiscalYear.getStatus())) {
            return List.of("この年度は締めていません。");
        }
        String normalized = normalize(reason);
        if (normalized == null || normalized.length() > MAX_REASON_LENGTH) {
            return List.of("締めの取消理由（" + MAX_REASON_LENGTH + "文字以内）を入力してください。");
        }
        List<String> closedLater = siblings(fiscalYear).stream()
                .filter(y -> y.getTaxYear() > fiscalYear.getTaxYear() && CLOSED.equals(y.getStatus()))
                .map(y -> y.getTaxYear() + "年分").toList();
        if (!closedLater.isEmpty()) {
            return List.of("翌年以降の締め済み年度（" + String.join("・", closedLater) + "）を先に取消してください。");
        }
        List<FiscalYearClosingEvent> history = eventRepository.findByFiscalYearIdOrderByIdAsc(fiscalYear.getId());
        int version = history.stream()
                .filter(e -> FiscalYearClosingEvent.CLOSE.equals(e.getEventType()))
                .map(FiscalYearClosingEvent::getCloseVersion)
                .max(Integer::compare)
                .orElse(nextVersion(history));
        TaxReturnPageData page = taxReturnService.load(appUserId, fiscalYear.getTaxYear(), null);
        eventRepository.save(event(fiscalYear, appUserId, FiscalYearClosingEvent.REOPEN, version, normalized,
                YearSnapshot.of(page), now));
        int superseded = outputRepository.supersedeVersion(fiscalYear.getId(), version);
        fiscalYear.setStatus(OPEN);
        fiscalYear.setClosedAt(null);
        audit(appUserId, fiscalYear, "reopen", version, "reopened superseded=" + superseded);
        return List.of();
    }

    /** 出力帳票の生成記録を追加する。page はサーバーで計算した同一リクエストの表示データ。 */
    @Transactional
    public void recordOutput(Long appUserId, TaxReturnPageData page, String outputType, String fileName, byte[] body,
            LocalDateTime generatedAt) {
        if (!isKnownOutputType(outputType)) {
            throw new IllegalArgumentException("unknown output type");
        }
        FiscalYear fiscalYear = ownedYear(appUserId, page.getSelectedYear());
        ReportOutputRecord record = new ReportOutputRecord();
        record.setFiscalYear(fiscalYear);
        record.setAppUserId(appUserId);
        record.setOutputType(outputType);
        record.setFileName(fileName.length() > 120 ? fileName.substring(0, 120) : fileName);
        record.setFileSha256(YearSnapshot.sha256(body));
        record.setFileSize((long) body.length);
        record.setDataFingerprint(YearSnapshot.of(page).fingerprint());
        record.setCalcRuleVersion(TaxReturnPdfRenderer.CALCULATION_RULE_VERSION);
        record.setFormEdition(BlueReturnEdition.forTaxYear(fiscalYear.getTaxYear()).getCode());
        boolean closed = CLOSED.equals(fiscalYear.getStatus());
        record.setYearStatus(closed ? CLOSED : OPEN);
        record.setCloseVersion(closed ? latestCloseVersion(fiscalYear) : null);
        record.setRecordStatus(ReportOutputRecord.CURRENT);
        record.setGeneratedAt(generatedAt);
        outputRepository.save(record);
        AUDIT.info("output userId={} year={} type={} result=recorded", appUserId, fiscalYear.getTaxYear(), outputType);
    }

    /** 出力記録を取消（無効）にする。記録は削除せず、理由と日時を残す。 */
    @Transactional
    public List<String> cancelOutput(Long appUserId, Long outputId, String reason, LocalDateTime now) {
        ReportOutputRecord record = outputRepository.findOwned(outputId, appUserId)
                .orElseThrow(YearClosingService::notFound);
        if (ReportOutputRecord.CANCELLED.equals(record.getRecordStatus())) {
            return List.of("この出力記録は既に取消済みです。");
        }
        String normalized = normalize(reason);
        if (normalized == null || normalized.length() > MAX_REASON_LENGTH) {
            return List.of("出力記録の取消理由（" + MAX_REASON_LENGTH + "文字以内）を入力してください。");
        }
        record.setRecordStatus(ReportOutputRecord.CANCELLED);
        record.setCancelReason(normalized);
        record.setCancelledAt(now);
        AUDIT.info("output-cancel userId={} year={} outputId={} result=cancelled", appUserId,
                record.getFiscalYear().getTaxYear(), outputId);
        return List.of();
    }

    @Transactional(readOnly = true)
    public ClosingPageData view(Long appUserId, TaxReturnPageData page) {
        FiscalYear fiscalYear = ownedYear(appUserId, page.getSelectedYear());
        YearSnapshot snapshot = YearSnapshot.of(page);
        String current = snapshot.fingerprint();
        List<FiscalYearClosingEvent> history = eventRepository.findByFiscalYearIdOrderByIdAsc(fiscalYear.getId());
        List<ClosingPageData.EventView> events = new ArrayList<>();
        FiscalYearClosingEvent previousClose = null;
        for (FiscalYearClosingEvent event : history) {
            boolean close = FiscalYearClosingEvent.CLOSE.equals(event.getEventType());
            String summary = close ? changeSummary(previousClose, event) : "締め第" + event.getCloseVersion() + "版を取消";
            events.add(new ClosingPageData.EventView(event.getEventType(), event.getCloseVersion(),
                    event.getOccurredAt(), event.getReason(), event.getSales(), event.getExpenses(), event.getIncome(),
                    event.getIncomeAfterDeduction(), event.getBlueDeduction(), event.getAssetsClosing(),
                    event.getJournalLineCount(), event.getDebitTotal(), event.getDataFingerprint(),
                    event.getCalcRuleVersion(), event.getFormEdition(), summary));
            if (close) {
                previousClose = event;
            }
        }
        List<ClosingPageData.OutputView> outputs = outputRepository
                .findByFiscalYearIdOrderByGeneratedAtDescIdDesc(fiscalYear.getId(), PageRequest.of(0, MAX_OUTPUTS_SHOWN))
                .stream()
                .map(r -> new ClosingPageData.OutputView(r.getId(), r.getOutputType(),
                        OUTPUT_LABELS.getOrDefault(r.getOutputType(), r.getOutputType()), r.getFileName(),
                        r.getGeneratedAt(), r.getCalcRuleVersion(), r.getFormEdition(), r.getYearStatus(),
                        r.getCloseVersion(), r.getFileSha256(), r.getFileSize(), r.getRecordStatus(),
                        snapshot.matches(r.getDataFingerprint()), r.getCancelReason(), r.getCancelledAt()))
                .toList();
        List<FiscalYear> siblings = siblings(fiscalYear);
        boolean closed = CLOSED.equals(fiscalYear.getStatus());
        return new ClosingPageData(closed, closed ? latestCloseVersion(fiscalYear) : null, fiscalYear.getClosedAt(),
                current.substring(0, 12), TaxReturnPdfRenderer.CALCULATION_RULE_VERSION,
                BlueReturnEdition.forTaxYear(fiscalYear.getTaxYear()).getCode(),
                siblings.stream().filter(y -> y.getTaxYear() < fiscalYear.getTaxYear() && !CLOSED.equals(y.getStatus()))
                        .map(y -> y.getTaxYear() + "年分").toList(),
                siblings.stream().filter(y -> y.getTaxYear() > fiscalYear.getTaxYear() && CLOSED.equals(y.getStatus()))
                        .map(y -> y.getTaxYear() + "年分").toList(),
                events, outputs, outputRepository.countByFiscalYearId(fiscalYear.getId()));
    }

    /** 締め中の年度の現在の締め版番号。締め記録のない旧データは null。 */
    @Transactional(readOnly = true)
    public Integer currentCloseVersion(Long appUserId, Integer year) {
        FiscalYear fiscalYear = ownedYear(appUserId, year);
        return CLOSED.equals(fiscalYear.getStatus()) ? latestCloseVersion(fiscalYear) : null;
    }

    private Integer latestCloseVersion(FiscalYear fiscalYear) {
        return eventRepository.findFirstByFiscalYearIdAndEventTypeOrderByCloseVersionDesc(fiscalYear.getId(),
                FiscalYearClosingEvent.CLOSE).map(FiscalYearClosingEvent::getCloseVersion).orElse(null);
    }

    private static int nextVersion(List<FiscalYearClosingEvent> history) {
        return history.stream().map(FiscalYearClosingEvent::getCloseVersion).max(Integer::compare).orElse(0) + 1;
    }

    private static String changeSummary(FiscalYearClosingEvent previous, FiscalYearClosingEvent current) {
        if (previous == null) {
            return "初回の締め";
        }
        if (previous.getDataFingerprint().equals(current.getDataFingerprint())) {
            return "第" + previous.getCloseVersion() + "版から会計データの変更なし";
        }
        List<String> parts = new ArrayList<>();
        diff(parts, "売上", previous.getSales(), current.getSales());
        diff(parts, "経費", previous.getExpenses(), current.getExpenses());
        diff(parts, "所得", previous.getIncome(), current.getIncome());
        diff(parts, "控除後所得", previous.getIncomeAfterDeduction(), current.getIncomeAfterDeduction());
        diff(parts, "期末資産", previous.getAssetsClosing(), current.getAssetsClosing());
        if (!previous.getJournalLineCount().equals(current.getJournalLineCount())) {
            parts.add("仕訳行 " + previous.getJournalLineCount() + "→" + current.getJournalLineCount());
        }
        if (!previous.getBlueDeduction().equals(current.getBlueDeduction())) {
            parts.add("控除区分 " + previous.getBlueDeduction() + "万円→" + current.getBlueDeduction() + "万円");
        }
        if (parts.isEmpty()) {
            parts.add("仕訳・帳票明細の内容変更、または指紋の算出方式の更新（合計額は同じ）");
        }
        return "第" + previous.getCloseVersion() + "版からの訂正: " + String.join("、", parts);
    }

    private static void diff(List<String> parts, String label, BigDecimal before, BigDecimal after) {
        if (before.compareTo(after) != 0) {
            parts.add(label + " " + yen(before) + "→" + yen(after) + "（" + (after.subtract(before).signum() > 0 ? "+" : "")
                    + yen(after.subtract(before)) + "）");
        }
    }

    private static String yen(BigDecimal value) {
        return String.format("%,d円", value.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact());
    }

    private FiscalYearClosingEvent event(FiscalYear fiscalYear, Long appUserId, String type, int version,
            String reason, YearSnapshot snapshot, LocalDateTime now) {
        FiscalYearClosingEvent event = new FiscalYearClosingEvent();
        event.setFiscalYear(fiscalYear);
        event.setAppUserId(appUserId);
        event.setEventType(type);
        event.setCloseVersion(version);
        event.setReason(reason);
        event.setSales(snapshot.sales());
        event.setExpenses(snapshot.expenses());
        event.setIncome(snapshot.income());
        event.setIncomeAfterDeduction(snapshot.incomeAfterDeduction());
        event.setBlueDeduction(snapshot.deduction());
        event.setAssetsClosing(snapshot.assetsClosing());
        event.setLiabilitiesClosing(snapshot.liabilitiesClosing());
        event.setJournalLineCount(snapshot.journalLineCount());
        event.setDebitTotal(snapshot.debitTotal());
        event.setCreditTotal(snapshot.creditTotal());
        event.setDataFingerprint(snapshot.fingerprint());
        event.setCalcRuleVersion(TaxReturnPdfRenderer.CALCULATION_RULE_VERSION);
        event.setFormEdition(BlueReturnEdition.forTaxYear(fiscalYear.getTaxYear()).getCode());
        event.setOccurredAt(now);
        return event;
    }

    private FiscalYear lockOwnedYear(Long appUserId, Integer year) {
        FiscalYear found = ownedYear(appUserId, year);
        return fiscalYearRepository.findOwnedByIdForUpdate(found.getId(), appUserId)
                .orElseThrow(YearClosingService::notFound);
    }

    private FiscalYear ownedYear(Long appUserId, Integer year) {
        if (year == null) {
            throw notFound();
        }
        BusinessProfile profile = businessProfileRepository.findByAppUserId(appUserId)
                .orElseThrow(YearClosingService::notFound);
        return fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId()).stream()
                .filter(y -> year.equals(y.getTaxYear()))
                .findFirst()
                .orElseThrow(YearClosingService::notFound);
    }

    private List<FiscalYear> siblings(FiscalYear fiscalYear) {
        List<FiscalYear> years = new ArrayList<>(fiscalYearRepository
                .findByBusinessProfileIdOrderByTaxYearDesc(fiscalYear.getBusinessProfile().getId()));
        years.sort(Comparator.comparing(FiscalYear::getTaxYear));
        return years;
    }

    private static EntryNotFoundException notFound() {
        return new EntryNotFoundException("対象が見つかりません。");
    }

    private static String normalize(String reason) {
        if (reason == null) {
            return null;
        }
        String stripped = reason.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static void audit(Long appUserId, FiscalYear fiscalYear, String action, Integer version, String result) {
        AUDIT.info("{} userId={} year={} version={} result={}", action, appUserId, fiscalYear.getTaxYear(),
                version, result);
    }
}
