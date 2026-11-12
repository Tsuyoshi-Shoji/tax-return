package org.example.features.taxreturn.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseEntry;
import org.example.entity.FiscalYear;
import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;
import org.example.entity.HouseholdConsumption;
import org.example.entity.EmployeeSalaryDetail;
import org.example.entity.IncomeEntry;
import org.example.entity.SettlementAdjustment;
import org.example.entity.TaxDeductionRecord;
import org.example.entity.TaxDependent;
import org.example.entity.TaxReturnInput;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMapping.AccountRef;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.asset.DepreciationRules;
import org.example.features.taxreturn.form.TaxReturnForms.DepreciationRulesForm;
import org.example.features.taxreturn.balance.BalanceSheetService;
import org.example.features.taxreturn.consumption.ConsumptionTaxCalculator;
import org.example.features.taxreturn.dto.TaxReturnExtras;
import org.example.features.taxreturn.dto.TaxReturnExtras.AssetRow;
import org.example.features.taxreturn.dto.TaxReturnExtras.AssetEventRow;
import org.example.features.taxreturn.dto.TaxReturnExtras.DeductionRow;
import org.example.features.taxreturn.dto.TaxReturnExtras.EmployeeSalaryRow;
import org.example.features.taxreturn.dto.TaxReturnExtras.HouseholdConsumptionRow;
import org.example.features.taxreturn.dto.TaxReturnExtras.IncomeBreakdownRow;
import org.example.features.taxreturn.dto.TaxReturnExtras.LabelAmount;
import org.example.features.taxreturn.dto.TaxReturnExtras.MonthRow;
import org.example.features.taxreturn.dto.TaxReturnExtras.Option;
import org.example.features.taxreturn.dto.TaxReturnExtras.PayeeRow;
import org.example.features.taxreturn.dto.TaxReturnPageData.CheckItem;
import org.example.features.taxreturn.dto.TaxReturnPageData.StatementRow;
import org.example.features.taxreturn.form.TaxReturnForms.ReturnInputForm;
import org.example.features.taxreturn.incometax.IncomeTaxCalculator;
import org.example.features.taxreturn.incometax.TaxYearRules;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FixedAssetRepository;
import org.example.repository.FixedAssetEventRepository;
import org.example.repository.IncomeEntryRepository;
import org.example.repository.TaxDeductionRecordRepository;
import org.example.repository.TaxDependentRepository;
import org.example.repository.TaxReturnInputRepository;
import org.example.repository.HouseholdConsumptionRepository;
import org.example.repository.EmployeeSalaryDetailRepository;

/** 確定申告画面の拡張データと関連チェックを組み立てる（読み取り専用）。 */
class TaxReturnExtrasBuilder {
    /** 売上・仕入金額の明細に個別表示する取引先数（残りは「上記以外」に合算）。 */
    static final int PARTNER_ROWS = 5;
    static final Map<String, String> DEDUCTION_LABELS = new LinkedHashMap<>();
    static final Map<String, String> STATUS_LABELS = Map.of(
            "CANDIDATE", "未確認", "VERIFIED", "確認済み", "APPLIED", "申告に反映済み", "INELIGIBLE", "対象外");
    static final Map<String, String> METHOD_LABELS = Map.of(
            DepreciationCalculator.STRAIGHT_LINE, "定額法",
            DepreciationCalculator.DECLINING_200, "200％定率法",
            DepreciationCalculator.DECLINING_250, "250％定率法",
            DepreciationCalculator.OLD_STRAIGHT_LINE, "旧定額法",
            DepreciationCalculator.OLD_DECLINING, "旧定率法",
            DepreciationCalculator.SMALL_AMOUNT_SPECIAL, "少額減価償却資産の特例（措法28の2）",
            DepreciationCalculator.LUMP_SUM_3Y, "一括償却資産（3年均等）");

    static {
        DEDUCTION_LABELS.put("SOCIAL_INSURANCE", "社会保険料");
        DEDUCTION_LABELS.put("SOCIAL_INSURANCE_NHI", "社会保険料（国民健康保険）");
        DEDUCTION_LABELS.put("SOCIAL_INSURANCE_PENSION", "社会保険料（国民年金）");
        DEDUCTION_LABELS.put("SMALL_BUSINESS_MUTUAL", "小規模企業共済等掛金");
        DEDUCTION_LABELS.put("LIFE_INSURANCE_GENERAL", "生命保険料（新・一般）");
        DEDUCTION_LABELS.put("LIFE_INSURANCE_MEDICAL", "生命保険料（介護医療）");
        DEDUCTION_LABELS.put("LIFE_INSURANCE_PENSION", "生命保険料（新・個人年金）");
        DEDUCTION_LABELS.put("EARTHQUAKE_INSURANCE", "地震保険料");
        DEDUCTION_LABELS.put("MEDICAL", "医療費");
        DEDUCTION_LABELS.put("DONATION", "寄附金（特定寄附金）");
    }

    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetEventRepository fixedAssetEventRepository;
    private final TaxReturnInputRepository taxReturnInputRepository;
    private final TaxDependentRepository taxDependentRepository;
    private final TaxDeductionRecordRepository taxDeductionRecordRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final HouseholdConsumptionRepository householdConsumptionRepository;
    private final EmployeeSalaryDetailRepository employeeSalaryDetailRepository;

    TaxReturnExtrasBuilder(FixedAssetRepository fixedAssetRepository, FixedAssetEventRepository fixedAssetEventRepository,
            TaxReturnInputRepository taxReturnInputRepository,
            TaxDependentRepository taxDependentRepository, TaxDeductionRecordRepository taxDeductionRecordRepository,
            IncomeEntryRepository incomeEntryRepository, ExpenseEntryRepository expenseEntryRepository,
            HouseholdConsumptionRepository householdConsumptionRepository,
            EmployeeSalaryDetailRepository employeeSalaryDetailRepository) {
        this.fixedAssetRepository = fixedAssetRepository;
        this.fixedAssetEventRepository = fixedAssetEventRepository;
        this.taxReturnInputRepository = taxReturnInputRepository;
        this.taxDependentRepository = taxDependentRepository;
        this.taxDeductionRecordRepository = taxDeductionRecordRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
        this.householdConsumptionRepository = householdConsumptionRepository;
        this.employeeSalaryDetailRepository = employeeSalaryDetailRepository;
    }

    record Context(BusinessProfile profile, FiscalYear year, List<FiscalYear> fiscalYears, SettlementAdjustment settlement,
            AccountMapping mapping, BalanceSheetService.Result balance, List<StatementRow> statementRows,
            List<IncomeEntry> incomes, List<ExpenseEntry> expenses, List<HouseholdConsumption> householdConsumptions,
            BigDecimal plIncome, boolean closed, Long appUserId) {
    }

    TaxReturnExtras build(Context c, List<String> errors, List<String> warnings, List<CheckItem> checks) {
        TaxReturnExtras x = new TaxReturnExtras();
        fillFiler(c, x);
        fillOptions(c, x);
        fillAssets(c, x, errors, warnings, checks);
        fillStatementPages(c, x, warnings, checks);
        balanceChecks(c, warnings, errors);
        fillIncomeTax(c, x, warnings, checks);
        fillConsumption(c, x, errors, warnings);
        x.setRuleNote(TaxYearRules.SOURCE + "で確認（確認日：" + TaxYearRules.VERIFIED_ON + "）");
        return x;
    }

    private void fillFiler(Context c, TaxReturnExtras x) {
        AppUser user = c.profile().getAppUser();
        TaxReturnExtras.Filer f = x.getFiler();
        f.setName(user.getDisplayName());
        f.setNameKana(user.getNameKana());
        f.setBirthDate(user.getBirthDate() == null ? null : user.getBirthDate().toString());
        f.setAddress(user.getAddress());
        f.setPhone(user.getPhone());
        f.setBusinessName(c.profile().getBusinessName());
        f.setIndustry(c.profile().getIndustry());
        f.setTaxOffice(c.profile().getTaxOffice());
        f.setHouseholdHeadName(c.profile().getHouseholdHeadName());
        f.setHouseholdHeadRelation(c.profile().getHouseholdHeadRelation());
    }

    private void fillOptions(Context c, TaxReturnExtras x) {
        for (String code : AccountMasterSeed.FIXED_ASSET_ACCOUNTS) {
            x.getAssetAccountOptions().add(new Option(code, c.mapping().byCode(code).map(AccountRef::name).orElse(code)));
        }
        for (String type : List.of("SOCIAL_INSURANCE", "SMALL_BUSINESS_MUTUAL", "LIFE_INSURANCE_GENERAL",
                "LIFE_INSURANCE_MEDICAL", "LIFE_INSURANCE_PENSION", "EARTHQUAKE_INSURANCE", "MEDICAL", "DONATION")) {
            x.getDeductionTypeOptions().add(new Option(type, DEDUCTION_LABELS.get(type)));
        }
    }

    private void fillAssets(Context c, TaxReturnExtras x, List<String> errors, List<String> warnings, List<CheckItem> checks) {
        FiscalYear year = c.year();
        List<FixedAsset> assets = fixedAssetRepository.findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(c.profile().getId());
        List<FixedAssetEvent> events = fixedAssetEventRepository.findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(c.profile().getId());
        TaxReturnInput input = taxReturnInputRepository.findByFiscalYearId(year.getId()).orElse(null);
        DepreciationRulesForm rulesForm = new DepreciationRulesForm();
        rulesForm.setMethodBasis(input == null ? "UNCONFIRMED" : input.getDepreciationMethodBasis());
        rulesForm.setScopeConfirmed(DepreciationRules.confirmationMatches(year.getTaxYear(), assets, input, events));
        rulesForm.setSpecialConfirmed(input != null && input.isDepreciationSpecialConfirmed());
        rulesForm.setBusinessMonths(input == null ? "12" : Integer.toString(input.getDepreciationBusinessMonths()));
        rulesForm.setEvidence(input == null ? null : input.getDepreciationEvidence());
        x.setDepreciationRules(rulesForm);
        x.setDepreciationRuleVersion(DepreciationRules.version(year.getTaxYear()));
        x.setDepreciationConfirmedAt(input == null || input.getDepreciationConfirmedAt() == null
                ? null : input.getDepreciationConfirmedAt().toString());
        List<String> ruleErrors = DepreciationRules.annualErrors(year.getTaxYear(), assets, input, events);
        errors.addAll(ruleErrors);
        checks.add(new CheckItem("check-depreciation-rules", "償却ルール・届出等の利用者確認",
                ruleErrors.isEmpty(), ruleErrors.isEmpty() ? "登録範囲の機械検証と利用者確認（法的適格性の証明ではありません）"
                        : "未確認・対象外・上限超過のため出力できません"));
        BigDecimal smallAmountTotal = BigDecimal.ZERO;
        BigDecimal expenseFundedAcquisitions = BigDecimal.ZERO;
        for (FixedAsset asset : assets) {
            if (DepreciationRules.recognitionDate(asset, events).isAfter(year.getPeriodEnd())) {
                continue;
            }
            FixedAssetEvent disposal = events.stream()
                    .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                    .filter(e -> FixedAssetEvent.SALE.equals(e.getEventType()) || FixedAssetEvent.DISPOSAL.equals(e.getEventType()))
                    .findFirst().orElse(null);
            FixedAssetEvent cessation = events.stream()
                    .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                    .filter(e -> FixedAssetEvent.BUSINESS_CESSATION.equals(e.getEventType()))
                    .findFirst().orElse(null);
            boolean retain = DepreciationRules.retainsBalanceAfterDisposal(asset);
            if (disposal != null && disposal.getEffectiveDate().getYear() < year.getTaxYear() && !retain) {
                continue;
            }
            boolean ceasedEarlier = cessation != null && cessation.getEffectiveDate().isBefore(year.getPeriodStart());
            BigDecimal ratio = ratioForYear(asset, events, year.getPeriodStart());
            int calculationYear = ceasedEarlier ? cessation.getEffectiveDate().getYear() : year.getTaxYear();
            DepreciationCalculator.YearResult r = DepreciationCalculator.forYear(asset, calculationYear, ratio, events, assets);
            boolean disposedThisYear = disposal != null && disposal.getEffectiveDate().getYear() == year.getTaxYear()
                    && (FixedAssetEvent.SALE.equals(disposal.getEventType())
                        || FixedAssetEvent.DISPOSAL.equals(disposal.getEventType()));
            if (r.opening().signum() == 0 && r.depreciation().signum() == 0
                    && r.closing().signum() == 0 && !disposedThisYear) {
                continue;
            }
            String accountName = c.mapping().byCode(asset.getAccountCode()).map(AccountRef::name).orElse(asset.getAccountCode());
            String method = DepreciationRules.methodForYear(asset, year.getTaxYear(), events);
            BigDecimal opening = asset.isPrivateUseConversion()
                    && asset.getServiceStartDate().getYear() == year.getTaxYear() ? BigDecimal.ZERO
                            : ceasedEarlier ? r.closing() : r.opening();
            BigDecimal depreciation = ceasedEarlier ? BigDecimal.ZERO : r.depreciation();
            BigDecimal businessPortion = ceasedEarlier ? BigDecimal.ZERO : r.businessPortion();
            AssetRow row = new AssetRow(asset.getId(), asset.getAssetName(), accountName,
                    asset.getAcquisitionDate().toString(), asset.getServiceStartDate().toString(), asset.getAcquisitionCost(),
                    METHOD_LABELS.getOrDefault(method, method),
                    DepreciationCalculator.isRegular(method) ? asset.getUsefulLife() : null,
                    r.rate(), ceasedEarlier ? 0 : r.months(), opening, depreciation, ratio, businessPortion,
                    disposedThisYear && !retain ? BigDecimal.ZERO : r.closing(), asset.getNotes(), !c.closed());
            row.setCalculationDetails(DepreciationRules.calculationDetails(asset, r));
            FixedAssetEvent methodChange = events.stream()
                    .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                    .filter(e -> FixedAssetEvent.METHOD_CHANGE.equals(e.getEventType())
                            && e.getEffectiveDate().getYear() <= year.getTaxYear())
                    .max(java.util.Comparator.comparing(FixedAssetEvent::getEffectiveDate)).orElse(null);
            if (methodChange != null) {
                row.setCalculationDetails(row.getCalculationDetails() + "、方法変更="
                        + asset.getDepreciationMethod() + "→" + method + "、変更時簿価="
                        + methodChange.getOpeningBookValue().toPlainString());
            }
            if ("CAPITAL_IMPROVEMENT".equals(asset.getAssetOrigin())) {
                row.setCalculationDetails(row.getCalculationDetails() + "、資本的支出として別資産登録"
                        + (asset.getParentAsset() == null ? "" : "（対象資産=" + asset.getParentAsset().getAssetName() + "）"));
            }
            row.setDepreciationBasis(DepreciationRules.depreciationBasis(asset, r, year.getTaxYear(), events));
            x.getAssets().add(row);
            x.setDepreciationTotal(x.getDepreciationTotal().add(r.depreciation()));
            x.setDepreciationBusinessTotal(x.getDepreciationBusinessTotal().add(r.businessPortion()));
            x.setAssetOpeningTotal(x.getAssetOpeningTotal().add(opening));
            x.setAssetClosingTotal(x.getAssetClosingTotal().add(disposedThisYear && !retain ? BigDecimal.ZERO : r.closing()));
            if (cessation != null && !cessation.getEffectiveDate().isAfter(year.getPeriodEnd())
                    && r.closing().signum() > 0) {
                warnings.add("固定資産「" + asset.getAssetName() + "」は廃業時点で未償却残高が"
                        + yen(r.closing()) + "円あります。私用転用・譲渡等の処理は別途確認してください。");
            }
            boolean acquiredThisYear = !asset.getAcquisitionDate().isBefore(year.getPeriodStart());
            if (DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(asset.getDepreciationMethod())
                    && asset.getServiceStartDate().getYear() == year.getTaxYear()) {
                smallAmountTotal = smallAmountTotal.add(asset.getAcquisitionCost());
            }
            if (acquiredThisYear && "EXPENSE_ENTRY".equals(asset.getAcquisitionFunding())) {
                expenseFundedAcquisitions = expenseFundedAcquisitions.add(asset.getAcquisitionCost());
            }
        }
        for (FixedAssetEvent event : events) {
            if (event.getEffectiveDate().isAfter(year.getPeriodEnd())) {
                continue;
            }
            x.getFixedAssetEvents().add(new AssetEventRow(event.getId(), event.getFixedAsset().getAssetName(),
                    event.getEventType(), event.getEffectiveDate().toString(), event.getBusinessRatio(),
                    event.getProceeds(), event.getOpeningBookValue(), event.getNewDepreciationMethod(), !c.closed()));
        }
        String reason = null;
        if (smallAmountTotal.compareTo(DepreciationRules.annualCap(input == null ? 12 : input.getDepreciationBusinessMonths())) > 0) {
            warnings.add("少額減価償却資産の特例の取得価額合計が事業月数に応じた上限を超えています（" + yen(smallAmountTotal) + "）。通常の償却方法で登録し直してください。");
            reason = "少額減価償却資産の上限を超えています";
        }

        BigDecimal registeredPurchases = BigDecimal.ZERO;
        for (ExpenseEntry e : expenseEntryRepository.findAllOwnedByFiscalYear(year.getId(), c.appUserId())) {
            if (e.getExpenseItem() != null && "fixed-asset-purchase".equals(e.getExpenseItem().getItemCode()) && e.getAmount() != null) {
                registeredPurchases = registeredPurchases.add(e.getAmount());
            }
        }
        if (registeredPurchases.compareTo(expenseFundedAcquisitions) != 0) {
            warnings.add("支出登録の「固定資産購入」（" + yen(registeredPurchases) + "）と、固定資産台帳で「支出登録済み」とした当年取得資産の取得価額（"
                    + yen(expenseFundedAcquisitions) + "）が一致しません。");
            reason = reason == null ? "固定資産購入と台帳が一致しません" : reason;
        }
        SettlementAdjustment s = c.settlement();
        if (s != null && s.getDepreciation() != null && s.getDepreciation().signum() > 0 && assets.isEmpty()) {
            warnings.add("減価償却費は固定資産台帳から計算します。以前に決算整理で入力した減価償却費（" + yen(s.getDepreciation())
                    + "）は反映されていません。固定資産台帳に資産を登録してください。");
            reason = reason == null ? "固定資産台帳が未登録です" : reason;
        }
        checks.add(new CheckItem("check-depreciation", "減価償却", reason == null,
                reason != null ? reason : "固定資産" + x.getAssets().size() + "件（必要経費 " + yen(x.getDepreciationBusinessTotal()) + "）"));
    }

    private static BigDecimal ratioForYear(FixedAsset asset, List<FixedAssetEvent> events, java.time.LocalDate periodStart) {
        return events.stream()
                .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(e.getEventType()))
                .filter(e -> !e.getEffectiveDate().isAfter(periodStart))
                .max(java.util.Comparator.comparing(FixedAssetEvent::getEffectiveDate))
                .map(FixedAssetEvent::getBusinessRatio)
                .orElse(asset.getBusinessRatio());
    }

    private void fillStatementPages(Context c, TaxReturnExtras x, List<String> warnings, List<CheckItem> checks) {
        Map<String, BigDecimal> statement = new LinkedHashMap<>();
        for (StatementRow row : c.statementRows()) {
            statement.put(row.getCode(), row.getAmount());
        }
        x.setBusinessRevenue(statement.getOrDefault(AccountMasterSeed.LINE_SALES, BigDecimal.ZERO));
        x.setIncomeBeforeBlue(statement.getOrDefault(AccountMasterSeed.LINE_INCOME_BEFORE_BLUE, c.plIncome()));
        x.setBlueDeductionApplied(statement.getOrDefault(AccountMasterSeed.LINE_BLUE_DEDUCTION, BigDecimal.ZERO));
        x.setBusinessIncome(statement.getOrDefault(AccountMasterSeed.LINE_INCOME, c.plIncome()));
        x.setSalaries(statement.getOrDefault("SALARIES", BigDecimal.ZERO));
        x.setDedicatedSalary(statement.getOrDefault(AccountMasterSeed.LINE_DEDICATED_SALARY, BigDecimal.ZERO));

        BigDecimal[] sales = new BigDecimal[12];
        BigDecimal[] purchases = new BigDecimal[12];
        BigDecimal[] household = new BigDecimal[12];
        java.util.Arrays.fill(sales, BigDecimal.ZERO);
        java.util.Arrays.fill(purchases, BigDecimal.ZERO);
        java.util.Arrays.fill(household, BigDecimal.ZERO);
        Map<String, IncomeBreakdownRow> breakdown = new LinkedHashMap<>();
        Map<String, PayeeRow> salesPartners = new LinkedHashMap<>();
        Map<String, PayeeRow> purchasePartners = new LinkedHashMap<>();
        BigDecimal withholding = BigDecimal.ZERO;
        for (IncomeEntry e : c.incomes()) {
            String code = c.mapping().forIncomeItem(e.getIncomeItem(), e.getTransactionDate()).map(AccountRef::code).orElse(AccountMasterSeed.ACC_SALES);
            int m = e.getTransactionDate().getMonthValue() - 1;
            if (AccountMasterSeed.ACC_MISC_INCOME.equals(code)) {
                x.setMiscIncome(x.getMiscIncome().add(e.getAmount()));
            } else {
                sales[m] = sales[m].add(e.getAmount());
                partner(salesPartners, e.getCounterparty(), "（売上先未登録）").add(e.getAmount(), e.getAmount());
            }
            BigDecimal tax = e.getWithholdingTax() == null ? BigDecimal.ZERO : e.getWithholdingTax();
            if (tax.signum() > 0) {
                withholding = withholding.add(tax);
                String payer = e.getCounterparty() == null ? "（支払者未登録）" : e.getCounterparty().getCounterpartyName();
                String address = e.getCounterparty() == null ? null : e.getCounterparty().getAddress();
                String item = e.getIncomeItem() == null ? "売上" : e.getIncomeItem().getItemName();
                breakdown.computeIfAbsent(item + "|" + payer, k -> new IncomeBreakdownRow("事業（営業等）", item, payer, address))
                        .add(e.getAmount(), tax);
            }
        }
        for (HouseholdConsumption consumption : c.householdConsumptions()) {
            int month = consumption.getTransactionDate().getMonthValue() - 1;
            household[month] = household[month].add(consumption.getAmount());
            x.setHouseholdConsumptionTotal(x.getHouseholdConsumptionTotal().add(consumption.getAmount()));
            x.getHouseholdConsumptionRows().add(new HouseholdConsumptionRow(consumption.getId(),
                    consumption.getTransactionDate().toString(), consumption.getDetails(), consumption.getAmount(), !c.closed()));
        }
        for (FixedAssetEvent event : fixedAssetEventRepository
                .findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(c.profile().getId())) {
            if (FixedAssetEvent.SALE.equals(event.getEventType())
                    && event.getEffectiveDate().getYear() == c.year().getTaxYear()
                    && DepreciationRules.retainsBalanceAfterDisposal(event.getFixedAsset())) {
                BigDecimal ratio = ratioForYear(event.getFixedAsset(), fixedAssetEventRepository
                        .findByFixedAssetIdOrderByEffectiveDateAscIdAsc(event.getFixedAsset().getId()), c.year().getPeriodStart());
                x.setMiscIncome(x.getMiscIncome().add(event.getProceeds().multiply(ratio)
                        .divide(new BigDecimal("100"), 0, RoundingMode.DOWN)));
            }
        }
        x.setBusinessWithholding(withholding);
        x.getIncomeBreakdown().addAll(breakdown.values());

        Map<String, PayeeRow> rent = new LinkedHashMap<>();
        Map<String, PayeeRow> interest = new LinkedHashMap<>();
        Map<String, PayeeRow> fees = new LinkedHashMap<>();
        for (ExpenseEntry e : c.expenses()) {
            String code = c.mapping().forExpenseItem(e.getExpenseItem(), e.getTransactionDate()).map(AccountRef::code).orElse("");
            BigDecimal deductible = e.getDeductibleAmount() != null ? e.getDeductibleAmount() : e.getAmount();
            if (AccountMasterSeed.ACC_PURCHASES.equals(code)) {
                int m = e.getTransactionDate().getMonthValue() - 1;
                purchases[m] = purchases[m].add(deductible);
                partner(purchasePartners, e.getCounterparty(), "（仕入先未登録）").add(deductible, deductible);
            }
            Map<String, PayeeRow> target = null;
            if (AccountMasterSeed.ACC_RENT.equals(code)) {
                target = rent;
            } else if (AccountMasterSeed.ACC_INTEREST_DISCOUNT.equals(code)) {
                target = interest;
            } else if (e.getExpenseItem() != null && ("tax-accountant".equals(e.getExpenseItem().getItemCode())
                    || "legal-professional".equals(e.getExpenseItem().getItemCode()))) {
                target = fees;
            }
            if (target != null) {
                String name = e.getCounterparty() == null ? "（支払先未登録）" : e.getCounterparty().getCounterpartyName();
                String address = e.getCounterparty() == null ? null : e.getCounterparty().getAddress();
                String description = e.getExpenseItem() == null ? null : e.getExpenseItem().getItemName();
                target.computeIfAbsent(name, k -> new PayeeRow(name, address, description)).add(e.getAmount(), deductible);
            }
        }
        for (int i = 0; i < 12; i++) {
            x.getMonthlySalesPurchases().add(new MonthRow(i + 1, sales[i], purchases[i], household[i]));
            x.setSalesTotal(x.getSalesTotal().add(sales[i]));
            x.setPurchaseTotal(x.getPurchaseTotal().add(purchases[i]));
        }
        x.getRentRows().addAll(rent.values());
        x.getInterestRows().addAll(interest.values());
        x.getFeeRows().addAll(fees.values());
        x.setSalesPartnerOther(topPartners(salesPartners, x.getSalesPartnerRows()));
        x.setPurchasePartnerOther(topPartners(purchasePartners, x.getPurchasePartnerRows()));
        BigDecimal salaryDetailTotal = BigDecimal.ZERO;
        BigDecimal dedicatedSalaryDetailTotal = BigDecimal.ZERO;
        for (EmployeeSalaryDetail detail : employeeSalaryDetailRepository
                .findByFiscalYearIdOrderBySalaryKindAscPersonNameAscIdAsc(c.year().getId())) {
            x.getEmployeeSalaryRows().add(new EmployeeSalaryRow(detail.getId(), detail.getSalaryKind(),
                    detail.getPersonName(), detail.getJobDescription(), detail.getWorkPeriodStart().toString(),
                    detail.getWorkPeriodEnd().toString(), detail.getPaymentPeriod(), detail.getAmount(), !c.closed()));
            if ("DEDICATED_SALARY".equals(detail.getSalaryKind())) {
                dedicatedSalaryDetailTotal = dedicatedSalaryDetailTotal.add(detail.getAmount());
            } else {
                salaryDetailTotal = salaryDetailTotal.add(detail.getAmount());
            }
        }
        boolean salaryDetailMatch = x.getEmployeeSalaryRows().isEmpty()
                ? x.getSalaries().signum() == 0 && x.getDedicatedSalary().signum() == 0
                : salaryDetailTotal.compareTo(x.getSalaries()) == 0
                        && dedicatedSalaryDetailTotal.compareTo(x.getDedicatedSalary()) == 0;
        if (!salaryDetailMatch) {
            warnings.add("給料賃金・専従者給与の個人別明細合計と会計データの決算書金額が一致しません。登録内容と給与仕訳を確認してください。");
        }
        checks.add(new CheckItem("check-salary-details", "給与の個人別内訳", salaryDetailMatch,
                salaryDetailMatch ? "会計データと一致" : "個人別内訳合計と会計データが一致しません"));
    }

    private static PayeeRow partner(Map<String, PayeeRow> partners, org.example.entity.Counterparty counterparty,
            String unknownLabel) {
        String name = counterparty == null ? unknownLabel : counterparty.getCounterpartyName();
        String address = counterparty == null ? null : counterparty.getAddress();
        return partners.computeIfAbsent(name, k -> new PayeeRow(name, address, null));
    }

    /** 金額の大きい順に上位 {@link #PARTNER_ROWS} 件を明細へ入れ、残りの合計（上記以外）を返す。 */
    private static BigDecimal topPartners(Map<String, PayeeRow> partners, List<PayeeRow> target) {
        List<PayeeRow> sorted = new java.util.ArrayList<>(partners.values());
        sorted.sort(java.util.Comparator.comparing(PayeeRow::getAmount).reversed().thenComparing(PayeeRow::getName));
        BigDecimal other = BigDecimal.ZERO;
        for (int i = 0; i < sorted.size(); i++) {
            if (i < PARTNER_ROWS) {
                target.add(sorted.get(i));
            } else {
                other = other.add(sorted.get(i).getAmount());
            }
        }
        return other;
    }

    private void balanceChecks(Context c, List<String> warnings, List<String> errors) {
        BalanceSheetService.Result b = c.balance();
        if (b.getProfitBeforeBlue().compareTo(c.plIncome()) != 0) {
            errors.add("仕訳から計算した所得（" + yen(b.getProfitBeforeBlue()) + "）と損益計算書の所得（" + yen(c.plIncome())
                    + "）が一致しません。仕訳が作成されていない取引がないか、取引を開いて再保存してください。");
        }
        if (!b.getUnclassifiedAccounts().isEmpty()) {
            warnings.add("勘定科目が特定できない仕訳があります（" + String.join("、", b.getUnclassifiedAccounts())
                    + "）。貸借対照表では「未分類」として表示しています。");
        }
        SettlementAdjustment s = c.settlement();
        if (s != null && s.isBalancesConfirmed() && s.getClosingCash() != null && s.getClosingCash().signum() > 0) {
            BigDecimal book = b.closingOf(AccountMasterSeed.ACC_CASH).add(b.closingOf(AccountMasterSeed.ACC_BANK_DEPOSIT));
            if (book.compareTo(s.getClosingCash()) != 0) {
                warnings.add("決算整理の期末現預金（" + yen(s.getClosingCash()) + "）と帳簿上の現金・普通預金の期末残高（" + yen(book)
                        + "）が一致しません。通帳・現金残高と取引の登録内容をご確認ください。");
            }
        }
        if (b.isCarriedForward() && s != null
                && safe(s.getOpeningInventory()).compareTo(b.openingOf(AccountMasterSeed.ACC_INVENTORY)) != 0) {
            warnings.add("期首棚卸（" + yen(safe(s.getOpeningInventory())) + "）が前年の期末棚卸資産（"
                    + yen(b.openingOf(AccountMasterSeed.ACC_INVENTORY)) + "）と一致しません。");
        }
    }

    private void fillIncomeTax(Context c, TaxReturnExtras x, List<String> warnings, List<CheckItem> checks) {
        FiscalYear year = c.year();
        TaxReturnInput input = taxReturnInputRepository.findByFiscalYearId(year.getId()).orElse(null);
        List<TaxDependent> dependents = taxDependentRepository.findByFiscalYearIdOrderByBirthDateAscIdAsc(year.getId());
        List<TaxDeductionRecord> records = taxDeductionRecordRepository.findByFiscalYearIdOrderByIdAsc(year.getId());
        x.setReturnInput(toForm(input));
        x.getDependents().addAll(dependents);
        int candidates = 0;
        Map<String, Long> social = new LinkedHashMap<>();
        for (TaxDeductionRecord r : records) {
            x.getDeductions().add(new DeductionRow(r.getId(), DEDUCTION_LABELS.getOrDefault(r.getDeductionType(), r.getDeductionType()),
                    r.getPaidDate() == null ? null : r.getPaidDate().toString(), r.getPaidAmount(), r.getReimbursedAmount(),
                    r.getPayer(), r.getNotes(), r.getStatus(), STATUS_LABELS.getOrDefault(r.getStatus(), r.getStatus()),
                    r.getSourceTransaction() != null));
            if ("CANDIDATE".equals(r.getStatus())) {
                candidates++;
            }
            boolean applied = "VERIFIED".equals(r.getStatus()) || "APPLIED".equals(r.getStatus());
            if (applied && r.getDeductionType() != null && r.getDeductionType().startsWith("SOCIAL_INSURANCE")) {
                social.merge(DEDUCTION_LABELS.getOrDefault(r.getDeductionType(), "社会保険料"),
                        r.getPaidAmount() == null ? 0 : r.getPaidAmount().longValue(), Long::sum);
            }
        }
        social.forEach((label, amount) -> x.getSocialInsuranceRows().add(new LabelAmount(label, amount)));
        if (input != null && input.getSalaryRevenue() != null && input.getSalaryRevenue().signum() > 0) {
            IncomeBreakdownRow salary = new IncomeBreakdownRow("給与", "給料", nvl(input.getSalaryPayerName()), input.getSalaryPayerAddress());
            salary.add(input.getSalaryRevenue(), input.getSalaryWithholding());
            x.getIncomeBreakdown().add(salary);
        }
        if (input != null && input.getMiscWithholding() != null && input.getMiscWithholding().signum() > 0) {
            IncomeBreakdownRow misc = new IncomeBreakdownRow("雑（その他）", "", nvl(input.getMiscPayerName()), null);
            misc.add(input.getMiscRevenue(), input.getMiscWithholding());
            x.getIncomeBreakdown().add(misc);
        }
        if (candidates > 0) {
            warnings.add("未確認の所得控除候補が" + candidates + "件あります。証憑を確認し「確認済み」にした控除のみ税額計算に反映します。");
        }
        TaxYearRules rules = TaxYearRules.of(year.getTaxYear()).orElse(null);
        if (rules == null) {
            x.setTaxUnsupportedReason(year.getTaxYear() + "年分の税額計算ルールは未登録です（対応年分：2024〜2026年分）。");
            warnings.add(x.getTaxUnsupportedReason());
            checks.add(new CheckItem("check-income-tax", "所得控除・税額", false, "対象外の年分です"));
            return;
        }
        boolean hasCapitalAssetSale = fixedAssetEventRepository
                .findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(c.profile().getId()).stream()
                .anyMatch(e -> FixedAssetEvent.SALE.equals(e.getEventType())
                        && e.getEffectiveDate().getYear() == year.getTaxYear()
                        && !DepreciationRules.retainsBalanceAfterDisposal(e.getFixedAsset()));
        if (hasCapitalAssetSale) {
            x.setTaxUnsupportedReason("固定資産売却の譲渡所得等は未対応のため、この年分の所得税額は表示しません。別途申告内容を確認してください。");
            warnings.add(x.getTaxUnsupportedReason());
            checks.add(new CheckItem("check-income-tax", "所得控除・税額", false, "固定資産の譲渡所得等を別途確認してください"));
            return;
        }
        IncomeTaxCalculator.Result tax = IncomeTaxCalculator.calculate(new IncomeTaxCalculator.Input(rules,
                x.getBusinessRevenue().longValue(), x.getBusinessIncome().longValue(), x.getBusinessWithholding().longValue(),
                input, dependents, records));
        x.setTax(tax);
        String reason = null;
        if (c.profile().getAppUser().getBirthDate() == null || blank(c.profile().getTaxOffice())
                || blank(c.profile().getAppUser().getAddress())) {
            warnings.add("確定申告書の作成に必要な申告者情報（生年月日・住所・提出先税務署）が未設定です。各種設定で登録してください。");
            reason = "申告者情報が未設定です";
        }
        if (candidates > 0) {
            reason = reason == null ? "未確認の控除候補が" + candidates + "件あります" : reason;
        }
        checks.add(new CheckItem("check-income-tax", "所得控除・税額", reason == null, reason == null ? "入力完了" : reason));
    }

    private void fillConsumption(Context c, TaxReturnExtras x, List<String> errors, List<String> warnings) {
        String status = c.profile().getConsumptionTaxStatus() == null ? "EXEMPT" : c.profile().getConsumptionTaxStatus();
        x.setConsumptionStatus(status);
        BigDecimal base = null;
        TaxReturnInput input = taxReturnInputRepository.findByFiscalYearId(c.year().getId()).orElse(null);
        if (input != null && input.getBasePeriodTaxableSales() != null) {
            base = input.getBasePeriodTaxableSales();
            x.setBasePeriodSource("入力値");
        } else {
            FiscalYear basePeriod = c.fiscalYears().stream()
                    .filter(f -> f.getTaxYear() == c.year().getTaxYear() - 2).findFirst().orElse(null);
            if (basePeriod != null) {
                List<IncomeEntry> entries = incomeEntryRepository.findAllOwnedByFiscalYear(basePeriod.getId(), c.appUserId()).stream()
                        .filter(e -> !e.isExcludeFromReturn() && e.getAmount() != null).toList();
                base = ConsumptionTaxCalculator.taxableSalesExcludingTax(entries);
                x.setBasePeriodSource(basePeriod.getTaxYear() + "年分の帳簿から計算（税抜）");
            }
        }
        x.setBasePeriodSales(base);
        if (!"SIMPLIFIED".equals(status)) {
            x.getConsumptionNotes().add("各種設定で免税事業者として登録されています。消費税の申告書は作成しません。");
            if (base != null && base.compareTo(ConsumptionTaxCalculator.EXEMPTION_THRESHOLD) > 0) {
                warnings.add("基準期間の課税売上高が1,000万円を超えています（" + yen(base) + "）。課税事業者に該当する可能性があります。");
            }
            return;
        }
        Integer type = c.profile().getSimplifiedBusinessType();
        if (type == null || !ConsumptionTaxCalculator.deemedRates().containsKey(type)) {
            errors.add("簡易課税の事業区分が未設定です。各種設定で登録してください。");
            return;
        }
        if (base != null && base.compareTo(ConsumptionTaxCalculator.SIMPLIFIED_UPPER_LIMIT) > 0) {
            errors.add("基準期間の課税売上高が5,000万円を超えているため、簡易課税制度は適用できません（このアプリは本則課税に対応していません）。");
            return;
        }
        if (base == null) {
            x.getConsumptionNotes().add("基準期間の課税売上高が不明です。申告情報に入力すると簡易課税の適用可否（5,000万円以下）を確認します。");
        }
        x.setConsumption(ConsumptionTaxCalculator.calculate(c.incomes(), type));
        x.getConsumptionNotes().add("簡易課税（第" + type + "種事業・みなし仕入率" + ConsumptionTaxCalculator.deemedRates().get(type)
                + "%）で計算しています。売上に係る対価の返還等・貸倒れ・中間納付額は反映していません。");
    }

    private static ReturnInputForm toForm(TaxReturnInput in) {
        ReturnInputForm f = new ReturnInputForm();
        f.setSpouseCohabiting(true);
        if (in == null) {
            return f;
        }
        f.setSalaryRevenue(plain(in.getSalaryRevenue()));
        f.setSalaryWithholding(plain(in.getSalaryWithholding()));
        f.setSalaryPayerName(in.getSalaryPayerName());
        f.setSalaryPayerAddress(in.getSalaryPayerAddress());
        f.setMiscRevenue(plain(in.getMiscRevenue()));
        f.setMiscExpense(plain(in.getMiscExpense()));
        f.setMiscWithholding(plain(in.getMiscWithholding()));
        f.setMiscPayerName(in.getMiscPayerName());
        f.setPrepaidTax(plain(in.getPrepaidTax()));
        f.setSelfDisability(in.getSelfDisability());
        f.setWidowStatus(in.getWidowStatus());
        f.setWorkingStudent(in.isWorkingStudent());
        f.setHasSpouse(in.isHasSpouse());
        f.setSpouseName(in.getSpouseName());
        f.setSpouseBirthDate(in.getSpouseBirthDate() == null ? null : in.getSpouseBirthDate().toString());
        f.setSpouseIncome(plain(in.getSpouseIncome()));
        f.setSpouseDisability(in.getSpouseDisability());
        f.setSpouseCohabiting(in.isSpouseCohabiting());
        f.setBasePeriodTaxableSales(in.getBasePeriodTaxableSales() == null ? null : plain(in.getBasePeriodTaxableSales()));
        return f;
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.setScale(0, java.math.RoundingMode.DOWN).toPlainString();
    }

    private static String nvl(String v) {
        return v == null ? "（未入力）" : v;
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String yen(BigDecimal v) {
        return new DecimalFormat("#,##0").format(v) + "円";
    }
}
