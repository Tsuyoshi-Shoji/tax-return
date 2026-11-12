package org.example.features.taxreturn.input;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;
import org.example.entity.EmployeeSalaryDetail;
import org.example.entity.HouseholdConsumption;
import org.example.entity.TaxDeductionRecord;
import org.example.entity.TaxDependent;
import org.example.entity.TaxReturnInput;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.asset.DepreciationRules;
import org.example.features.taxreturn.form.TaxReturnForms.DepreciationRulesForm;
import org.example.features.taxreturn.form.TaxReturnForms.DeductionForm;
import org.example.features.taxreturn.form.TaxReturnForms.DependentForm;
import org.example.features.taxreturn.form.TaxReturnForms.FixedAssetForm;
import org.example.features.taxreturn.form.TaxReturnForms.FixedAssetEventForm;
import org.example.features.taxreturn.form.TaxReturnForms.EmployeeSalaryForm;
import org.example.features.taxreturn.form.TaxReturnForms.HouseholdConsumptionForm;
import org.example.features.taxreturn.form.TaxReturnForms.ReturnInputForm;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.FixedAssetRepository;
import org.example.repository.FixedAssetEventRepository;
import org.example.repository.EmployeeSalaryDetailRepository;
import org.example.repository.HouseholdConsumptionRepository;
import org.example.repository.TaxDeductionRecordRepository;
import org.example.repository.TaxDependentRepository;
import org.example.repository.TaxReturnInputRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 確定申告画面で入力する年分別データ（固定資産台帳・申告情報・扶養親族・所得控除）の登録・削除。
 * すべて認証済みユーザーの事業者・年度に限定し、確定済み年度は変更しない。
 */
@Service
public class TaxReturnInputService {
    public static final Set<String> DEDUCTION_TYPES = Set.of(
            "SOCIAL_INSURANCE", "SMALL_BUSINESS_MUTUAL", "LIFE_INSURANCE_GENERAL", "LIFE_INSURANCE_MEDICAL",
            "LIFE_INSURANCE_PENSION", "EARTHQUAKE_INSURANCE", "MEDICAL", "DONATION");
    public static final Set<String> DEDUCTION_STATUSES = Set.of("CANDIDATE", "VERIFIED", "INELIGIBLE");
    private static final Set<String> DISABILITY = Set.of("NONE", "GENERAL", "SPECIAL");
    private static final Set<String> WIDOW = Set.of("NONE", "WIDOW", "SINGLE_PARENT");
    private static final Set<String> METHODS = DepreciationRules.METHODS;
    private static final Set<String> FUNDING = Set.of("EXPENSE_ENTRY", "OWNER_FUNDS");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");
    private static final Pattern FORBIDDEN = Pattern.compile("[<>\"'`\\\\\\p{Cntrl}]");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final String CLOSED_MESSAGE = "確定済みの年度は編集できません。";

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetEventRepository fixedAssetEventRepository;
    private final TaxReturnInputRepository taxReturnInputRepository;
    private final TaxDependentRepository taxDependentRepository;
    private final TaxDeductionRecordRepository taxDeductionRecordRepository;
    private final HouseholdConsumptionRepository householdConsumptionRepository;
    private final EmployeeSalaryDetailRepository employeeSalaryDetailRepository;
    private final ClosingJournalService closingJournalService;

    public TaxReturnInputService(BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository, FixedAssetRepository fixedAssetRepository,
            FixedAssetEventRepository fixedAssetEventRepository,
            TaxReturnInputRepository taxReturnInputRepository, TaxDependentRepository taxDependentRepository,
            TaxDeductionRecordRepository taxDeductionRecordRepository,
            HouseholdConsumptionRepository householdConsumptionRepository,
            EmployeeSalaryDetailRepository employeeSalaryDetailRepository,
            ClosingJournalService closingJournalService) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.fixedAssetRepository = fixedAssetRepository;
        this.fixedAssetEventRepository = fixedAssetEventRepository;
        this.taxReturnInputRepository = taxReturnInputRepository;
        this.taxDependentRepository = taxDependentRepository;
        this.taxDeductionRecordRepository = taxDeductionRecordRepository;
        this.householdConsumptionRepository = householdConsumptionRepository;
        this.employeeSalaryDetailRepository = employeeSalaryDetailRepository;
        this.closingJournalService = closingJournalService;
    }

    @Transactional
    public List<String> addFixedAssetEvent(Long appUserId, Integer year, FixedAssetEventForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        FixedAsset asset = form.getAssetId() == null ? null
                : fixedAssetRepository.findByIdAndBusinessProfileId(form.getAssetId(), fy.getBusinessProfile().getId()).orElse(null);
        if (asset == null) {
            throw new EntryNotFoundException("Asset not found");
        }
        String type = form.getEventType() == null ? "" : form.getEventType().trim();
        if (!Set.of(FixedAssetEvent.BUSINESS_RATIO_CHANGE, FixedAssetEvent.SALE, FixedAssetEvent.DISPOSAL,
                FixedAssetEvent.INHERITANCE, FixedAssetEvent.GIFT, FixedAssetEvent.METHOD_CHANGE,
                FixedAssetEvent.BUSINESS_CESSATION).contains(type)) {
            errors.add("固定資産イベントの種類が不正です。");
        }
        LocalDate effectiveDate = date("適用日", form.getEffectiveDate(), true, errors);
        BigDecimal ratio = FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(type)
                ? ratio("事業専用割合", form.getBusinessRatio(), errors) : null;
        BigDecimal proceeds = FixedAssetEvent.SALE.equals(type)
                ? amount("売却価額", form.getProceeds(), true, errors) : null;
        boolean needsOpeningValue = FixedAssetEvent.INHERITANCE.equals(type) || FixedAssetEvent.GIFT.equals(type)
                || FixedAssetEvent.METHOD_CHANGE.equals(type);
        BigDecimal openingBookValue = needsOpeningValue
                ? amount("引継ぎ・変更時の期首簿価", form.getOpeningBookValue(), true, errors) : null;
        String newMethod = FixedAssetEvent.METHOD_CHANGE.equals(type)
                ? (form.getNewDepreciationMethod() == null ? "" : form.getNewDepreciationMethod().trim()) : null;
        String notes = text("摘要・確認根拠", form.getNotes(), 255,
                FixedAssetEvent.INHERITANCE.equals(type) || FixedAssetEvent.GIFT.equals(type)
                        || FixedAssetEvent.METHOD_CHANGE.equals(type), errors);
        if (effectiveDate != null && (effectiveDate.isBefore(fy.getPeriodStart()) || effectiveDate.isAfter(fy.getPeriodEnd()))) {
            errors.add("適用日は対象年度内の日付を入力してください。");
        }
        if (effectiveDate != null && !FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(type)
                && effectiveDate.isBefore(asset.getServiceStartDate())) {
            errors.add("適用日は事業供用開始日以降の日付を入力してください。");
        }
        if (openingBookValue != null && (openingBookValue.signum() < 0
                || openingBookValue.compareTo(asset.getAcquisitionCost()) > 0)) {
            errors.add("引継ぎ・変更時の期首簿価は0円以上、当初取得価額以下で入力してください。");
        }
        if (needsOpeningValue && !form.isHistoryConfirmed()) {
            errors.add("過年度の償却履歴・確認根拠を確認したことを明示してください。");
        }
        if (FixedAssetEvent.METHOD_CHANGE.equals(type)) {
            if (!Set.of(DepreciationCalculator.STRAIGHT_LINE, DepreciationCalculator.DECLINING_200,
                    DepreciationCalculator.DECLINING_250).contains(newMethod)
                    || !DepreciationCalculator.isRegular(asset.getDepreciationMethod())
                    || newMethod.equals(asset.getDepreciationMethod())) {
                errors.add("途中変更は定額法・200％定率法・250％定率法間の変更に限ります。");
            }
            if (effectiveDate != null && !effectiveDate.equals(LocalDate.of(effectiveDate.getYear(), 1, 1))) {
                errors.add("償却方法変更の適用日は変更年の1月1日にしてください。");
            }
            if (effectiveDate != null && !effectiveDate.isAfter(asset.getServiceStartDate())) {
                errors.add("償却方法変更は供用開始後の年から登録してください。");
            }
        }
        if ((FixedAssetEvent.INHERITANCE.equals(type) || FixedAssetEvent.GIFT.equals(type))
                && asset.isPrivateUseConversion()) {
            errors.add("相続・贈与と非業務用からの転用の併用履歴は未対応です。");
        }
        if (FixedAssetEvent.BUSINESS_CESSATION.equals(type)
                && (!DepreciationCalculator.isRegular(asset.getDepreciationMethod())
                    || (effectiveDate != null && effectiveDate.isBefore(asset.getServiceStartDate())))) {
            errors.add("廃業時の月割りは通常償却資産で、供用開始日以降の場合に限ります。");
        }
        if (FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(type)
                && effectiveDate != null && !effectiveDate.equals(fy.getPeriodStart())) {
            errors.add("事業専用割合の変更は年度の初日から適用してください。変更年度内の月割り按分は未対応です。");
        }
        if (ratio != null && (ratio.signum() <= 0 || ratio.compareTo(new BigDecimal("100")) > 0)) {
            errors.add("事業専用割合は0%超100%以下で入力してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        List<FixedAssetEvent> events = fixedAssetEventRepository.findByFixedAssetIdOrderByEffectiveDateAscIdAsc(asset.getId());
        if (events.stream().anyMatch(e -> e.getEffectiveDate().equals(effectiveDate))) {
            return List.of("同じ日付の固定資産イベントが登録済みです。");
        }
        if (events.stream().anyMatch(e -> FixedAssetEvent.SALE.equals(e.getEventType())
                || FixedAssetEvent.DISPOSAL.equals(e.getEventType())
                || FixedAssetEvent.BUSINESS_CESSATION.equals(e.getEventType()))) {
            return List.of("売却・除却済みの固定資産には追加イベントを登録できません。");
        }
        if ((FixedAssetEvent.INHERITANCE.equals(type) || FixedAssetEvent.GIFT.equals(type))
                && events.stream().anyMatch(e -> FixedAssetEvent.INHERITANCE.equals(e.getEventType())
                        || FixedAssetEvent.GIFT.equals(e.getEventType())
                        || FixedAssetEvent.METHOD_CHANGE.equals(e.getEventType()))) {
            return List.of("相続・贈与と方法変更の履歴は各資産1件までで、併用できません。");
        }
        if (FixedAssetEvent.METHOD_CHANGE.equals(type)
                && events.stream().anyMatch(e -> FixedAssetEvent.INHERITANCE.equals(e.getEventType())
                        || FixedAssetEvent.GIFT.equals(e.getEventType())
                        || FixedAssetEvent.METHOD_CHANGE.equals(e.getEventType()))) {
            return List.of("相続・贈与と方法変更の履歴は各資産1件までで、併用できません。");
        }
        if ((FixedAssetEvent.INHERITANCE.equals(type) || FixedAssetEvent.GIFT.equals(type))
                && events.stream().anyMatch(e -> FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(e.getEventType())
                        && e.getEffectiveDate().isAfter(effectiveDate))) {
            return List.of("移転日より後の日付のイベントが登録済みです。先にそのイベントを削除してください。");
        }
        boolean closedYearAffected = fiscalYearRepository
                .findByBusinessProfileIdOrderByTaxYearDesc(fy.getBusinessProfile().getId()).stream()
                .anyMatch(y -> isClosed(y) && !effectiveDate.isAfter(y.getPeriodEnd()));
        if (closedYearAffected) {
            return List.of("適用日が確定済み年度に含まれる固定資産イベントは登録できません。");
        }
        FixedAssetEvent event = new FixedAssetEvent();
        event.setFixedAsset(asset);
        event.setEventType(type);
        event.setEffectiveDate(effectiveDate);
        event.setBusinessRatio(ratio);
        event.setProceeds(proceeds);
        event.setOpeningBookValue(openingBookValue);
        event.setNewDepreciationMethod(newMethod);
        event.setHistoryConfirmed(form.isHistoryConfirmed());
        event.setNotes(notes);
        fixedAssetEventRepository.save(event);
        invalidateDepreciationConfirmations(fy.getBusinessProfile().getId(), effectiveDate);
        closingJournalService.rebuildOpenYears(fy.getBusinessProfile().getId());
        return List.of();
    }

    @Transactional
    public List<String> deleteFixedAssetEvent(Long appUserId, Integer year, Long id) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        FixedAssetEvent event = id == null ? null
                : fixedAssetEventRepository.findByIdAndFixedAssetBusinessProfileId(id, fy.getBusinessProfile().getId()).orElse(null);
        if (event == null) {
            throw new EntryNotFoundException("Asset event not found");
        }
        boolean closedYearAffected = fiscalYearRepository
                .findByBusinessProfileIdOrderByTaxYearDesc(fy.getBusinessProfile().getId()).stream()
                .anyMatch(y -> isClosed(y) && !event.getEffectiveDate().isAfter(y.getPeriodEnd()));
        if (closedYearAffected) {
            return List.of("確定済み年度に影響する固定資産イベントは削除できません。");
        }
        fixedAssetEventRepository.delete(event);
        fixedAssetEventRepository.flush();
        invalidateDepreciationConfirmations(fy.getBusinessProfile().getId(), event.getEffectiveDate());
        closingJournalService.rebuildOpenYears(fy.getBusinessProfile().getId());
        return List.of();
    }

    @Transactional
    public List<String> addHouseholdConsumption(Long appUserId, Integer year, HouseholdConsumptionForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        LocalDate occurred = date("取引日", form.getTransactionDate(), true, errors);
        String details = text("内容", form.getDetails(), 100, true, errors);
        BigDecimal amount = amount("家事消費等の金額", form.getAmount(), true, errors);
        if (occurred != null && (occurred.isBefore(fy.getPeriodStart()) || occurred.isAfter(fy.getPeriodEnd()))) {
            errors.add("取引日は対象年度内の日付を入力してください。");
        }
        if (amount != null && amount.signum() <= 0) {
            errors.add("金額は1円以上で入力してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        HouseholdConsumption entry = new HouseholdConsumption();
        entry.setFiscalYear(fy);
        entry.setTransactionDate(occurred);
        entry.setDetails(details);
        entry.setAmount(amount);
        householdConsumptionRepository.save(entry);
        closingJournalService.rebuild(fy.getId());
        return List.of();
    }

    @Transactional
    public List<String> deleteHouseholdConsumption(Long appUserId, Integer year, Long id) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        HouseholdConsumption entry = id == null ? null
                : householdConsumptionRepository.findByIdAndFiscalYearId(id, fy.getId()).orElse(null);
        if (entry == null) {
            throw new EntryNotFoundException("Household consumption not found");
        }
        householdConsumptionRepository.delete(entry);
        householdConsumptionRepository.flush();
        closingJournalService.rebuild(fy.getId());
        return List.of();
    }

    @Transactional
    public List<String> addEmployeeSalaryDetail(Long appUserId, Integer year, EmployeeSalaryForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        String kind = form.getSalaryKind() == null ? "" : form.getSalaryKind().trim();
        if (!Set.of("SALARIES", "DEDICATED_SALARY").contains(kind)) {
            errors.add("給与区分を選択してください。");
        }
        String name = text("氏名", form.getPersonName(), 100, true, errors);
        String job = text("職務", form.getJobDescription(), 100, true, errors);
        LocalDate from = date("従事期間の開始日", form.getWorkPeriodStart(), true, errors);
        LocalDate to = date("従事期間の終了日", form.getWorkPeriodEnd(), true, errors);
        String paymentPeriod = text("支給期", form.getPaymentPeriod(), 30, true, errors);
        BigDecimal amount = amount("支給額", form.getAmount(), true, errors);
        if (from != null && (from.isBefore(fy.getPeriodStart()) || from.isAfter(fy.getPeriodEnd()))) {
            errors.add("従事期間の開始日は対象年度内の日付を入力してください。");
        }
        if (to != null && (to.isBefore(fy.getPeriodStart()) || to.isAfter(fy.getPeriodEnd()))) {
            errors.add("従事期間の終了日は対象年度内の日付を入力してください。");
        }
        if (from != null && to != null && to.isBefore(from)) {
            errors.add("従事期間の終了日は開始日以降の日付を入力してください。");
        }
        if (amount != null && amount.signum() <= 0) {
            errors.add("支給額は1円以上で入力してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        EmployeeSalaryDetail detail = new EmployeeSalaryDetail();
        detail.setFiscalYear(fy);
        detail.setSalaryKind(kind);
        detail.setPersonName(name);
        detail.setJobDescription(job);
        detail.setWorkPeriodStart(from);
        detail.setWorkPeriodEnd(to);
        detail.setPaymentPeriod(paymentPeriod);
        detail.setAmount(amount);
        employeeSalaryDetailRepository.save(detail);
        return List.of();
    }

    @Transactional
    public List<String> deleteEmployeeSalaryDetail(Long appUserId, Integer year, Long id) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        EmployeeSalaryDetail detail = id == null ? null
                : employeeSalaryDetailRepository.findByIdAndFiscalYearId(id, fy.getId()).orElse(null);
        if (detail == null) {
            throw new EntryNotFoundException("Employee salary detail not found");
        }
        employeeSalaryDetailRepository.delete(detail);
        return List.of();
    }

    @Transactional
    public List<String> addFixedAsset(Long appUserId, Integer year, FixedAssetForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        String name = text("資産の名称", form.getAssetName(), 100, true, errors);
        String account = form.getAccountCode() == null ? "" : form.getAccountCode().trim();
        if (!AccountMasterSeed.FIXED_ASSET_ACCOUNTS.contains(account)) {
            errors.add("勘定科目を選択してください。");
        }
        LocalDate acquired = date("取得年月日", form.getAcquisitionDate(), true, errors);
        LocalDate serviceStart = date("事業供用開始日", form.getServiceStartDate(), false, errors);
        BigDecimal cost = amount("取得価額", form.getAcquisitionCost(), true, errors);
        String method = form.getDepreciationMethod() == null ? "" : form.getDepreciationMethod().trim();
        if (!METHODS.contains(method)) {
            errors.add("償却方法を選択してください。");
        }
        Integer life = integer("法定耐用年数", form.getUsefulLife(), 2, 50, DepreciationCalculator.isRegular(method), errors);
        String lifeBasis = form.getLifeBasis() == null ? "STATUTORY" : form.getLifeBasis().trim();
        if (!Set.of("STATUTORY", DepreciationRules.USED_SIMPLIFIED).contains(lifeBasis)) {
            errors.add("耐用年数の根拠を選択してください。");
        }
        Integer elapsedMonths = integer("中古取得時の経過月数", form.getUsedElapsedMonths(), 0, 2400,
                DepreciationRules.USED_SIMPLIFIED.equals(lifeBasis), errors);
        BigDecimal ratio = amount("事業専用割合", form.getBusinessRatio(), true, errors);
        if (ratio != null && (ratio.signum() <= 0 || ratio.compareTo(new BigDecimal("100")) > 0)) {
            errors.add("事業専用割合は1〜100の整数で入力してください。");
        }
        String funding = form.getAcquisitionFunding() == null ? "" : form.getAcquisitionFunding().trim();
        if (!FUNDING.contains(funding)) {
            errors.add("取得時の支払方法を選択してください。");
        }
        String origin = form.getAssetOrigin() == null || form.getAssetOrigin().isBlank()
                ? "ACQUISITION" : form.getAssetOrigin().trim();
        if (!Set.of("ACQUISITION", "CAPITAL_IMPROVEMENT").contains(origin)) {
            errors.add("資産の取得区分を選択してください。");
        }
        FixedAsset parentAsset = null;
        if ("CAPITAL_IMPROVEMENT".equals(origin)) {
            parentAsset = form.getParentAssetId() == null ? null
                    : fixedAssetRepository.findByIdAndBusinessProfileId(form.getParentAssetId(),
                            fy.getBusinessProfile().getId()).orElse(null);
            if (parentAsset == null) {
                errors.add("資本的支出の対象となる固定資産を選択してください。");
            } else {
                if (!DepreciationCalculator.isRegular(parentAsset.getDepreciationMethod())
                        || parentAsset.getParentAsset() != null
                        || !account.equals(parentAsset.getAccountCode())
                        || !DepreciationRules.USED_SIMPLIFIED.equals(lifeBasis)
                            && !java.util.Objects.equals(life, parentAsset.getStatutoryUsefulLife())) {
                    errors.add("資本的支出は通常償却資産と同じ勘定科目・耐用年数で別資産登録してください。");
                }
                if (DepreciationRules.USED_SIMPLIFIED.equals(lifeBasis)
                        || DepreciationRules.USED_SIMPLIFIED.equals(parentAsset.getLifeBasis())) {
                    errors.add("中古簡便法を適用した資産の資本的支出は未対応です。");
                }
                if (acquired != null && acquired.isBefore(LocalDate.of(2007, 4, 1))) {
                    errors.add("2007年3月31日以前の資本的支出は旧資産への加算処理が必要なため未対応です。");
                }
                if (acquired != null && fixedAssetEventRepository.findByFixedAssetIdOrderByEffectiveDateAscIdAsc(parentAsset.getId())
                        .stream().anyMatch(e -> (FixedAssetEvent.SALE.equals(e.getEventType())
                                || FixedAssetEvent.DISPOSAL.equals(e.getEventType())
                                || FixedAssetEvent.BUSINESS_CESSATION.equals(e.getEventType()))
                                && !e.getEffectiveDate().isAfter(acquired))) {
                    errors.add("処分・廃業後の資産への資本的支出は登録できません。");
                }
            }
        } else if (form.getParentAssetId() != null) {
            errors.add("親資産は資本的支出の場合だけ指定できます。");
        }
        String transferType = form.getOwnershipTransferType() == null || form.getOwnershipTransferType().isBlank()
                ? "" : form.getOwnershipTransferType().trim();
        if (!transferType.isEmpty() && !Set.of(FixedAssetEvent.INHERITANCE, FixedAssetEvent.GIFT).contains(transferType)) {
            errors.add("相続・贈与の区分が不正です。");
        }
        boolean transferred = FixedAssetEvent.INHERITANCE.equals(transferType) || FixedAssetEvent.GIFT.equals(transferType);
        LocalDate transferDate = transferred
                ? date("相続・贈与日", form.getOwnershipTransferDate(), true, errors) : null;
        BigDecimal transferBookValue = transferred
                ? amount("相続・贈与時の簿価", form.getOwnershipOpeningBookValue(), true, errors) : null;
        String transferEvidence = transferred
                ? text("相続・贈与の確認根拠", form.getOwnershipEvidence(), 255, true, errors) : null;
        String notes = text("摘要", form.getNotes(), 255, false, errors);
        if (acquired != null && serviceStart != null && serviceStart.isBefore(acquired)) {
            errors.add("事業供用開始日は取得年月日以降の日付を入力してください。");
        }
        if (acquired != null && acquired.isAfter(fy.getPeriodEnd())) {
            errors.add("取得年月日は対象年度の末日以前の日付を入力してください。");
        }
        LocalDate recognitionDate = transferred ? transferDate
                : form.isPrivateUseConversion() ? serviceStart : acquired;
        if (transferred) {
            if (!form.isOwnershipHistoryConfirmed()) {
                errors.add("相続・贈与では承継日簿価・過年度償却履歴を確認してください。");
            }
            if (transferDate != null && (transferDate.isBefore(fy.getPeriodStart())
                    || transferDate.isAfter(fy.getPeriodEnd())
                    || (acquired != null && transferDate.isBefore(acquired))
                    || (serviceStart != null && transferDate.isBefore(serviceStart)))) {
                errors.add("相続・贈与日は取得日・供用開始日以後で、対象年度内の日付にしてください。");
            }
            if (!DepreciationCalculator.isRegular(method) || form.isPrivateUseConversion()
                    || "CAPITAL_IMPROVEMENT".equals(origin)) {
                errors.add("相続・贈与での引継ぎは通常償却資産に限り、私用転用・資本的支出との併用は未対応です。");
            }
            if (transferBookValue != null && cost != null
                    && (transferBookValue.signum() < 0 || transferBookValue.compareTo(cost) > 0)) {
                errors.add("引継ぎ時の簿価は0円以上、当初取得価額以下にしてください。");
            }
        } else if ((form.getOwnershipTransferDate() != null && !form.getOwnershipTransferDate().isBlank())
                || (form.getOwnershipOpeningBookValue() != null && !form.getOwnershipOpeningBookValue().isBlank())
                || (form.getOwnershipEvidence() != null && !form.getOwnershipEvidence().isBlank())
                || form.isOwnershipHistoryConfirmed()) {
            errors.add("相続・贈与の情報は取得区分を選択した場合だけ入力できます。");
        }
        if (form.isPrivateUseConversion() && serviceStart == null) {
            errors.add("私用からの転用は事業供用開始日を入力してください。");
        }
        if (recognitionDate != null && fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(fy.getBusinessProfile().getId())
                .stream().anyMatch(y -> isClosed(y) && !recognitionDate.isAfter(y.getPeriodEnd()))) {
            errors.add("取得・転用が確定済み年度の帳簿に影響するため登録できません。");
        }
        if (cost != null && cost.signum() <= 0) {
            errors.add("取得価額は1円以上で入力してください。");
        }
        if (cost != null && acquired != null && DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(method)
                && (cost.compareTo(DepreciationCalculator.LUMP_SUM_MIN) < 0
                    || cost.compareTo(DepreciationRules.smallAmountLimit(acquired)) >= 0)) {
            errors.add("少額特例は10万円以上、2026年3月31日以前取得は30万円未満、4月1日以後取得は40万円未満です。");
        }
        if (cost != null && DepreciationCalculator.LUMP_SUM_3Y.equals(method)
                && (cost.compareTo(DepreciationCalculator.LUMP_SUM_MIN) < 0 || cost.compareTo(DepreciationCalculator.LUMP_SUM_LIMIT) >= 0)) {
            errors.add("一括償却資産は取得価額10万円以上20万円未満の資産に限ります。");
        }
        if (DepreciationCalculator.LUMP_SUM_3Y.equals(method) && !AccountMasterSeed.ACC_LUMP_SUM_ASSETS.equals(account)) {
            errors.add("一括償却資産を選択した場合は、勘定科目に「一括償却資産」を選択してください。");
        }
        if (AccountMasterSeed.ACC_LUMP_SUM_ASSETS.equals(account) && !DepreciationCalculator.LUMP_SUM_3Y.equals(method)) {
            errors.add("勘定科目が一括償却資産の場合は一括償却方式を選択してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        FixedAsset asset = new FixedAsset();
        asset.setBusinessProfile(fy.getBusinessProfile());
        asset.setAssetName(name);
        asset.setAccountCode(account);
        asset.setAcquisitionDate(acquired);
        asset.setServiceStartDate(serviceStart == null ? acquired : serviceStart);
        asset.setAcquisitionCost(cost);
        asset.setUsefulLife(DepreciationRules.USED_SIMPLIFIED.equals(lifeBasis) && life != null
                ? DepreciationRules.usedUsefulLife(life, elapsedMonths) : (life == null ? 1 : life));
        asset.setStatutoryUsefulLife(life);
        asset.setLifeBasis(lifeBasis);
        asset.setUsedElapsedMonths(elapsedMonths);
        asset.setUsedRequirementsConfirmed(form.isUsedRequirementsConfirmed());
        asset.setPrivateUseConversion(form.isPrivateUseConversion());
        asset.setRateTransitionConfirmed(form.isRateTransitionConfirmed());
        asset.setDepreciationMethod(method);
        asset.setBusinessRatio(ratio);
        asset.setAcquisitionFunding(funding);
        asset.setAssetOrigin(origin);
        asset.setParentAsset(parentAsset);
        asset.setNotes(notes);
        errors.addAll(DepreciationRules.assetErrors(asset));
        if (!DepreciationRules.supported(fy.getTaxYear())) {
            errors.add("減価償却ルールは2024〜2026年分のみ対応しています。");
        }
        if (DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(method)) {
            TaxReturnInput input = taxReturnInputRepository.findByFiscalYearId(fy.getId()).orElse(null);
            int months = input == null ? 12 : input.getDepreciationBusinessMonths();
            BigDecimal total = fixedAssetRepository
                    .findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(fy.getBusinessProfile().getId()).stream()
                    .filter(a -> DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(a.getDepreciationMethod()))
                    .filter(a -> a.getServiceStartDate().getYear() == asset.getServiceStartDate().getYear())
                    .map(FixedAsset::getAcquisitionCost).reduce(cost, BigDecimal::add);
            if (asset.getServiceStartDate().getYear() != fy.getTaxYear()) {
                errors.add("少額特例は供用開始年を選択して登録してください。");
            }
            if (total.compareTo(DepreciationRules.annualCap(months)) > 0) {
                errors.add("少額特例の取得価額合計が年分の上限（事業月数按分）を超えます。");
            }
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        fixedAssetRepository.saveAndFlush(asset);
        if (transferred) {
            FixedAssetEvent event = new FixedAssetEvent();
            event.setFixedAsset(asset);
            event.setEventType(transferType);
            event.setEffectiveDate(transferDate);
            event.setOpeningBookValue(transferBookValue);
            event.setHistoryConfirmed(true);
            event.setNotes(transferEvidence);
            fixedAssetEventRepository.save(event);
        }
        invalidateDepreciationConfirmations(fy.getBusinessProfile().getId(), recognitionDate);
        closingJournalService.rebuildOpenYears(fy.getBusinessProfile().getId());
        return List.of();
    }

    @Transactional
    public List<String> saveDepreciationRules(Long appUserId, Integer year, DepreciationRulesForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        if (!DepreciationRules.supported(fy.getTaxYear())) {
            errors.add("減価償却ルールは2024〜2026年分のみ対応しています。");
        }
        if (!DepreciationRules.METHOD_BASES.contains(form.getMethodBasis() == null ? "" : form.getMethodBasis())) {
            errors.add("償却方法の適用根拠（法定・届出・承認等）を選択してください。");
        }
        Integer months = integer("事業を営んでいた月数", form.getBusinessMonths(), 1, 12, true, errors);
        String evidence = text("確認根拠", form.getEvidence(), 500, true, errors);
        if (!errors.isEmpty()) {
            return errors;
        }
        BigDecimal specialTotal = fixedAssetRepository
                .findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(fy.getBusinessProfile().getId()).stream()
                .filter(a -> DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(a.getDepreciationMethod()))
                .filter(a -> a.getServiceStartDate().getYear() == fy.getTaxYear())
                .map(FixedAsset::getAcquisitionCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (specialTotal.compareTo(DepreciationRules.annualCap(months)) > 0) {
            return List.of("少額特例の取得価額合計が指定した事業月数の上限を超えています。資産の償却方法を見直してください。");
        }
        TaxReturnInput input = taxReturnInputRepository.findByFiscalYearId(fy.getId()).orElseGet(() -> {
            TaxReturnInput created = new TaxReturnInput();
            created.setFiscalYear(fy);
            return created;
        });
        input.setDepreciationRuleVersion(form.isScopeConfirmed() ? DepreciationRules.version(fy.getTaxYear()) : null);
        input.setDepreciationMethodBasis(form.getMethodBasis());
        input.setDepreciationSpecialConfirmed(form.isSpecialConfirmed());
        input.setDepreciationBusinessMonths(months);
        input.setDepreciationEvidence(evidence);
        input.setDepreciationConfirmedAt(form.isScopeConfirmed() ? LocalDateTime.now() : null);
        taxReturnInputRepository.save(input);
        return List.of();
    }

    @Transactional
    public List<String> deleteFixedAsset(Long appUserId, Integer year, Long assetId) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        FixedAsset asset = assetId == null ? null
                : fixedAssetRepository.findByIdAndBusinessProfileId(assetId, fy.getBusinessProfile().getId()).orElse(null);
        if (asset == null) {
            throw new EntryNotFoundException("Asset not found");
        }
        if (fixedAssetRepository.findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(fy.getBusinessProfile().getId())
                .stream().anyMatch(a -> a.getParentAsset() != null && asset.getId().equals(a.getParentAsset().getId()))) {
            return List.of("資本的支出の子資産が登録されています。先に子資産を削除してください。");
        }
        if (!fixedAssetEventRepository.findByFixedAssetIdOrderByEffectiveDateAscIdAsc(asset.getId()).isEmpty()) {
            return List.of("固定資産イベントが登録されています。履歴を削除してから資産を削除してください。");
        }
        List<FixedAssetEvent> events = fixedAssetEventRepository
                .findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(fy.getBusinessProfile().getId());
        boolean closedYearAffected = fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(fy.getBusinessProfile().getId())
                .stream().anyMatch(y -> isClosed(y)
                        && !DepreciationRules.recognitionDate(asset, events).isAfter(y.getPeriodEnd()));
        if (closedYearAffected) {
            return List.of("確定済みの年度で償却している資産は削除できません。");
        }
        fixedAssetRepository.delete(asset);
        fixedAssetRepository.flush();
        invalidateDepreciationConfirmations(fy.getBusinessProfile().getId(), DepreciationRules.recognitionDate(asset, events));
        closingJournalService.rebuildOpenYears(fy.getBusinessProfile().getId());
        return List.of();
    }

    @Transactional
    public List<String> saveReturnInput(Long appUserId, Integer year, ReturnInputForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        BigDecimal salaryRevenue = amountOrZero("給与の収入金額", form.getSalaryRevenue(), errors);
        BigDecimal salaryWithholding = amountOrZero("給与の源泉徴収税額", form.getSalaryWithholding(), errors);
        String salaryPayer = text("給与の支払者", form.getSalaryPayerName(), 100, false, errors);
        String salaryPayerAddress = text("給与の支払者の所在地", form.getSalaryPayerAddress(), 255, false, errors);
        BigDecimal miscRevenue = amountOrZero("雑所得の収入金額", form.getMiscRevenue(), errors);
        BigDecimal miscExpense = amountOrZero("雑所得の必要経費", form.getMiscExpense(), errors);
        BigDecimal miscWithholding = amountOrZero("雑所得の源泉徴収税額", form.getMiscWithholding(), errors);
        String miscPayer = text("雑所得の支払者", form.getMiscPayerName(), 100, false, errors);
        BigDecimal prepaid = amountOrZero("予定納税額", form.getPrepaidTax(), errors);
        String selfDisability = choice("本人の障害者区分", form.getSelfDisability(), DISABILITY, errors);
        String widow = choice("寡婦・ひとり親", form.getWidowStatus(), WIDOW, errors);
        String spouseName = text("配偶者の氏名", form.getSpouseName(), 100, false, errors);
        LocalDate spouseBirth = date("配偶者の生年月日", form.getSpouseBirthDate(), false, errors);
        BigDecimal spouseIncome = amountOrZero("配偶者の合計所得金額", form.getSpouseIncome(), errors);
        String spouseDisability = choice("配偶者の障害者区分", form.getSpouseDisability(), DISABILITY, errors);
        BigDecimal basePeriod = amount("基準期間の課税売上高", form.getBasePeriodTaxableSales(), false, errors);
        if (form.isHasSpouse() && (spouseName == null || spouseBirth == null)) {
            errors.add("配偶者がいる場合は、配偶者の氏名と生年月日を入力してください。");
        }
        if (salaryWithholding != null && salaryRevenue != null && salaryWithholding.compareTo(salaryRevenue) > 0) {
            errors.add("給与の源泉徴収税額は収入金額以下で入力してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        TaxReturnInput input = taxReturnInputRepository.findByFiscalYearId(fy.getId()).orElseGet(() -> {
            TaxReturnInput created = new TaxReturnInput();
            created.setFiscalYear(fy);
            return created;
        });
        input.setSalaryRevenue(salaryRevenue);
        input.setSalaryWithholding(salaryWithholding);
        input.setSalaryPayerName(salaryPayer);
        input.setSalaryPayerAddress(salaryPayerAddress);
        input.setMiscRevenue(miscRevenue);
        input.setMiscExpense(miscExpense);
        input.setMiscWithholding(miscWithholding);
        input.setMiscPayerName(miscPayer);
        input.setPrepaidTax(prepaid);
        input.setSelfDisability(selfDisability);
        input.setWidowStatus(widow);
        input.setWorkingStudent(form.isWorkingStudent());
        input.setHasSpouse(form.isHasSpouse());
        input.setSpouseName(form.isHasSpouse() ? spouseName : null);
        input.setSpouseBirthDate(form.isHasSpouse() ? spouseBirth : null);
        input.setSpouseIncome(form.isHasSpouse() ? spouseIncome : BigDecimal.ZERO);
        input.setSpouseDisability(form.isHasSpouse() ? spouseDisability : "NONE");
        input.setSpouseCohabiting(form.isSpouseCohabiting());
        input.setBasePeriodTaxableSales(basePeriod);
        taxReturnInputRepository.save(input);
        return List.of();
    }

    @Transactional
    public List<String> addDependent(Long appUserId, Integer year, DependentForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        String name = text("扶養親族の氏名", form.getDependentName(), 100, true, errors);
        String relation = text("続柄", form.getRelationship(), 30, true, errors);
        LocalDate birth = date("生年月日", form.getBirthDate(), true, errors);
        BigDecimal income = amountOrZero("合計所得金額", form.getIncome(), errors);
        String disability = choice("障害者区分", form.getDisability(), DISABILITY, errors);
        if (birth != null && birth.isAfter(fy.getPeriodEnd())) {
            errors.add("生年月日は対象年の12月31日以前の日付を入力してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        TaxDependent dependent = new TaxDependent();
        dependent.setFiscalYear(fy);
        dependent.setDependentName(name);
        dependent.setRelationship(relation);
        dependent.setBirthDate(birth);
        dependent.setIncome(income);
        dependent.setCohabiting(form.isCohabiting());
        dependent.setDisability(disability);
        taxDependentRepository.save(dependent);
        return List.of();
    }

    @Transactional
    public List<String> deleteDependent(Long appUserId, Integer year, Long id) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        TaxDependent dependent = id == null ? null : taxDependentRepository.findByIdAndFiscalYearId(id, fy.getId()).orElse(null);
        if (dependent == null) {
            throw new EntryNotFoundException("Dependent not found");
        }
        taxDependentRepository.delete(dependent);
        return List.of();
    }

    @Transactional
    public List<String> addDeduction(Long appUserId, Integer year, DeductionForm form) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        List<String> errors = new ArrayList<>();
        String type = choice("控除の種類", form.getDeductionType(), DEDUCTION_TYPES, errors);
        LocalDate paid = date("支払日", form.getPaidDate(), false, errors);
        BigDecimal amount = amount("支払金額", form.getPaidAmount(), true, errors);
        BigDecimal reimbursed = amount("補塡される金額", form.getReimbursedAmount(), false, errors);
        String payer = text("支払先", form.getPayer(), 100, false, errors);
        String notes = text("メモ", form.getNotes(), 255, false, errors);
        if (paid != null && (paid.isBefore(fy.getPeriodStart()) || paid.isAfter(fy.getPeriodEnd()))) {
            errors.add("支払日は対象年の日付を入力してください（控除は実際に支払った年分に適用します）。");
        }
        if (amount != null && amount.signum() <= 0) {
            errors.add("支払金額は1円以上で入力してください。");
        }
        if (reimbursed != null && amount != null && reimbursed.compareTo(amount) > 0) {
            errors.add("補塡される金額は支払金額以下で入力してください。");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        TaxDeductionRecord record = new TaxDeductionRecord();
        record.setFiscalYear(fy);
        record.setDeductionType(type);
        record.setPaidDate(paid);
        record.setPaidAmount(amount);
        record.setReimbursedAmount("MEDICAL".equals(type) ? reimbursed : null);
        record.setPayer(payer);
        record.setNotes(notes);
        record.setStatus("CANDIDATE");
        taxDeductionRecordRepository.save(record);
        return List.of();
    }

    @Transactional
    public List<String> updateDeductionStatus(Long appUserId, Integer year, Long id, String status) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        if (status == null || !DEDUCTION_STATUSES.contains(status)) {
            return List.of("控除の状態が不正です。");
        }
        TaxDeductionRecord record = id == null ? null : taxDeductionRecordRepository.findByIdAndFiscalYearId(id, fy.getId()).orElse(null);
        if (record == null) {
            throw new EntryNotFoundException("Deduction not found");
        }
        record.setStatus(status);
        record.setReviewedAt("CANDIDATE".equals(status) ? null : LocalDateTime.now());
        taxDeductionRecordRepository.save(record);
        return List.of();
    }

    @Transactional
    public List<String> deleteDeduction(Long appUserId, Integer year, Long id) {
        FiscalYear fy = ownedYear(appUserId, year);
        if (isClosed(fy)) {
            return List.of(CLOSED_MESSAGE);
        }
        TaxDeductionRecord record = id == null ? null : taxDeductionRecordRepository.findByIdAndFiscalYearId(id, fy.getId()).orElse(null);
        if (record == null) {
            throw new EntryNotFoundException("Deduction not found");
        }
        if (record.getSourceTransaction() != null) {
            return List.of("取引から作成された控除候補は削除できません。対象外にする場合は状態を「対象外」に変更してください。");
        }
        taxDeductionRecordRepository.delete(record);
        return List.of();
    }

    private void invalidateDepreciationConfirmations(Long profileId, LocalDate affectedFrom) {
        for (FiscalYear year : fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profileId)) {
            if (!isClosed(year) && !affectedFrom.isAfter(year.getPeriodEnd())) {
                taxReturnInputRepository.findByFiscalYearId(year.getId()).ifPresent(input -> {
                    input.setDepreciationRuleVersion(null);
                    input.setDepreciationConfirmedAt(null);
                });
            }
        }
    }

    private FiscalYear ownedYear(Long appUserId, Integer year) {
        BusinessProfile profile = businessProfileRepository.findByAppUserIdForUpdate(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Business profile not found"));
        if (year == null) {
            throw new EntryNotFoundException("Fiscal year not specified");
        }
        return fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId()).stream()
                .filter(f -> year.equals(f.getTaxYear()))
                .findFirst()
                .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
    }

    private static boolean isClosed(FiscalYear fy) {
        return "closed".equals(fy.getStatus());
    }

    private static String normalize(String raw) {
        return raw == null ? "" : Normalizer.normalize(raw, Normalizer.Form.NFKC).trim();
    }

    private static String text(String label, String raw, int max, boolean required, List<String> errors) {
        String value = raw == null ? "" : raw.strip();
        if (value.isEmpty()) {
            if (required) {
                errors.add(label + "を入力してください。");
            }
            return null;
        }
        if (value.length() > max) {
            errors.add(label + "は" + max + "文字以内で入力してください。");
            return null;
        }
        if (FORBIDDEN.matcher(value).find()) {
            errors.add(label + "に使用できない文字が含まれています。");
            return null;
        }
        return value;
    }

    private static BigDecimal amount(String label, String raw, boolean required, List<String> errors) {
        String value = normalize(raw).replace(",", "").replace(" ", "");
        if (value.isEmpty()) {
            if (required) {
                errors.add(label + "を入力してください。");
            }
            return null;
        }
        if (!value.matches("\\d{1,10}")) {
            errors.add(label + "は0〜9,999,999,999の整数で入力してください。");
            return null;
        }
        BigDecimal parsed = new BigDecimal(value);
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            errors.add(label + "は0〜9,999,999,999の範囲で入力してください。");
            return null;
        }
        return parsed;
    }

    private static BigDecimal amountOrZero(String label, String raw, List<String> errors) {
        BigDecimal value = amount(label, raw, false, errors);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal ratio(String label, String raw, List<String> errors) {
        String value = normalize(raw).replace("%", "").replace(" ", "");
        if (value.isEmpty() || !value.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            errors.add(label + "は0〜100の範囲で小数第2位まで入力してください。");
            return null;
        }
        BigDecimal parsed = new BigDecimal(value);
        if (parsed.compareTo(new BigDecimal("100")) > 0) {
            errors.add(label + "は0〜100の範囲で入力してください。");
            return null;
        }
        return parsed;
    }

    private static Integer integer(String label, String raw, int min, int max, boolean required, List<String> errors) {
        String value = normalize(raw);
        if (value.isEmpty()) {
            if (required) {
                errors.add(label + "を入力してください。");
            }
            return null;
        }
        if (!value.matches("\\d{1,3}") || Integer.parseInt(value) < min || Integer.parseInt(value) > max) {
            errors.add(label + "は" + min + "〜" + max + "の整数で入力してください。");
            return null;
        }
        return Integer.parseInt(value);
    }

    private static LocalDate date(String label, String raw, boolean required, List<String> errors) {
        String value = normalize(raw);
        if (value.isEmpty()) {
            if (required) {
                errors.add(label + "を入力してください。");
            }
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(value, ISO);
            if (parsed.isBefore(LocalDate.of(1900, 1, 1)) || parsed.isAfter(LocalDate.of(2100, 12, 31))) {
                errors.add(label + "の日付が範囲外です。");
                return null;
            }
            return parsed;
        } catch (DateTimeParseException ex) {
            errors.add(label + "は正しい日付（YYYY-MM-DD）で入力してください。");
            return null;
        }
    }

    private static String choice(String label, String raw, Set<String> allowed, List<String> errors) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() && allowed.contains("NONE")) {
            return "NONE";
        }
        if (!allowed.contains(value)) {
            errors.add(label + "の値が不正です。");
            return null;
        }
        return value;
    }
}
