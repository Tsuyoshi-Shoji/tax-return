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
import org.example.entity.TaxDeductionRecord;
import org.example.entity.TaxDependent;
import org.example.entity.TaxReturnInput;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.form.TaxReturnForms.DeductionForm;
import org.example.features.taxreturn.form.TaxReturnForms.DependentForm;
import org.example.features.taxreturn.form.TaxReturnForms.FixedAssetForm;
import org.example.features.taxreturn.form.TaxReturnForms.ReturnInputForm;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.FixedAssetRepository;
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
    private static final Set<String> METHODS = Set.of(
            DepreciationCalculator.STRAIGHT_LINE, DepreciationCalculator.SMALL_AMOUNT_SPECIAL, DepreciationCalculator.LUMP_SUM_3Y);
    private static final Set<String> FUNDING = Set.of("EXPENSE_ENTRY", "OWNER_FUNDS");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");
    private static final Pattern FORBIDDEN = Pattern.compile("[<>\"'`\\\\\\p{Cntrl}]");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final String CLOSED_MESSAGE = "確定済みの年度は編集できません。";

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final TaxReturnInputRepository taxReturnInputRepository;
    private final TaxDependentRepository taxDependentRepository;
    private final TaxDeductionRecordRepository taxDeductionRecordRepository;
    private final ClosingJournalService closingJournalService;

    public TaxReturnInputService(BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository, FixedAssetRepository fixedAssetRepository,
            TaxReturnInputRepository taxReturnInputRepository, TaxDependentRepository taxDependentRepository,
            TaxDeductionRecordRepository taxDeductionRecordRepository, ClosingJournalService closingJournalService) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.fixedAssetRepository = fixedAssetRepository;
        this.taxReturnInputRepository = taxReturnInputRepository;
        this.taxDependentRepository = taxDependentRepository;
        this.taxDeductionRecordRepository = taxDeductionRecordRepository;
        this.closingJournalService = closingJournalService;
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
        Integer life = integer("耐用年数", form.getUsefulLife(), 2, 50, DepreciationCalculator.STRAIGHT_LINE.equals(method), errors);
        BigDecimal ratio = amount("事業専用割合", form.getBusinessRatio(), true, errors);
        if (ratio != null && (ratio.signum() <= 0 || ratio.compareTo(new BigDecimal("100")) > 0)) {
            errors.add("事業専用割合は1〜100の整数で入力してください。");
        }
        String funding = form.getAcquisitionFunding() == null ? "" : form.getAcquisitionFunding().trim();
        if (!FUNDING.contains(funding)) {
            errors.add("取得時の支払方法を選択してください。");
        }
        String notes = text("摘要", form.getNotes(), 255, false, errors);
        if (acquired != null && serviceStart != null && serviceStart.isBefore(acquired)) {
            errors.add("事業供用開始日は取得年月日以降の日付を入力してください。");
        }
        if (acquired != null && acquired.isAfter(fy.getPeriodEnd())) {
            errors.add("取得年月日は対象年度の末日以前の日付を入力してください。");
        }
        if (acquired != null && fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(fy.getBusinessProfile().getId())
                .stream().anyMatch(y -> isClosed(y) && !acquired.isAfter(y.getPeriodEnd()))) {
            errors.add("確定済みの年度以前に取得した資産は登録できません（確定済み年度の帳簿が変わるため）。");
        }
        if (cost != null && cost.signum() <= 0) {
            errors.add("取得価額は1円以上で入力してください。");
        }
        if (cost != null && DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(method)
                && cost.compareTo(DepreciationCalculator.SMALL_AMOUNT_LIMIT) >= 0) {
            errors.add("少額減価償却資産の特例は取得価額30万円未満の資産に限ります。");
        }
        if (cost != null && DepreciationCalculator.LUMP_SUM_3Y.equals(method)
                && (cost.compareTo(DepreciationCalculator.LUMP_SUM_MIN) < 0 || cost.compareTo(DepreciationCalculator.LUMP_SUM_LIMIT) >= 0)) {
            errors.add("一括償却資産は取得価額10万円以上20万円未満の資産に限ります。");
        }
        if (DepreciationCalculator.LUMP_SUM_3Y.equals(method) && !AccountMasterSeed.ACC_LUMP_SUM_ASSETS.equals(account)) {
            errors.add("一括償却資産を選択した場合は、勘定科目に「一括償却資産」を選択してください。");
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
        asset.setUsefulLife(life == null ? 1 : life);
        asset.setDepreciationMethod(method);
        asset.setBusinessRatio(ratio);
        asset.setAcquisitionFunding(funding);
        asset.setNotes(notes);
        fixedAssetRepository.save(asset);
        closingJournalService.rebuildOpenYears(fy.getBusinessProfile().getId());
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
        boolean closedYearAffected = fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(fy.getBusinessProfile().getId())
                .stream().anyMatch(y -> isClosed(y) && !asset.getAcquisitionDate().isAfter(y.getPeriodEnd()));
        if (closedYearAffected) {
            return List.of("確定済みの年度で償却している資産は削除できません。");
        }
        fixedAssetRepository.delete(asset);
        fixedAssetRepository.flush();
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

    private FiscalYear ownedYear(Long appUserId, Integer year) {
        BusinessProfile profile = businessProfileRepository.findByAppUserId(appUserId)
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
