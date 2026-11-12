package org.example.features.taxreturn.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.example.entity.AppUser;
import org.example.entity.AccountingTransaction;
import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseEntry;
import org.example.entity.FiscalYear;
import org.example.entity.IncomeEntry;
import org.example.entity.JournalEntry;
import org.example.entity.SettlementAdjustment;
import org.example.entity.TransactionSettlement;
import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;
import org.example.entity.HouseholdConsumption;
import org.example.entity.EmployeeSalaryDetail;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.asset.DepreciationRules;
import org.example.features.taxreturn.export.BookExportFilter;
import org.example.features.taxreturn.balance.BalanceSheetService;
import org.example.features.taxreturn.dto.TaxReturnExtras;
import org.example.features.taxreturn.incometax.BlueReturnEdition;
import org.example.features.taxreturn.requirements.FilingRequirements;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.repository.FixedAssetRepository;
import org.example.repository.FixedAssetEventRepository;
import org.example.repository.HouseholdConsumptionRepository;
import org.example.repository.EmployeeSalaryDetailRepository;
import org.example.repository.TaxDeductionRecordRepository;
import org.example.repository.TaxDependentRepository;
import org.example.repository.TaxReturnInputRepository;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMapping.AccountRef;
import org.example.features.accounting.AccountMapping.StatementLayout;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.accounting.service.AccountMappingService;
import org.example.entity.StatementLine;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.report.dto.ReportMonthRow;
import org.example.features.report.dto.ReportYearOption;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.dto.TaxReturnPageData.BasicInfo;
import org.example.features.taxreturn.dto.TaxReturnPageData.BsRow;
import org.example.features.taxreturn.dto.TaxReturnPageData.CheckItem;
import org.example.features.taxreturn.dto.TaxReturnPageData.Diff;
import org.example.features.taxreturn.dto.TaxReturnPageData.JournalLine;
import org.example.features.taxreturn.dto.TaxReturnPageData.LedgerAccount;
import org.example.features.taxreturn.dto.TaxReturnPageData.LedgerLine;
import org.example.features.taxreturn.dto.TaxReturnPageData.PlRow;
import org.example.features.taxreturn.dto.TaxReturnPageData.StatementRow;
import org.example.features.taxreturn.form.SettlementForm;
import org.example.features.taxreturn.service.TaxReturnService;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FiscalYearClosingEventRepository;
import org.example.entity.FiscalYearClosingEvent;
import org.example.features.taxreturn.closing.YearSnapshot;
import org.example.repository.FiscalYearRepository;
import org.example.repository.IncomeEntryRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.SettlementAdjustmentRepository;
import org.example.repository.TransactionSettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxReturnServiceImpl implements TaxReturnService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999");
    private static final int JOURNAL_PREVIEW_SIZE = 5;
    private static final String CLOSED = "closed";
    private static final String DRAFT = "draft";
    private static final String UNCATEGORIZED = "未分類";
    private static final String DEPRECIATION_LABEL = "減価償却費";
    private static final String INVENTORY_LABEL = "棚卸調整";
    private static final String OWNER_DRAW_LABEL = "事業主貸";
    private static final String COMPOUND_COUNTER_LABEL = "諸口";
    private static final String SETTLEMENT_STATUS_SETTLED = "SETTLED";
    private static final String SETTLEMENT_STATUS_UNSETTLED = "UNSETTLED";
    private static final String SETTLEMENT_STATUS_PARTIALLY_SETTLED = "PARTIALLY_SETTLED";
    private static final String SOURCE_TYPE_SETTLEMENT = "SETTLEMENT_ADJUSTMENT";
    private static final String DEDUCTION_METHOD_UNCONFIRMED = "UNCONFIRMED";
    private static final String DEDUCTION_METHOD_ETAX = "ETAX";
    private static final String DEDUCTION_METHOD_QUALIFIED_EBOOKS = "QUALIFIED_EBOOKS";
    private static final String DEDUCTION_METHOD_NONE = "NONE";
    private static final String SECTION_REVENUE = "REVENUE";
    private static final String SECTION_EXPENSE = "EXPENSE";
    private static final String SECTION_ADJUSTMENT = "ADJUSTMENT";
    private static final String SETTLEMENT_SOURCE_LABEL = "決算整理";
    private static final Set<String> TOTAL_LINES = Set.of(
            AccountMasterSeed.LINE_GROSS_PROFIT, AccountMasterSeed.LINE_EXPENSE_TOTAL,
            AccountMasterSeed.LINE_INCOME_BEFORE_BLUE, AccountMasterSeed.LINE_INCOME);

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final AccountingTransactionRepository transactionRepository;
    private final SettlementAdjustmentRepository settlementRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TransactionSettlementRepository transactionSettlementRepository;
    private final AccountMappingService accountMappingService;
    private final BalanceSheetService balanceSheetService;
    private final ClosingJournalService closingJournalService;
    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetEventRepository fixedAssetEventRepository;
    private final HouseholdConsumptionRepository householdConsumptionRepository;
    private final EmployeeSalaryDetailRepository employeeSalaryDetailRepository;
    private final TaxReturnInputRepository taxReturnInputRepository;
    private final TaxDependentRepository taxDependentRepository;
    private final TaxDeductionRecordRepository taxDeductionRecordRepository;
    private final FiscalYearClosingEventRepository closingEventRepository;

    public TaxReturnServiceImpl(
            BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository,
            IncomeEntryRepository incomeEntryRepository,
            ExpenseEntryRepository expenseEntryRepository,
            AccountingTransactionRepository transactionRepository,
            SettlementAdjustmentRepository settlementRepository,
            JournalEntryRepository journalEntryRepository,
            TransactionSettlementRepository transactionSettlementRepository,
            AccountMappingService accountMappingService,
            BalanceSheetService balanceSheetService,
            ClosingJournalService closingJournalService,
            FixedAssetRepository fixedAssetRepository,
            FixedAssetEventRepository fixedAssetEventRepository,
            HouseholdConsumptionRepository householdConsumptionRepository,
            EmployeeSalaryDetailRepository employeeSalaryDetailRepository,
            TaxReturnInputRepository taxReturnInputRepository,
            TaxDependentRepository taxDependentRepository,
            TaxDeductionRecordRepository taxDeductionRecordRepository,
            FiscalYearClosingEventRepository closingEventRepository) {
        this.closingEventRepository = closingEventRepository;
        this.balanceSheetService = balanceSheetService;
        this.closingJournalService = closingJournalService;
        this.fixedAssetRepository = fixedAssetRepository;
        this.fixedAssetEventRepository = fixedAssetEventRepository;
        this.householdConsumptionRepository = householdConsumptionRepository;
        this.employeeSalaryDetailRepository = employeeSalaryDetailRepository;
        this.taxReturnInputRepository = taxReturnInputRepository;
        this.taxDependentRepository = taxDependentRepository;
        this.taxDeductionRecordRepository = taxDeductionRecordRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
        this.transactionRepository = transactionRepository;
        this.settlementRepository = settlementRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.transactionSettlementRepository = transactionSettlementRepository;
        this.accountMappingService = accountMappingService;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxReturnPageData load(Long appUserId, Integer year, SettlementForm overrideForm) {
        BusinessProfile profile = loadProfile(appUserId);
        List<FiscalYear> fiscalYears = fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId());
        FiscalYear selected = resolve(fiscalYears, year);
        AccountMapping mapping = accountMappingService.snapshot();
        SettlementAdjustment saved = settlementRepository.findByFiscalYearId(selected.getId()).orElse(null);
        Calc current = calculate(selected, appUserId, saved, mapping);

        FiscalYear prior = fiscalYears.stream()
                .filter(f -> f.getTaxYear() == selected.getTaxYear() - 1)
                .findFirst().orElse(null);
        Calc priorCalc = prior == null ? null
                : calculate(prior, appUserId, settlementRepository.findByFiscalYearId(prior.getId()).orElse(null), mapping);

        int deduction = saved == null ? 65 : saved.getBlueDeduction();
        BigDecimal deductionAmount = BigDecimal.valueOf(deduction * 10000L);
        BigDecimal afterDeduction = current.income.subtract(deductionAmount).max(BigDecimal.ZERO);
        Statement statement = buildStatement(
                mapping.layout(AccountMasterSeed.FORM_BLUE_GENERAL_PL, selected.getTaxYear()), current, saved, deductionAmount);

        BalanceSheetService.Result bsResult = balanceSheetService.compute(selected, mapping);
        BalanceSheet bs = balanceSheet(bsResult);
        List<ReportYearOption> options = fiscalYears.stream()
                .map(f -> new ReportYearOption(f.getTaxYear(), yearLabel(f.getTaxYear())))
                .toList();
        List<ReportMonthRow> months = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            months.add(new ReportMonthRow(i + 1, current.incomeByMonth[i], current.expenseByMonth[i], 0, 0));
        }

        long draftCount = journalEntryRepository.countByFiscalYearIdAndStatus(selected.getId(), DRAFT)
                + transactionRepository.countDraftsWithoutJournal(selected.getId());
        long unsettledCount = transactionRepository
                .findByFiscalYearIdAndSettlementStatusInOrderByTransactionDateAscIdAsc(
                        selected.getId(),
                        List.of(SETTLEMENT_STATUS_UNSETTLED, SETTLEMENT_STATUS_PARTIALLY_SETTLED))
                .size();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<CheckItem> checks = new ArrayList<>();
        BasicInfo basic = basicInfo(profile, deduction);
        buildChecks(basic, current, saved, bs, draftCount, unsettledCount, deduction, months, errors, warnings, checks);
        buildTransactionJournalChecks(selected.getId(), errors, checks);
        buildMappingChecks(current, statement, errors, warnings, checks);
        TaxReturnExtras extras = extrasBuilder().build(new TaxReturnExtrasBuilder.Context(
                profile, selected, fiscalYears, saved, mapping, bsResult, statement.rows,
                current.incomeEntries, current.expenseEntries, current.householdConsumptions, current.income,
                CLOSED.equals(selected.getStatus()), appUserId), errors, warnings, checks);
        BlueReturnEdition edition = BlueReturnEdition.forTaxYear(selected.getTaxYear());
        FilingRequirements.Result filingRequirements = applyFilingRequirements(
                profile, saved, deduction, current, bs, draftCount, extras, edition, errors, warnings, checks);

        SettlementForm form = overrideForm != null ? overrideForm : toForm(saved);
        TaxReturnPageData page = new TaxReturnPageData(
                options, selected.getTaxYear(), yearLabel(selected.getTaxYear()), CLOSED.equals(selected.getStatus()),
                basic, deduction,
                current.sales, current.expenses, current.income, afterDeduction,
                diff(current.sales, priorCalc == null ? null : priorCalc.sales, true),
                diff(current.expenses, priorCalc == null ? null : priorCalc.expenses, false),
                diff(current.income, priorCalc == null ? null : priorCalc.income, true),
                months, current.plRows, statement.rows, form,
                bs.assets, bs.liabilities, bs.assetsOpening, bs.assetsClosing, bs.liabilitiesOpening, bs.liabilitiesClosing,
                journalPreview(current.journal), current.journal, ledger(current.journal),
                checks, errors, warnings);
        page.setBlueDeductionRequirementsConfirmed(
                saved != null && saved.isBlueDeductionRequirementsConfirmed());
        page.setBlueDeduction65Method(saved == null ? DEDUCTION_METHOD_UNCONFIRMED : saved.getBlueDeduction65Method());
        page.setExtras(extras);
        page.setBalanceCarriedForward(bsResult.isCarriedForward());
        page.setBlueReturnEdition(edition);
        page.setFilingRequirements(filingRequirements);
        applyClosingIntegrity(selected, page);
        return page;
    }

    /** 確定済み年度の現データが締め時点の指紋と一致するかを確認し、不一致なら出力を止める。 */
    private void applyClosingIntegrity(FiscalYear selected, TaxReturnPageData page) {
        if (!CLOSED.equals(selected.getStatus())) {
            return;
        }
        FiscalYearClosingEvent close = closingEventRepository
                .findFirstByFiscalYearIdAndEventTypeOrderByCloseVersionDesc(selected.getId(), FiscalYearClosingEvent.CLOSE)
                .orElse(null);
        if (close == null) {
            page.getWarnings().add("この年度は締め記録がない確定済み年度です。締め時点との一致を確認できません。");
            page.getChecks().add(new CheckItem("check-closing-integrity", "締め時点との一致", false,
                    "締め記録がないため確認できません。"));
            return;
        }
        boolean intact = YearSnapshot.of(page).matches(close.getDataFingerprint());
        if (!intact) {
            page.getErrors().add("確定済み年度のデータが締め時点（第" + close.getCloseVersion()
                    + "版）から変化しています。締めを取消して内容を確認し、訂正理由を付けて再度締めてください。");
        }
        page.getChecks().add(new CheckItem("check-closing-integrity", "締め時点との一致", intact,
                intact ? "締め第" + close.getCloseVersion() + "版と一致しています。"
                        : "締め第" + close.getCloseVersion() + "版と一致しません。"));
    }

    private TaxReturnExtrasBuilder extrasBuilder() {
        return new TaxReturnExtrasBuilder(fixedAssetRepository, fixedAssetEventRepository,
                taxReturnInputRepository, taxDependentRepository,
                taxDeductionRecordRepository, incomeEntryRepository, expenseEntryRepository,
                householdConsumptionRepository, employeeSalaryDetailRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public BooksExport exportBooks(Long appUserId, Integer year) {
        return exportBooks(appUserId, year, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BooksExport exportBooks(Long appUserId, Integer year, String fromDate, String toDate, String accountCode) {
        TaxReturnPageData page = load(appUserId, year, null);
        FiscalYear selected = loadOwnedFiscalYear(appUserId, page.getSelectedYear());
        BookExportFilter filter = exportFilter(page, selected, fromDate, toDate, accountCode);
        List<JournalEntry> entries = new ArrayList<>(new LinkedHashSet<>(
                journalEntryRepository.findAllWithLinesByFiscalYearId(selected.getId())));
        if (entries.isEmpty() && !page.getJournalAll().isEmpty()) {
            page.getErrors().add("保存された仕訳がないため帳簿を出力できません。");
        }
        return new BooksExport(page, page.getErrors().isEmpty() && filter != null
                ? org.example.features.taxreturn.export.BooksCsv.build(page, entries,
                        accountMappingService.snapshot(), fixedAssetEventRepository
                                .findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(selected.getBusinessProfile().getId()), filter)
                : "");
    }

    @Override
    @Transactional(readOnly = true)
    public BooksExport exportFixedAssetLedger(Long appUserId, Integer year) {
        return exportFixedAssetLedger(appUserId, year, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BooksExport exportFixedAssetLedger(Long appUserId, Integer year, String fromDate, String toDate,
            String accountCode) {
        TaxReturnPageData page = load(appUserId, year, null);
        FiscalYear selected = loadOwnedFiscalYear(appUserId, page.getSelectedYear());
        BookExportFilter filter = exportFilter(page, selected, fromDate, toDate, accountCode);
        Long profileId = selected.getBusinessProfile().getId();
        return new BooksExport(page, page.getErrors().isEmpty() && filter != null
                ? org.example.features.taxreturn.export.FixedAssetLedgerCsv.build(selected.getTaxYear(),
                        fixedAssetRepository.findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(profileId),
                        fixedAssetEventRepository.findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(profileId),
                        accountMappingService.snapshot(), filter)
                : "");
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookAccountOption> availableBookAccounts() {
        return accountMappingService.snapshot().accounts().stream()
                .map(account -> new BookAccountOption(account.code(), account.name()))
                .toList();
    }

    private BookExportFilter exportFilter(TaxReturnPageData page, FiscalYear year, String fromDate, String toDate,
            String accountCode) {
        LocalDate from = parseExportDate(page, fromDate, "開始日");
        LocalDate to = parseExportDate(page, toDate, "終了日");
        if (!page.getErrors().isEmpty()) return null;
        from = from == null ? year.getPeriodStart() : from;
        to = to == null ? year.getPeriodEnd() : to;
        if (from.isAfter(to)) {
            page.getErrors().add("帳簿出力の開始日は終了日以前の日付を指定してください。");
        } else if (from.isBefore(year.getPeriodStart()) || to.isAfter(year.getPeriodEnd())) {
            page.getErrors().add("帳簿出力の期間は選択年度内で指定してください。");
        }
        String normalizedAccount = accountCode == null || accountCode.isBlank() ? null : accountCode.strip();
        if (normalizedAccount != null && accountMappingService.snapshot().byCode(normalizedAccount).isEmpty()) {
            page.getErrors().add("存在しない勘定科目が指定されています。科目を選び直してください。");
        }
        if (!page.getErrors().isEmpty()) return null;
        return new BookExportFilter(from, to, normalizedAccount,
                from.equals(year.getPeriodStart()) && to.equals(year.getPeriodEnd()));
    }

    private LocalDate parseExportDate(TaxReturnPageData page, String value, String label) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException ex) {
            page.getErrors().add("帳簿出力の" + label + "はYYYY-MM-DD形式の日付を指定してください。");
            return null;
        }
    }

    @Override
    @Transactional
    public List<String> saveSettlement(Long appUserId, Integer year, SettlementForm form) {
        FiscalYear fiscalYear = loadOwnedFiscalYear(appUserId, year);
        if (CLOSED.equals(fiscalYear.getStatus())) {
            return List.of("確定済みの年度は編集できません。");
        }
        List<String> errors = new ArrayList<>();
        BigDecimal openingInventory = parse("期首棚卸", form.getOpeningInventory(), errors);
        BigDecimal closingInventory = parse("期末棚卸", form.getClosingInventory(), errors);
        BigDecimal receivable = parse("売掛金", form.getAccountsReceivable(), errors);
        BigDecimal payable = parse("買掛金", form.getAccountsPayable(), errors);
        BigDecimal unpaid = parse("未払金", form.getUnpaidExpenses(), errors);
        BigDecimal unreceived = parse("未収入金", form.getUnreceivedIncome(), errors);
        BigDecimal openingBalance = parse("期首残高", form.getOpeningBalance(), errors);
        BigDecimal closingCash = parse("期末現預金", form.getClosingCash(), errors);
        if (!errors.isEmpty()) {
            return errors;
        }
        SettlementAdjustment entity = settlementRepository.findByFiscalYearId(fiscalYear.getId())
                .orElseGet(() -> newSettlement(fiscalYear));
        entity.setOpeningInventory(openingInventory);
        entity.setClosingInventory(closingInventory);
        entity.setDepreciation(registerDepreciation(fiscalYear));
        entity.setAccountsReceivable(receivable);
        entity.setAccountsPayable(payable);
        entity.setUnpaidExpenses(unpaid);
        entity.setUnreceivedIncome(unreceived);
        entity.setOpeningBalance(openingBalance);
        entity.setClosingCash(closingCash);
        entity.setBalancesConfirmed(true);
        entity.setSettlementStatus(resolveSettlementStatus(receivable, payable, unpaid, unreceived));
        SettlementAdjustment savedAdjustment = settlementRepository.save(entity);
        applySettlementAllocationByAdjustment(fiscalYear.getId(), savedAdjustment);
        closingJournalService.rebuild(fiscalYear.getId());
        return List.of();
    }

    /** 固定資産台帳による当年分の減価償却費（必要経費算入額）。決算整理の参照用に保存する。 */
    private BigDecimal registerDepreciation(FiscalYear fiscalYear) {
        BigDecimal total = BigDecimal.ZERO;
        List<FixedAsset> assets = fixedAssetRepository
                .findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(fiscalYear.getBusinessProfile().getId());
        List<FixedAssetEvent> events = fixedAssetEventRepository
                .findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(fiscalYear.getBusinessProfile().getId());
        for (FixedAsset asset : assets) {
            if (!DepreciationRules.recognitionDate(asset, events).isAfter(fiscalYear.getPeriodEnd())) {
                BigDecimal ratio = ratioForYear(asset, fiscalYear.getPeriodStart());
                total = total.add(DepreciationCalculator.forYear(asset, fiscalYear.getTaxYear(), ratio, events, assets).businessPortion());
            }
        }
        return total;
    }

    @Override
    @Transactional
    public List<String> updateDeduction(Long appUserId, Integer year, String deduction,
            boolean requirementsConfirmed, String deduction65Method) {
        FiscalYear fiscalYear = loadOwnedFiscalYear(appUserId, year);
        if (CLOSED.equals(fiscalYear.getStatus())) {
            return List.of("確定済みの年度は編集できません。");
        }
        int value;
        if ("65".equals(deduction)) {
            value = 65;
        } else if ("55".equals(deduction)) {
            value = 55;
        } else if ("10".equals(deduction)) {
            value = 10;
        } else {
            return List.of("青色申告特別控除の値が不正です。");
        }
        String method = DEDUCTION_METHOD_NONE;
        if (value == 65) {
            if (DEDUCTION_METHOD_ETAX.equals(deduction65Method)
                    || DEDUCTION_METHOD_QUALIFIED_EBOOKS.equals(deduction65Method)
                    || DEDUCTION_METHOD_UNCONFIRMED.equals(deduction65Method)) {
                method = deduction65Method;
            } else {
                return List.of("65万円控除の適用方法を選択してください。");
            }
        }
        SettlementAdjustment entity = settlementRepository.findByFiscalYearId(fiscalYear.getId())
                .orElseGet(() -> newSettlement(fiscalYear));
        entity.setBlueDeduction(value);
        entity.setBlueDeductionRequirementsConfirmed(requirementsConfirmed);
        entity.setBlueDeduction65Method(method);
        settlementRepository.save(entity);
        return List.of();
    }

    private SettlementAdjustment newSettlement(FiscalYear fiscalYear) {
        SettlementAdjustment entity = new SettlementAdjustment();
        entity.setFiscalYear(fiscalYear);
        entity.setSettlementStatus(SETTLEMENT_STATUS_UNSETTLED);
        return entity;
    }

    private BigDecimal parse(String label, String raw, List<String> errors) {
        String normalized = raw == null ? "" : Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .replace(",", "").replace(" ", "").trim();
        if (normalized.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (!normalized.matches("\\d{1,10}")) {
            errors.add(label + "は0〜9,999,999,999の整数で入力してください。");
            return BigDecimal.ZERO;
        }
        BigDecimal value = new BigDecimal(normalized);
        if (value.compareTo(MAX_AMOUNT) > 0) {
            errors.add(label + "は0〜9,999,999,999の範囲で入力してください。");
            return BigDecimal.ZERO;
        }
        return value;
    }

    private BusinessProfile loadProfile(Long appUserId) {
        return businessProfileRepository.findByAppUserId(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Business profile not found"));
    }

    private FiscalYear loadOwnedFiscalYear(Long appUserId, Integer year) {
        BusinessProfile profile = loadProfile(appUserId);
        return resolve(fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId()), year);
    }

    private FiscalYear resolve(List<FiscalYear> fiscalYears, Integer year) {
        if (fiscalYears.isEmpty()) {
            throw new EntryNotFoundException("Fiscal year not found");
        }
        if (year == null) {
            return fiscalYears.get(0);
        }
        return fiscalYears.stream()
                .filter(f -> year.equals(f.getTaxYear()))
                .findFirst()
                .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
    }

    private BasicInfo basicInfo(BusinessProfile profile, int deduction) {
        AppUser user = profile.getAppUser();
        String filing = profile.getFilingType() != null && profile.getFilingType().startsWith("blue")
                ? "青色申告（" + deduction + "万円控除）"
                : "白色申告";
        return new BasicInfo(
                user.getDisplayName(), profile.getBusinessName(), profile.getIndustry(), user.getPhone(), user.getAddress(), user.getEmail(), filing);
    }

    private SettlementForm toForm(SettlementAdjustment s) {
        SettlementForm form = new SettlementForm();
        if (s != null) {
            form.setOpeningInventory(s.getOpeningInventory().toPlainString());
            form.setClosingInventory(s.getClosingInventory().toPlainString());
            form.setDepreciation(s.getDepreciation().toPlainString());
            form.setAccountsReceivable(s.getAccountsReceivable().toPlainString());
            form.setAccountsPayable(s.getAccountsPayable().toPlainString());
            form.setUnpaidExpenses(s.getUnpaidExpenses().toPlainString());
            form.setUnreceivedIncome(s.getUnreceivedIncome().toPlainString());
            form.setOpeningBalance(s.getOpeningBalance().toPlainString());
            form.setClosingCash(s.getClosingCash().toPlainString());
        }
        return form;
    }

    private Calc calculate(FiscalYear fiscalYear, Long appUserId, SettlementAdjustment settlement, AccountMapping mapping) {
        Calc calc = new Calc();
        List<IncomeEntry> incomes = incomeEntryRepository.findAllOwnedByFiscalYear(fiscalYear.getId(), appUserId)
                .stream().filter(e -> !e.isExcludeFromReturn() && e.getAmount() != null && e.getTransactionDate() != null)
                .toList();
        List<ExpenseEntry> expenses = expenseEntryRepository.findAllOwnedByFiscalYear(fiscalYear.getId(), appUserId)
                .stream().filter(e -> !e.isExcludeFromReturn() && e.getAmount() != null && e.getTransactionDate() != null)
                .toList();
        java.util.Arrays.fill(calc.incomeByMonth, BigDecimal.ZERO);
        java.util.Arrays.fill(calc.expenseByMonth, BigDecimal.ZERO);

        List<Object[]> sortable = new ArrayList<>();
        for (IncomeEntry e : incomes) {
            int idx = e.getTransactionDate().getMonthValue() - 1;
            calc.incomeByMonth[idx] = calc.incomeByMonth[idx].add(e.getAmount());
            calc.sales = calc.sales.add(e.getAmount());
            String item = e.getIncomeItem() == null ? UNCATEGORIZED : e.getIncomeItem().getItemName();
            AccountTotal account = incomeAccount(e, item, mapping, calc);
            account.amount = account.amount.add(e.getAmount());
            String debit = e.getReceiveMethod() == null ? "普通預金" : e.getReceiveMethod().getMethodName();
            sortable.add(new Object[] {e.getTransactionDate(), e.getId(), 0,
                    journalLine(e.getTransactionDate().toString(), e.getDetails(), item, debit, e.getAmount(), account.name, e.getAmount())});
        }
        List<HouseholdConsumption> householdConsumptions =
                householdConsumptionRepository.findByFiscalYearIdOrderByTransactionDateAscIdAsc(fiscalYear.getId());
        AccountRef salesRef = mapping.byCode(AccountMasterSeed.ACC_SALES).orElse(null);
        AccountTotal salesAccount = salesRef == null
                ? calc.account(SECTION_REVENUE, AccountMasterSeed.ACC_SALES, "売上高", Integer.MAX_VALUE)
                : calc.account(SECTION_REVENUE, salesRef.code(), salesRef.name(), salesRef.displayOrder());
        for (HouseholdConsumption consumption : householdConsumptions) {
            int idx = consumption.getTransactionDate().getMonthValue() - 1;
            calc.incomeByMonth[idx] = calc.incomeByMonth[idx].add(consumption.getAmount());
            calc.sales = calc.sales.add(consumption.getAmount());
            salesAccount.amount = salesAccount.amount.add(consumption.getAmount());
            sortable.add(new Object[] {consumption.getTransactionDate(), consumption.getId(), 0,
                    journalLine(consumption.getTransactionDate().toString(), consumption.getDetails(), "家事消費等",
                            OWNER_DRAW_LABEL, consumption.getAmount(), "売上高", consumption.getAmount())});
        }
        for (ExpenseEntry e : expenses) {
            BigDecimal amount = e.getDeductibleAmount() != null ? e.getDeductibleAmount() : e.getAmount();
            int idx = e.getTransactionDate().getMonthValue() - 1;
            calc.expenseByMonth[idx] = calc.expenseByMonth[idx].add(amount);
            String item = e.getExpenseItem() == null ? UNCATEGORIZED : e.getExpenseItem().getItemName();
            AccountTotal account = expenseAccount(e, item, mapping, calc);
            account.amount = account.amount.add(amount);
            String credit = e.getPaymentMethod() == null ? "現金" : e.getPaymentMethod().getMethodName();
            BigDecimal ownerDraw = e.isHomeApportionmentApplied()
                    ? e.getAmount().subtract(amount).max(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            // 家事按分は複合仕訳: 借方 経費(按分額)＋事業主貸(残額) / 貸方 支払方法(支出総額)
            BigDecimal creditAmount = amount.add(ownerDraw);
            sortable.add(new Object[] {e.getTransactionDate(), e.getId(), 1,
                    journalLine(e.getTransactionDate().toString(), e.getDetails(), item, account.name, amount, credit, creditAmount)});
            if (ownerDraw.signum() > 0) {
                calc.ownerDraw = calc.ownerDraw.add(ownerDraw);
                JournalLine head = journalLine(e.getTransactionDate().toString(), e.getDetails(), item, OWNER_DRAW_LABEL, ownerDraw, null, null);
                sortable.add(new Object[] {e.getTransactionDate(), e.getId(), 2,
                        new JournalLine(head.getDate(), head.getSummary(), OWNER_DRAW_LABEL, ownerDraw, null, null, true)});
            }
        }
        List<FixedAsset> assets = fixedAssetRepository
                .findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(fiscalYear.getBusinessProfile().getId());
        List<FixedAssetEvent> assetEvents = fixedAssetEventRepository
                .findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(fiscalYear.getBusinessProfile().getId());
        BigDecimal depreciation = BigDecimal.ZERO;
        BigDecimal[] depreciationByMonth = new BigDecimal[12];
        java.util.Arrays.fill(depreciationByMonth, BigDecimal.ZERO);
        for (FixedAsset asset : assets) {
            if (!DepreciationRules.recognitionDate(asset, assetEvents).isAfter(fiscalYear.getPeriodEnd())) {
                BigDecimal portion = DepreciationCalculator.forYear(asset, fiscalYear.getTaxYear(),
                        ratioForYear(asset, fiscalYear.getPeriodStart()), assetEvents, assets).businessPortion();
                depreciation = depreciation.add(portion);
                LocalDate disposal = DepreciationRules.disposalDate(asset, assetEvents);
                int month = disposal != null && disposal.getYear() == fiscalYear.getTaxYear()
                        && !DepreciationCalculator.LUMP_SUM_3Y.equals(asset.getDepreciationMethod())
                        ? disposal.getMonthValue() - 1 : 11;
                depreciationByMonth[month] = depreciationByMonth[month].add(portion);
            }
        }
        for (FixedAssetEvent event : assetEvents) {
            if (event.getEffectiveDate().isBefore(fiscalYear.getPeriodStart())
                    || event.getEffectiveDate().isAfter(fiscalYear.getPeriodEnd())
                    || (!FixedAssetEvent.SALE.equals(event.getEventType()) && !FixedAssetEvent.DISPOSAL.equals(event.getEventType()))) {
                continue;
            }
            FixedAsset asset = event.getFixedAsset();
            BigDecimal ratio = ratioForYear(asset, fiscalYear.getPeriodStart());
            boolean lump = DepreciationCalculator.LUMP_SUM_3Y.equals(asset.getDepreciationMethod());
            BigDecimal bookValue = lump ? BigDecimal.ZERO : DepreciationCalculator.forYear(asset, fiscalYear.getTaxYear(),
                    ratio, assetEvents, assets).closing();
            BigDecimal proceeds = FixedAssetEvent.SALE.equals(event.getEventType()) && event.getProceeds() != null
                    ? event.getProceeds() : BigDecimal.ZERO;
            BigDecimal difference = FixedAssetEvent.SALE.equals(event.getEventType())
                    ? (lump ? proceeds.multiply(ratio).divide(new BigDecimal("100"), 0, RoundingMode.DOWN) : BigDecimal.ZERO)
                    : bookValue.multiply(ratio).divide(new BigDecimal("100"), 0, RoundingMode.DOWN).negate();
            if (difference.signum() > 0) {
                AccountRef ref = mapping.byCode(AccountMasterSeed.ACC_MISC_INCOME).orElse(null);
                AccountTotal account = ref == null
                        ? calc.account(SECTION_REVENUE, AccountMasterSeed.ACC_MISC_INCOME, "雑収入", Integer.MAX_VALUE)
                        : calc.account(SECTION_REVENUE, ref.code(), ref.name(), ref.displayOrder());
                account.amount = account.amount.add(difference);
                calc.sales = calc.sales.add(difference);
                int month = event.getEffectiveDate().getMonthValue() - 1;
                calc.incomeByMonth[month] = calc.incomeByMonth[month].add(difference);
            } else if (difference.signum() < 0) {
                AccountRef ref = mapping.byCode(AccountMasterSeed.ACC_MISC_EXPENSE).orElse(null);
                AccountTotal account = ref == null
                        ? calc.account(SECTION_EXPENSE, AccountMasterSeed.ACC_MISC_EXPENSE, "雑費", Integer.MAX_VALUE)
                        : calc.account(SECTION_EXPENSE, ref.code(), ref.name(), ref.displayOrder());
                account.amount = account.amount.add(difference.abs());
                int month = event.getEffectiveDate().getMonthValue() - 1;
                calc.expenseByMonth[month] = calc.expenseByMonth[month].add(difference.abs());
            }
        }
        calc.depreciation = depreciation;
        calc.incomeEntries = incomes;
        calc.expenseEntries = expenses;
        calc.householdConsumptions = householdConsumptions;
        BigDecimal inventory = settlement == null ? BigDecimal.ZERO
                : safeAmount(settlement.getOpeningInventory()).subtract(safeAmount(settlement.getClosingInventory()));
        if (depreciation.signum() != 0) {
            AccountRef ref = mapping.byCode(AccountMasterSeed.ACC_DEPRECIATION).orElse(null);
            AccountTotal account = ref != null
                    ? calc.account(SECTION_EXPENSE, ref.code(), ref.name(), ref.displayOrder())
                    : calc.account(SECTION_EXPENSE, null, DEPRECIATION_LABEL, Integer.MAX_VALUE);
            account.amount = account.amount.add(depreciation);
        }
        if (inventory.signum() != 0) {
            AccountTotal account = calc.account(SECTION_ADJUSTMENT, null, INVENTORY_LABEL, Integer.MAX_VALUE);
            account.amount = account.amount.add(inventory);
        }
        for (int i = 0; i < 12; i++) {
            calc.expenseByMonth[i] = calc.expenseByMonth[i].add(depreciationByMonth[i]);
        }
        calc.expenseByMonth[11] = calc.expenseByMonth[11].add(inventory);
        List<AccountTotal> ordered = new ArrayList<>(calc.accounts.values());
        ordered.sort(Comparator.comparingInt((AccountTotal a) -> sectionOrder(a.section))
                .thenComparingInt(a -> a.order)
                .thenComparing(a -> a.name));
        for (AccountTotal a : ordered) {
            if (!SECTION_REVENUE.equals(a.section)) {
                calc.expenses = calc.expenses.add(a.amount);
            }
            calc.plRows.add(new PlRow(a.name, a.amount, a.section));
        }
        calc.income = calc.sales.subtract(calc.expenses);

        List<JournalLine> persisted = buildJournalFromPersistedEntries(fiscalYear.getId());
        if (!persisted.isEmpty()) {
            calc.journal.addAll(persisted);
        } else {
            sortable.sort(Comparator
                    .comparing((Object[] o) -> (java.time.LocalDate) o[0])
                    .thenComparing(o -> (Long) o[1], Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(o -> (Integer) o[2]));
            sortable.forEach(o -> calc.journal.add((JournalLine) o[3]));
        }
        return calc;
    }

    private AccountTotal incomeAccount(IncomeEntry e, String item, AccountMapping mapping, Calc calc) {
        Optional<AccountRef> ref = mapping.forIncomeItem(e.getIncomeItem(), e.getTransactionDate());
        if (ref.isPresent()) {
            noteReview(ref.get(), item, calc);
            if (!ref.get().isProfitAndLoss()) {
                calc.nonPlItems.add(item);
            }
            return calc.account(SECTION_REVENUE, ref.get().code(), ref.get().name(), ref.get().displayOrder());
        }
        calc.unmappedItems.add(item);
        String categoryCode = e.getIncomeItem() == null || e.getIncomeItem().getIncomeCategory() == null
                ? "" : e.getIncomeItem().getIncomeCategory().getCategoryCode();
        String fallbackCode = "misc".equals(categoryCode) ? AccountMasterSeed.ACC_MISC_INCOME : AccountMasterSeed.ACC_SALES;
        return mapping.byCode(fallbackCode)
                .map(r -> calc.account(SECTION_REVENUE, r.code(), r.name(), r.displayOrder()))
                .orElseGet(() -> calc.account(SECTION_REVENUE, null, "売上高", Integer.MAX_VALUE));
    }

    private AccountTotal expenseAccount(ExpenseEntry e, String item, AccountMapping mapping, Calc calc) {
        Optional<AccountRef> ref = mapping.forExpenseItem(e.getExpenseItem(), e.getTransactionDate());
        if (ref.isPresent()) {
            noteReview(ref.get(), item, calc);
            if (!ref.get().isProfitAndLoss()) {
                calc.nonPlItems.add(item);
            }
            return calc.account(SECTION_EXPENSE, ref.get().code(), ref.get().name(), ref.get().displayOrder());
        }
        calc.unmappedItems.add(item);
        return calc.account(SECTION_EXPENSE, null, item, Integer.MAX_VALUE);
    }

    private static void noteReview(AccountRef ref, String item, Calc calc) {
        if (ref.requiresReview()) {
            calc.reviewItems.putIfAbsent(item, ref.reviewNote() == null ? "" : ref.reviewNote());
        }
    }

    private BigDecimal ratioForYear(FixedAsset asset, java.time.LocalDate periodStart) {
        return fixedAssetEventRepository.findByFixedAssetIdOrderByEffectiveDateAscIdAsc(asset.getId()).stream()
                .filter(e -> FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(e.getEventType()))
                .filter(e -> !e.getEffectiveDate().isAfter(periodStart))
                .max(Comparator.comparing(FixedAssetEvent::getEffectiveDate))
                .map(FixedAssetEvent::getBusinessRatio)
                .orElse(asset.getBusinessRatio());
    }

    private static int sectionOrder(String section) {
        if (SECTION_REVENUE.equals(section)) {
            return 0;
        }
        return SECTION_EXPENSE.equals(section) ? 1 : 2;
    }

    /**
     * 勘定科目別の集計を青色申告決算書（一般用）損益計算書の欄へ集約する。
     * 任意記載欄に対応付けた科目は表示順に空欄へ割り当て、欄が不足する分は雑費へ含める。
     */
    private Statement buildStatement(StatementLayout layout, Calc calc, SettlementAdjustment settlement,
            BigDecimal deductionAmount) {
        Statement st = new Statement();
        if (layout.isEmpty()) {
            return st;
        }
        Set<String> lineCodes = new HashSet<>();
        layout.lines().forEach(l -> lineCodes.add(l.getLineCode()));
        Map<String, BigDecimal> amounts = new HashMap<>();
        Map<String, List<String>> sources = new HashMap<>();
        List<AccountTotal> freeAccounts = new ArrayList<>();
        for (AccountTotal a : calc.accounts.values()) {
            if (SECTION_ADJUSTMENT.equals(a.section) || a.amount.signum() == 0) {
                continue;
            }
            String line = a.code == null ? null : layout.lineByAccountCode().get(a.code);
            if (AccountMasterSeed.LINE_FREE.equals(line)) {
                freeAccounts.add(a);
                continue;
            }
            if (line == null || !lineCodes.contains(line)) {
                line = SECTION_REVENUE.equals(a.section) ? AccountMasterSeed.LINE_SALES : AccountMasterSeed.LINE_MISC_EXPENSE;
                if (a.code != null) {
                    st.unassignedAccounts.add(a.name);
                }
            }
            addStatementAmount(amounts, sources, line, a.amount, a.name);
        }
        freeAccounts.sort(Comparator.comparingInt((AccountTotal a) -> a.order).thenComparing(a -> a.name));
        List<StatementLine> freeLines = layout.lines().stream()
                .filter(l -> AccountMasterSeed.LINE_TYPE_FREE.equals(l.getLineType()))
                .toList();
        Map<String, String> freeLabels = new HashMap<>();
        for (int i = 0; i < freeAccounts.size(); i++) {
            AccountTotal a = freeAccounts.get(i);
            if (i < freeLines.size()) {
                String line = freeLines.get(i).getLineCode();
                freeLabels.put(line, a.name);
                addStatementAmount(amounts, sources, line, a.amount, a.name);
            } else {
                addStatementAmount(amounts, sources, AccountMasterSeed.LINE_MISC_EXPENSE, a.amount, a.name);
                st.overflowAccounts.add(a.name);
            }
        }
        if (settlement != null) {
            addStatementAmount(amounts, sources, AccountMasterSeed.LINE_OPENING_INVENTORY,
                    safeAmount(settlement.getOpeningInventory()), SETTLEMENT_SOURCE_LABEL);
            addStatementAmount(amounts, sources, AccountMasterSeed.LINE_CLOSING_INVENTORY,
                    safeAmount(settlement.getClosingInventory()), SETTLEMENT_SOURCE_LABEL);
        }

        List<StatementLine> lines = layout.lines();
        amounts.put(AccountMasterSeed.LINE_COGS_SUBTOTAL,
                value(amounts, AccountMasterSeed.LINE_OPENING_INVENTORY).add(value(amounts, AccountMasterSeed.LINE_PURCHASES)));
        amounts.put(AccountMasterSeed.LINE_COGS,
                value(amounts, AccountMasterSeed.LINE_COGS_SUBTOTAL).subtract(value(amounts, AccountMasterSeed.LINE_CLOSING_INVENTORY)));
        amounts.put(AccountMasterSeed.LINE_GROSS_PROFIT,
                value(amounts, AccountMasterSeed.LINE_SALES).subtract(value(amounts, AccountMasterSeed.LINE_COGS)));
        amounts.put(AccountMasterSeed.LINE_EXPENSE_TOTAL,
                sumBetween(lines, amounts, AccountMasterSeed.LINE_GROSS_PROFIT, AccountMasterSeed.LINE_EXPENSE_TOTAL));
        amounts.put(AccountMasterSeed.LINE_NET_BEFORE_RESERVES,
                value(amounts, AccountMasterSeed.LINE_GROSS_PROFIT).subtract(value(amounts, AccountMasterSeed.LINE_EXPENSE_TOTAL)));
        amounts.put(AccountMasterSeed.LINE_REVERSAL_TOTAL,
                sumBetween(lines, amounts, AccountMasterSeed.LINE_NET_BEFORE_RESERVES, AccountMasterSeed.LINE_REVERSAL_TOTAL));
        amounts.put(AccountMasterSeed.LINE_PROVISION_TOTAL,
                sumBetween(lines, amounts, AccountMasterSeed.LINE_REVERSAL_TOTAL, AccountMasterSeed.LINE_PROVISION_TOTAL));
        BigDecimal beforeBlue = value(amounts, AccountMasterSeed.LINE_NET_BEFORE_RESERVES)
                .add(value(amounts, AccountMasterSeed.LINE_REVERSAL_TOTAL))
                .subtract(value(amounts, AccountMasterSeed.LINE_PROVISION_TOTAL));
        BigDecimal blue = deductionAmount.min(beforeBlue.max(BigDecimal.ZERO));
        amounts.put(AccountMasterSeed.LINE_INCOME_BEFORE_BLUE, beforeBlue);
        amounts.put(AccountMasterSeed.LINE_BLUE_DEDUCTION, blue);
        amounts.put(AccountMasterSeed.LINE_INCOME, beforeBlue.subtract(blue));
        st.incomeBeforeBlue = beforeBlue;

        for (StatementLine line : lines) {
            String code = line.getLineCode();
            String label = freeLabels.getOrDefault(code, line.getLineLabel());
            String accounts = String.join("、", sources.getOrDefault(code, List.of()));
            st.rows.add(new StatementRow(line.getLineNumber(), circledNumber(line.getLineNumber()), code, label,
                    value(amounts, code), line.getLineType(), accounts, TOTAL_LINES.contains(code)));
        }
        return st;
    }

    private static void addStatementAmount(Map<String, BigDecimal> amounts, Map<String, List<String>> sources,
            String line, BigDecimal amount, String source) {
        amounts.merge(line, amount, BigDecimal::add);
        if (amount.signum() != 0) {
            List<String> list = sources.computeIfAbsent(line, k -> new ArrayList<>());
            if (!list.contains(source)) {
                list.add(source);
            }
        }
    }

    private static BigDecimal value(Map<String, BigDecimal> amounts, String line) {
        return amounts.getOrDefault(line, BigDecimal.ZERO);
    }

    /** from欄とto欄の間（両端を除く）にある計算欄以外の欄を合計する。 */
    private static BigDecimal sumBetween(List<StatementLine> lines, Map<String, BigDecimal> amounts, String from, String to) {
        BigDecimal total = BigDecimal.ZERO;
        boolean inside = false;
        for (StatementLine line : lines) {
            if (line.getLineCode().equals(to)) {
                break;
            }
            if (inside && !AccountMasterSeed.LINE_TYPE_CALC.equals(line.getLineType())) {
                total = total.add(value(amounts, line.getLineCode()));
            }
            if (line.getLineCode().equals(from)) {
                inside = true;
            }
        }
        return total;
    }

    static String circledNumber(int n) {
        if (n >= 1 && n <= 20) {
            return String.valueOf((char) (0x2460 + n - 1));
        }
        if (n >= 21 && n <= 35) {
            return String.valueOf((char) (0x3251 + n - 21));
        }
        if (n >= 36 && n <= 50) {
            return String.valueOf((char) (0x32B1 + n - 36));
        }
        return "(" + n + ")";
    }

    private void buildMappingChecks(Calc calc, Statement statement, List<String> errors, List<String> warnings,
            List<CheckItem> checks) {
        String reason = null;
        if (statement.rows.isEmpty()) {
            warnings.add("対象年度の青色申告決算書の様式欄が登録されていません。");
            reason = "様式欄が未登録です";
        } else if (statement.incomeBeforeBlue.compareTo(calc.income) != 0) {
            errors.add("決算書の青色申告特別控除前の所得金額と損益計算書の所得が一致していません。");
            reason = "決算書と損益計算書の所得が一致しません";
        }

        if (!calc.unmappedItems.isEmpty()) {
            warnings.add("勘定科目が未設定の品目があります（" + summarize(calc.unmappedItems) + "）。売上高または雑費として集計しています。");
            reason = reason == null ? "勘定科目が未設定の品目があります" : reason;
        }
        if (!calc.nonPlItems.isEmpty()) {
            warnings.add("事業損益に含めない科目の取引が申告対象になっています（" + summarize(calc.nonPlItems) + "）。申告対象外の設定を確認してください。");
            reason = reason == null ? "損益に含めない科目の取引があります" : reason;
        }
        if (!statement.unassignedAccounts.isEmpty()) {
            warnings.add("決算書の欄が未設定の科目があります（" + summarize(statement.unassignedAccounts) + "）。売上金額または雑費に含めています。");
            reason = reason == null ? "決算書の欄が未設定の科目があります" : reason;
        }
        if (!statement.overflowAccounts.isEmpty()) {
            warnings.add("任意記載欄を超える経費科目（" + summarize(statement.overflowAccounts) + "）を雑費に含めています。");
            reason = reason == null ? "任意記載欄を超える科目があります" : reason;
        }
        if (!calc.reviewItems.isEmpty()) {
            List<String> notes = new ArrayList<>();
            calc.reviewItems.forEach((item, note) -> notes.add(note.isEmpty() ? item : item + "：" + note));
            warnings.add("勘定科目の確認が必要な取引があります（" + summarize(notes) + "）。");
        }
        String message = reason != null ? reason
                : calc.reviewItems.isEmpty() ? "入力完了" : "要確認の品目が" + calc.reviewItems.size() + "件あります";
        checks.add(new CheckItem("check-account-mapping", "科目対応", reason == null, message));
    }

    private void buildTransactionJournalChecks(Long fiscalYearId, List<String> errors, List<CheckItem> checks) {
        List<AccountingTransaction> transactions =
                transactionRepository.findByFiscalYearIdOrderByTransactionDateAscIdAsc(fiscalYearId);
        Map<Long, List<JournalEntry>> entriesByTransaction = new HashMap<>();
        for (JournalEntry entry : journalEntryRepository.findAllWithLinesByFiscalYearId(fiscalYearId)) {
            if (entry.getAccountingTransaction() != null) {
                entriesByTransaction.computeIfAbsent(entry.getAccountingTransaction().getId(), k -> new ArrayList<>()).add(entry);
            }
        }
        List<String> problems = new ArrayList<>();
        for (AccountingTransaction transaction : transactions) {
            List<JournalEntry> entries = entriesByTransaction.getOrDefault(transaction.getId(), List.of());
            if (entries.size() != 1) {
                problems.add("取引#" + transaction.getId() + "の仕訳件数=" + entries.size());
                continue;
            }
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            for (org.example.entity.JournalLine line : entries.get(0).getLines()) {
                if ("D".equalsIgnoreCase(line.getDebitCredit())) {
                    debit = debit.add(safeAmount(line.getAmount()));
                } else if ("C".equalsIgnoreCase(line.getDebitCredit())) {
                    credit = credit.add(safeAmount(line.getAmount()));
                }
            }
            if (debit.compareTo(credit) != 0 || debit.compareTo(safeAmount(transaction.getTotalAmount())) != 0) {
                problems.add("取引#" + transaction.getId() + "の取引金額と仕訳借貸が一致しません");
            }
        }
        if (!problems.isEmpty()) {
            errors.add("取引データと仕訳の件数・金額が一致しません（" + summarize(problems) + "）。");
        }
        checks.add(new CheckItem("check-transaction-journal-reconciliation", "取引と仕訳の照合",
                problems.isEmpty(), problems.isEmpty()
                        ? "取引" + transactions.size() + "件と仕訳が1対1・金額一致"
                        : "取引データと仕訳の件数または金額に差異があります"));
    }

    private static String summarize(java.util.Collection<String> values) {
        List<String> list = new ArrayList<>(values);
        if (list.size() <= 3) {
            return String.join("、", list);
        }
        return String.join("、", list.subList(0, 3)) + " ほか" + (list.size() - 3) + "件";
    }

    private JournalLine journalLine(String date, String details, String fallback,
            String debit, BigDecimal debitAmount, String credit, BigDecimal creditAmount) {
        String summary = details == null || details.isBlank() ? fallback : details.trim();
        if (summary.length() > 60) {
            summary = summary.substring(0, 60) + "…";
        }
        return new JournalLine(date.replace('-', '/'), summary, debit, debitAmount, credit, creditAmount);
    }

    private String resolveSettlementStatus(
            BigDecimal accountsReceivable,
            BigDecimal accountsPayable,
            BigDecimal unpaidExpenses,
            BigDecimal unreceivedIncome) {
        BigDecimal pending = safeAmount(accountsReceivable)
                .add(safeAmount(accountsPayable))
                .add(safeAmount(unpaidExpenses))
                .add(safeAmount(unreceivedIncome));
        return pending.signum() == 0 ? SETTLEMENT_STATUS_SETTLED : SETTLEMENT_STATUS_UNSETTLED;
    }

    private void applySettlementAllocationByAdjustment(Long fiscalYearId, SettlementAdjustment adjustment) {
        if (adjustment == null || adjustment.getId() == null) {
            return;
        }
        transactionSettlementRepository.deleteBySettlementAdjustmentId(adjustment.getId());

        List<org.example.entity.AccountingTransaction> candidates = transactionRepository
                .findByFiscalYearIdAndSettlementStatusInOrderByTransactionDateAscIdAsc(
                        fiscalYearId,
                        List.of(SETTLEMENT_STATUS_UNSETTLED, SETTLEMENT_STATUS_PARTIALLY_SETTLED));

        BigDecimal totalOutstanding = BigDecimal.ZERO;
        Map<Long, BigDecimal> outstandingById = new LinkedHashMap<>();
        for (org.example.entity.AccountingTransaction transaction : candidates) {
            BigDecimal total = safeAmount(transaction.getTotalAmount());
            BigDecimal settled = safeAmount(transactionSettlementRepository.sumSettledAmountBySourceTransactionId(transaction.getId()));
            BigDecimal outstanding = total.subtract(settled);
            if (outstanding.signum() < 0) {
                outstanding = BigDecimal.ZERO;
            }
            outstandingById.put(transaction.getId(), outstanding);
            totalOutstanding = totalOutstanding.add(outstanding);
        }

        BigDecimal pendingAmount = safeAmount(adjustment.getAccountsReceivable())
                .add(safeAmount(adjustment.getAccountsPayable()))
                .add(safeAmount(adjustment.getUnpaidExpenses()))
                .add(safeAmount(adjustment.getUnreceivedIncome()));
        BigDecimal settleBudget = totalOutstanding.subtract(pendingAmount);
        if (settleBudget.signum() < 0) {
            settleBudget = BigDecimal.ZERO;
        }

        for (org.example.entity.AccountingTransaction transaction : candidates) {
            if (settleBudget.signum() <= 0) {
                break;
            }
            BigDecimal outstanding = outstandingById.getOrDefault(transaction.getId(), BigDecimal.ZERO);
            if (outstanding.signum() <= 0) {
                continue;
            }
            BigDecimal applied = outstanding.min(settleBudget);
            if (applied.signum() <= 0) {
                continue;
            }
            TransactionSettlement settlement = new TransactionSettlement();
            settlement.setSourceTransaction(transaction);
            settlement.setSettlementAdjustment(adjustment);
            settlement.setSettledAmount(applied);
            transactionSettlementRepository.save(settlement);
            settleBudget = settleBudget.subtract(applied);
        }

        refreshTransactionSettlementStatus(fiscalYearId);
    }

    private void refreshTransactionSettlementStatus(Long fiscalYearId) {
        List<org.example.entity.AccountingTransaction> transactions = transactionRepository
                .findByFiscalYearIdOrderByTransactionDateAscIdAsc(fiscalYearId);
        for (org.example.entity.AccountingTransaction transaction : transactions) {
            BigDecimal total = safeAmount(transaction.getTotalAmount());
            BigDecimal settled = safeAmount(transactionSettlementRepository.sumSettledAmountBySourceTransactionId(transaction.getId()));
            if (settled.signum() <= 0) {
                if (transaction.getSettlementStatus() == null || transaction.getSettlementStatus().isBlank()) {
                    transaction.setSettlementStatus(SETTLEMENT_STATUS_UNSETTLED);
                }
                transaction.setSettlementReferenceId(null);
                transactionRepository.save(transaction);
                continue;
            }
            if (settled.compareTo(total) >= 0) {
                transaction.setSettlementStatus(SETTLEMENT_STATUS_SETTLED);
            } else {
                transaction.setSettlementStatus(SETTLEMENT_STATUS_PARTIALLY_SETTLED);
            }
            transaction.setSettlementReferenceId(
                    transactionSettlementRepository.findLatestSettlementAdjustmentIdBySourceTransactionId(transaction.getId()));
            transactionRepository.save(transaction);
        }
    }

    private List<JournalLine> buildJournalFromPersistedEntries(Long fiscalYearId) {
        List<JournalEntry> entries = new ArrayList<>(new LinkedHashSet<>(journalEntryRepository.findAllWithLinesByFiscalYearId(fiscalYearId)));
        List<JournalLine> converted = new ArrayList<>();
        for (JournalEntry entry : entries) {
            List<org.example.entity.JournalLine> debitLines = entry.getLines().stream()
                    .filter(line -> "D".equalsIgnoreCase(line.getDebitCredit()))
                    .sorted(Comparator.comparing(org.example.entity.JournalLine::getLineNo))
                    .toList();
            List<org.example.entity.JournalLine> creditLines = entry.getLines().stream()
                    .filter(line -> "C".equalsIgnoreCase(line.getDebitCredit()))
                    .sorted(Comparator.comparing(org.example.entity.JournalLine::getLineNo))
                    .toList();
            if (debitLines.isEmpty() || creditLines.isEmpty()) {
                continue;
            }
            String creditAccount = creditLines.size() == 1
                    ? creditLines.get(0).getAccountName()
                    : COMPOUND_COUNTER_LABEL;
            BigDecimal creditAmount = creditLines.stream()
                    .map(org.example.entity.JournalLine::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String date = entry.getEntryDate() == null ? "" : entry.getEntryDate().toString().replace('-', '/');
            String summary = entry.getSummary() == null ? "" : entry.getSummary();
            for (int i = 0; i < debitLines.size(); i++) {
                org.example.entity.JournalLine debit = debitLines.get(i);
                if (i == 0) {
                    converted.add(new JournalLine(
                            date,
                            summary,
                            debit.getAccountName(),
                            safeAmount(debit.getAmount()),
                            creditAccount,
                            safeAmount(creditAmount),
                            false));
                } else {
                    converted.add(new JournalLine(
                            date,
                            summary,
                            debit.getAccountName(),
                            safeAmount(debit.getAmount()),
                            null,
                            null,
                            true));
                }
            }
        }
        return converted;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private static List<List<JournalLine>> journalEntries(List<JournalLine> journal) {
        List<List<JournalLine>> entries = new ArrayList<>();
        for (JournalLine line : journal) {
            if (line.isContinuation() && !entries.isEmpty()) {
                entries.get(entries.size() - 1).add(line);
            } else {
                List<JournalLine> entry = new ArrayList<>();
                entry.add(line);
                entries.add(entry);
            }
        }
        return entries;
    }

    private static List<JournalLine> journalPreview(List<JournalLine> journal) {
        return journalEntries(journal).stream().limit(JOURNAL_PREVIEW_SIZE)
                .flatMap(List::stream).toList();
    }

    private List<LedgerAccount> ledger(List<JournalLine> journal) {
        Map<String, List<LedgerLine>> accounts = new TreeMap<>();
        for (List<JournalLine> entry : journalEntries(journal)) {
            JournalLine head = entry.get(0);
            String creditAccount = head.getCreditAccount();
            String creditCounter = entry.size() > 1 ? COMPOUND_COUNTER_LABEL : head.getDebitAccount();
            for (JournalLine line : entry) {
                accounts.computeIfAbsent(line.getDebitAccount(), k -> new ArrayList<>())
                        .add(new LedgerLine(head.getDate(), head.getSummary(), creditAccount,
                                line.getDebitAmount(), null));
            }
            accounts.computeIfAbsent(creditAccount, k -> new ArrayList<>())
                    .add(new LedgerLine(head.getDate(), head.getSummary(), creditCounter,
                            null, head.getCreditAmount()));
        }
        List<LedgerAccount> result = new ArrayList<>();
        accounts.forEach((name, lines) -> result.add(new LedgerAccount(name, lines)));
        return result;
    }

    private BalanceSheet balanceSheet(BalanceSheetService.Result r) {
        Set<String> alwaysShown = Set.of(AccountMasterSeed.ACC_CASH, AccountMasterSeed.ACC_BANK_DEPOSIT,
                AccountMasterSeed.ACC_OWNER_DRAW, AccountMasterSeed.ACC_OWNER_CONTRIBUTION);
        BalanceSheet bs = new BalanceSheet();
        for (BalanceSheetService.Balance b : r.assetSide()) {
            if ((b.getCode() != null && alwaysShown.contains(b.getCode())) || b.getOpening().signum() != 0 || b.getClosing().signum() != 0) {
                bs.assets.add(new BsRow(b.getName(), b.getOpening(), b.getClosing()));
            }
        }
        for (BalanceSheetService.Balance b : r.liabilitySide()) {
            if ((b.getCode() != null && alwaysShown.contains(b.getCode())) || b.getOpening().signum() != 0 || b.getClosing().signum() != 0) {
                bs.liabilities.add(new BsRow(b.getName(), b.getOpening(), b.getClosing()));
            }
        }
        bs.liabilities.add(new BsRow("元入金", r.getCapital(), r.getCapital()));
        bs.liabilities.add(new BsRow("青色申告特別控除前の所得金額", BigDecimal.ZERO, r.getProfitBeforeBlue()));
        bs.assetsOpening = r.getAssetsOpening();
        bs.assetsClosing = r.getAssetsClosing();
        bs.liabilitiesOpening = r.getLiabilitiesOpening();
        bs.liabilitiesClosing = r.getLiabilitiesClosing();
        bs.source = r;
        return bs;
    }

    private boolean journalBalanced(List<JournalLine> journal) {
        return journalEntries(journal).stream()
                .allMatch(entry -> entry.stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .compareTo(entry.stream().map(JournalLine::getCreditAmount).filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)) == 0);
    }

    private FilingRequirements.Result applyFilingRequirements(BusinessProfile profile, SettlementAdjustment saved,
            int deduction, Calc calc, BalanceSheet bs, long draftCount, TaxReturnExtras extras,
            BlueReturnEdition edition, List<String> errors, List<String> warnings, List<CheckItem> checks) {
        FilingRequirements.Result result = FilingRequirements.evaluate(new FilingRequirements.Input(
                profile.getFilingType(), deduction,
                saved == null ? DEDUCTION_METHOD_UNCONFIRMED : saved.getBlueDeduction65Method(),
                saved != null && saved.isBlueDeductionRequirementsConfirmed(),
                !calc.journal.isEmpty(), journalBalanced(calc.journal), draftCount, !calc.plRows.isEmpty(),
                bs.assetsOpening.compareTo(bs.liabilitiesOpening) == 0
                        && bs.assetsClosing.compareTo(bs.liabilitiesClosing) == 0,
                saved != null && saved.isBalancesConfirmed(), !extras.getAssets().isEmpty(),
                edition.getFilingDeadline(), LocalDate.now(ZoneId.of("Asia/Tokyo"))));
        errors.addAll(result.getErrors());
        warnings.addAll(result.getWarnings());
        checks.add(new CheckItem("check-filing-requirements", "申告方式・控除区分別の必要書類",
                result.isSatisfied(),
                result.isSatisfied()
                        ? "不足なし（利用者確認 " + result.getUserCheckCount() + "件）"
                        : "不足 " + result.getMissingCount() + "件・利用者確認 " + result.getUserCheckCount() + "件"));
        return result;
    }

    private void buildChecks(BasicInfo basic, Calc calc, SettlementAdjustment saved, BalanceSheet bs, long draftCount,
            long unsettledCount,
            int deduction, List<ReportMonthRow> months, List<String> errors, List<String> warnings,
            List<CheckItem> checks) {
        if (basic.getName() == null || basic.getName().isBlank()) {
            errors.add("氏名が未設定です。");
        }
        BigDecimal monthIncome = months.stream().map(ReportMonthRow::getIncome).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthExpense = months.stream().map(ReportMonthRow::getExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean monthlyOk = monthIncome.compareTo(calc.sales) == 0 && monthExpense.compareTo(calc.expenses) == 0;
        if (!monthlyOk) {
            errors.add("月別合計と年間合計が一致していません。");
        }
        boolean balanced = bs.assetsOpening.compareTo(bs.liabilitiesOpening) == 0
                && bs.assetsClosing.compareTo(bs.liabilitiesClosing) == 0;
        if (!balanced) {
            errors.add("貸借が一致していません（期末差額 "
                    + new DecimalFormat("#,##0").format(bs.assetsClosing.subtract(bs.liabilitiesClosing)) + "円）。決算整理の内容をご確認ください。");
        }
        boolean journalBalanced = journalBalanced(calc.journal);
        if (!journalBalanced) {
            errors.add("借方と貸方が一致していない仕訳があります。");
        }
        if (saved == null) {
            warnings.add("決算整理が未保存です。");
        }
        if (draftCount > 0) {
            warnings.add("未確認の仕訳が" + draftCount + "件あります。");
        }
        if (unsettledCount > 0) {
            warnings.add("未消込の取引が" + unsettledCount + "件あります。");
        }
        boolean deductionRequirementsConfirmed = saved != null && saved.isBlueDeductionRequirementsConfirmed();
        checks.add(new CheckItem("check-blue-deduction-confirmation", "控除要件の利用者確認",
                deductionRequirementsConfirmed,
                deductionRequirementsConfirmed ? "利用者が確認済み（アプリは適用可否を判定しません）"
                        : "選択した控除の要件を確認してください"));

        boolean hasData = !calc.plRows.isEmpty();
        checks.add(new CheckItem("check-profit-loss", "損益計算書", hasData && saved != null,
                !hasData ? "損益データがありません" : saved == null ? "決算整理が未保存です" : "入力完了"));
        checks.add(new CheckItem("check-monthly-sales", "月別売上", monthlyOk,
                monthlyOk ? "入力完了" : "月別合計が年間合計と一致していません"));
        checks.add(new CheckItem("check-balance-sheet", "貸借対照表", balanced,
                balanced ? "入力完了" : "貸借が一致していません"));
        checks.add(new CheckItem("check-journal", "仕訳", draftCount == 0,
                draftCount == 0 ? "入力完了" : "未確認の仕訳が" + draftCount + "件あります"));
        checks.add(new CheckItem("check-settlement", "消込", unsettledCount == 0,
                unsettledCount == 0 ? "入力完了" : "未消込の取引が" + unsettledCount + "件あります"));
    }

    private Diff diff(BigDecimal current, BigDecimal prior, boolean upIsGood) {
        if (prior == null) {
            return new Diff(false, "-", "-", true, true);
        }
        BigDecimal delta = current.subtract(prior);
        boolean up = delta.signum() >= 0;
        DecimalFormat amountFormat = new DecimalFormat("+#,##0;-#,##0");
        String amountText = amountFormat.format(delta);
        String rateText = "-";
        if (prior.signum() != 0) {
            BigDecimal rate = delta.multiply(BigDecimal.valueOf(100)).divide(prior.abs(), 1, RoundingMode.HALF_UP);
            rateText = new DecimalFormat("+0.0;-0.0").format(rate) + "%";
        }
        return new Diff(true, amountText, rateText, up, up == upIsGood);
    }

    private String yearLabel(int year) {
        return year >= 2019 ? year + "年（令和" + (year - 2018) + "年）" : year + "年";
    }

    private static class Calc {
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal ownerDraw = BigDecimal.ZERO;
        final BigDecimal[] incomeByMonth = new BigDecimal[12];
        final BigDecimal[] expenseByMonth = new BigDecimal[12];
        final List<PlRow> plRows = new ArrayList<>();
        final List<JournalLine> journal = new ArrayList<>();
        final Map<String, AccountTotal> accounts = new LinkedHashMap<>();
        final Map<String, String> reviewItems = new LinkedHashMap<>();
        final Set<String> unmappedItems = new LinkedHashSet<>();
        final Set<String> nonPlItems = new LinkedHashSet<>();
        BigDecimal depreciation = BigDecimal.ZERO;
        List<IncomeEntry> incomeEntries = List.of();
        List<ExpenseEntry> expenseEntries = List.of();
        List<HouseholdConsumption> householdConsumptions = List.of();

        AccountTotal account(String section, String code, String name, int order) {
            String key = section + ":" + (code != null ? code : "#" + name);
            return accounts.computeIfAbsent(key, k -> new AccountTotal(section, code, name, order));
        }
    }

    private static class AccountTotal {
        final String section;
        final String code;
        final String name;
        final int order;
        BigDecimal amount = BigDecimal.ZERO;

        AccountTotal(String section, String code, String name, int order) {
            this.section = section;
            this.code = code;
            this.name = name;
            this.order = order;
        }
    }

    private static class Statement {
        final List<StatementRow> rows = new ArrayList<>();
        final Set<String> unassignedAccounts = new LinkedHashSet<>();
        final List<String> overflowAccounts = new ArrayList<>();
        BigDecimal incomeBeforeBlue = BigDecimal.ZERO;
    }

    private static class BalanceSheet {
        final List<BsRow> assets = new ArrayList<>();
        final List<BsRow> liabilities = new ArrayList<>();
        BigDecimal assetsOpening = BigDecimal.ZERO;
        BigDecimal assetsClosing = BigDecimal.ZERO;
        BigDecimal liabilitiesOpening = BigDecimal.ZERO;
        BigDecimal liabilitiesClosing = BigDecimal.ZERO;
        BalanceSheetService.Result source;
    }
}
