package org.example.features.taxreturn.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseEntry;
import org.example.entity.FiscalYear;
import org.example.entity.IncomeEntry;
import org.example.entity.JournalEntry;
import org.example.entity.SettlementAdjustment;
import org.example.entity.TransactionSettlement;
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
import org.example.features.taxreturn.form.SettlementForm;
import org.example.features.taxreturn.service.TaxReturnService;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
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

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final AccountingTransactionRepository transactionRepository;
    private final SettlementAdjustmentRepository settlementRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TransactionSettlementRepository transactionSettlementRepository;

    public TaxReturnServiceImpl(
            BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository,
            IncomeEntryRepository incomeEntryRepository,
            ExpenseEntryRepository expenseEntryRepository,
            AccountingTransactionRepository transactionRepository,
            SettlementAdjustmentRepository settlementRepository,
            JournalEntryRepository journalEntryRepository,
            TransactionSettlementRepository transactionSettlementRepository) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
        this.transactionRepository = transactionRepository;
        this.settlementRepository = settlementRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.transactionSettlementRepository = transactionSettlementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxReturnPageData load(Long appUserId, Integer year, SettlementForm overrideForm) {
        BusinessProfile profile = loadProfile(appUserId);
        List<FiscalYear> fiscalYears = fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId());
        FiscalYear selected = resolve(fiscalYears, year);
        SettlementAdjustment saved = settlementRepository.findByFiscalYearId(selected.getId()).orElse(null);
        Calc current = calculate(selected, appUserId, saved);

        FiscalYear prior = fiscalYears.stream()
                .filter(f -> f.getTaxYear() == selected.getTaxYear() - 1)
                .findFirst().orElse(null);
        Calc priorCalc = prior == null ? null
                : calculate(prior, appUserId, settlementRepository.findByFiscalYearId(prior.getId()).orElse(null));

        int deduction = saved == null ? 65 : saved.getBlueDeduction();
        BigDecimal deductionAmount = BigDecimal.valueOf(deduction * 10000L);
        BigDecimal afterDeduction = current.income.subtract(deductionAmount).max(BigDecimal.ZERO);

        BalanceSheet bs = balanceSheet(saved, current.income, current.ownerDraw);
        List<ReportYearOption> options = fiscalYears.stream()
                .map(f -> new ReportYearOption(f.getTaxYear(), yearLabel(f.getTaxYear())))
                .toList();
        List<ReportMonthRow> months = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            months.add(new ReportMonthRow(i + 1, current.incomeByMonth[i], current.expenseByMonth[i], 0, 0));
        }

        long draftCount = transactionRepository.countByFiscalYearIdAndStatus(selected.getId(), DRAFT);
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

        SettlementForm form = overrideForm != null ? overrideForm : toForm(saved);
        return new TaxReturnPageData(
                options, selected.getTaxYear(), yearLabel(selected.getTaxYear()), CLOSED.equals(selected.getStatus()),
                basic, deduction,
                current.sales, current.expenses, current.income, afterDeduction,
                diff(current.sales, priorCalc == null ? null : priorCalc.sales, true),
                diff(current.expenses, priorCalc == null ? null : priorCalc.expenses, false),
                diff(current.income, priorCalc == null ? null : priorCalc.income, true),
                months, current.plRows, form,
                bs.assets, bs.liabilities, bs.assetsOpening, bs.assetsClosing, bs.liabilitiesOpening, bs.liabilitiesClosing,
                journalPreview(current.journal), current.journal, ledger(current.journal),
                checks, errors, warnings);
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
        BigDecimal depreciation = parse("減価償却費", form.getDepreciation(), errors);
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
        entity.setDepreciation(depreciation);
        entity.setAccountsReceivable(receivable);
        entity.setAccountsPayable(payable);
        entity.setUnpaidExpenses(unpaid);
        entity.setUnreceivedIncome(unreceived);
        entity.setOpeningBalance(openingBalance);
        entity.setClosingCash(closingCash);
        entity.setSettlementStatus(resolveSettlementStatus(receivable, payable, unpaid, unreceived));
        SettlementAdjustment savedAdjustment = settlementRepository.save(entity);
        applySettlementAllocationByAdjustment(fiscalYear.getId(), savedAdjustment);
        upsertSettlementJournalEntry(fiscalYear, savedAdjustment);
        return List.of();
    }

    @Override
    @Transactional
    public List<String> updateDeduction(Long appUserId, Integer year, String deduction) {
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
        SettlementAdjustment entity = settlementRepository.findByFiscalYearId(fiscalYear.getId())
                .orElseGet(() -> newSettlement(fiscalYear));
        entity.setBlueDeduction(value);
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

    private Calc calculate(FiscalYear fiscalYear, Long appUserId, SettlementAdjustment settlement) {
        Calc calc = new Calc();
        List<IncomeEntry> incomes = incomeEntryRepository.findAllOwnedByFiscalYear(fiscalYear.getId(), appUserId)
                .stream().filter(e -> !e.isExcludeFromReturn() && e.getAmount() != null && e.getTransactionDate() != null)
                .toList();
        List<ExpenseEntry> expenses = expenseEntryRepository.findAllOwnedByFiscalYear(fiscalYear.getId(), appUserId)
                .stream().filter(e -> !e.isExcludeFromReturn() && e.getAmount() != null && e.getTransactionDate() != null)
                .toList();
        java.util.Arrays.fill(calc.incomeByMonth, BigDecimal.ZERO);
        java.util.Arrays.fill(calc.expenseByMonth, BigDecimal.ZERO);

        Map<String, BigDecimal> incomeItems = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseItems = new LinkedHashMap<>();
        List<Object[]> sortable = new ArrayList<>();
        for (IncomeEntry e : incomes) {
            int idx = e.getTransactionDate().getMonthValue() - 1;
            calc.incomeByMonth[idx] = calc.incomeByMonth[idx].add(e.getAmount());
            calc.sales = calc.sales.add(e.getAmount());
            String item = e.getIncomeItem() == null ? UNCATEGORIZED : e.getIncomeItem().getItemName();
            incomeItems.merge(item, e.getAmount(), BigDecimal::add);
            String debit = e.getReceiveMethod() == null ? "普通預金" : e.getReceiveMethod().getMethodName();
            String credit = e.getIncomeItem() == null ? "売上高" : e.getIncomeItem().getItemName();
            sortable.add(new Object[] {e.getTransactionDate(), e.getId(), 0,
                    journalLine(e.getTransactionDate().toString(), e.getDetails(), item, debit, e.getAmount(), credit, e.getAmount())});
        }
        for (ExpenseEntry e : expenses) {
            BigDecimal amount = e.getDeductibleAmount() != null ? e.getDeductibleAmount() : e.getAmount();
            int idx = e.getTransactionDate().getMonthValue() - 1;
            calc.expenseByMonth[idx] = calc.expenseByMonth[idx].add(amount);
            String item = e.getExpenseItem() == null ? UNCATEGORIZED : e.getExpenseItem().getItemName();
            expenseItems.merge(item, amount, BigDecimal::add);
            String credit = e.getPaymentMethod() == null ? "現金" : e.getPaymentMethod().getMethodName();
            BigDecimal ownerDraw = e.isHomeApportionmentApplied()
                    ? e.getAmount().subtract(amount).max(BigDecimal.ZERO)
                    : BigDecimal.ZERO;
            // 家事按分は複合仕訳: 借方 経費(按分額)＋事業主貸(残額) / 貸方 支払方法(支出総額)
            BigDecimal creditAmount = amount.add(ownerDraw);
            sortable.add(new Object[] {e.getTransactionDate(), e.getId(), 1,
                    journalLine(e.getTransactionDate().toString(), e.getDetails(), item, item, amount, credit, creditAmount)});
            if (ownerDraw.signum() > 0) {
                calc.ownerDraw = calc.ownerDraw.add(ownerDraw);
                JournalLine head = journalLine(e.getTransactionDate().toString(), e.getDetails(), item, OWNER_DRAW_LABEL, ownerDraw, null, null);
                sortable.add(new Object[] {e.getTransactionDate(), e.getId(), 2,
                        new JournalLine(head.getDate(), head.getSummary(), OWNER_DRAW_LABEL, ownerDraw, null, null, true)});
            }
        }
        if (settlement != null) {
            BigDecimal depreciation = settlement.getDepreciation();
            BigDecimal inventory = settlement.getOpeningInventory().subtract(settlement.getClosingInventory());
            if (depreciation.signum() != 0) {
                expenseItems.merge(DEPRECIATION_LABEL, depreciation, BigDecimal::add);
            }
            if (inventory.signum() != 0) {
                expenseItems.merge(INVENTORY_LABEL, inventory, BigDecimal::add);
            }
            calc.expenseByMonth[11] = calc.expenseByMonth[11].add(depreciation).add(inventory);
        }
        for (BigDecimal v : expenseItems.values()) {
            calc.expenses = calc.expenses.add(v);
        }
        calc.income = calc.sales.subtract(calc.expenses);

        incomeItems.forEach((k, v) -> calc.plRows.add(new PlRow(k, v)));
        expenseItems.forEach((k, v) -> calc.plRows.add(new PlRow(k, v)));

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

    private void upsertSettlementJournalEntry(FiscalYear fiscalYear, SettlementAdjustment adjustment) {
        if (fiscalYear == null || adjustment == null) {
            return;
        }
        JournalEntry entry = journalEntryRepository.findBySourceTypeAndSourceId(SOURCE_TYPE_SETTLEMENT, adjustment.getId())
                .orElseGet(JournalEntry::new);
        entry.setFiscalYear(fiscalYear);
        entry.setAccountingTransaction(null);
        entry.setSourceType(SOURCE_TYPE_SETTLEMENT);
        entry.setSourceId(adjustment.getId());
        entry.setEntryDate(fiscalYear.getPeriodEnd());
        entry.setSummary("決算整理仕訳");
        entry.setStatus(DRAFT);

        List<org.example.entity.JournalLine> lines = new ArrayList<>();
        int lineNo = 1;

        BigDecimal depreciation = safeAmount(adjustment.getDepreciation());
        if (depreciation.signum() > 0) {
            lines.add(buildPersistedLine(lineNo++, "D", DEPRECIATION_LABEL, depreciation, "決算整理"));
            lines.add(buildPersistedLine(lineNo++, "C", "減価償却累計額", depreciation, "決算整理"));
        }

        BigDecimal inventoryDelta = safeAmount(adjustment.getOpeningInventory()).subtract(safeAmount(adjustment.getClosingInventory()));
        if (inventoryDelta.signum() > 0) {
            lines.add(buildPersistedLine(lineNo++, "D", "仕入高", inventoryDelta, "棚卸調整"));
            lines.add(buildPersistedLine(lineNo++, "C", "棚卸資産", inventoryDelta, "棚卸調整"));
        } else if (inventoryDelta.signum() < 0) {
            BigDecimal abs = inventoryDelta.abs();
            lines.add(buildPersistedLine(lineNo++, "D", "棚卸資産", abs, "棚卸調整"));
            lines.add(buildPersistedLine(lineNo++, "C", "仕入高", abs, "棚卸調整"));
        }

        entry.setLines(lines);
        journalEntryRepository.save(entry);
    }

    private org.example.entity.JournalLine buildPersistedLine(
            int lineNo,
            String debitCredit,
            String accountName,
            BigDecimal amount,
            String memo) {
        org.example.entity.JournalLine line = new org.example.entity.JournalLine();
        line.setLineNo(lineNo);
        line.setDebitCredit(debitCredit);
        line.setAccountName(accountName);
        line.setAmount(safeAmount(amount));
        line.setMemo(memo);
        return line;
    }

    private List<JournalLine> buildJournalFromPersistedEntries(Long fiscalYearId) {
        List<JournalEntry> entries = journalEntryRepository.findAllWithLinesByFiscalYearId(fiscalYearId);
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

    private BalanceSheet balanceSheet(SettlementAdjustment s, BigDecimal income, BigDecimal ownerDraw) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal openingBalance = s == null ? zero : s.getOpeningBalance();
        BigDecimal openingInventory = s == null ? zero : s.getOpeningInventory();
        BigDecimal closingInventory = s == null ? zero : s.getClosingInventory();
        BigDecimal cash = s == null ? zero : s.getClosingCash();
        BigDecimal receivable = s == null ? zero : s.getAccountsReceivable();
        BigDecimal unreceived = s == null ? zero : s.getUnreceivedIncome();
        BigDecimal payable = s == null ? zero : s.getAccountsPayable();
        BigDecimal unpaid = s == null ? zero : s.getUnpaidExpenses();
        BigDecimal capital = openingBalance.add(openingInventory);

        BalanceSheet bs = new BalanceSheet();
        bs.assets.add(new BsRow("現預金", openingBalance, cash));
        bs.assets.add(new BsRow("売掛金", zero, receivable));
        bs.assets.add(new BsRow("未収入金", zero, unreceived));
        bs.assets.add(new BsRow("棚卸資産", openingInventory, closingInventory));
        bs.assets.add(new BsRow(OWNER_DRAW_LABEL, zero, ownerDraw));
        bs.liabilities.add(new BsRow("買掛金", zero, payable));
        bs.liabilities.add(new BsRow("未払金", zero, unpaid));
        bs.liabilities.add(new BsRow("元入金", capital, capital));
        bs.liabilities.add(new BsRow("青色申告特別控除前所得", zero, income));
        for (BsRow r : bs.assets) {
            bs.assetsOpening = bs.assetsOpening.add(r.getOpening());
            bs.assetsClosing = bs.assetsClosing.add(r.getClosing());
        }
        for (BsRow r : bs.liabilities) {
            bs.liabilitiesOpening = bs.liabilitiesOpening.add(r.getOpening());
            bs.liabilitiesClosing = bs.liabilitiesClosing.add(r.getClosing());
        }
        return bs;
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
        boolean journalBalanced = journalEntries(calc.journal).stream()
                .allMatch(entry -> entry.stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .compareTo(entry.stream().map(JournalLine::getCreditAmount).filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)) == 0);
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
        if (deduction == 65) {
            warnings.add("65万円控除は複式簿記による記帳・貸借対照表の添付などの要件を満たす必要があります。");
        }

        boolean hasData = !calc.plRows.isEmpty();
        checks.add(new CheckItem("check-profit-loss", "損益計算書", hasData && saved != null,
                !hasData ? "損益データがありません" : saved == null ? "決算整理が未保存です" : "入力完了"));
        checks.add(new CheckItem("check-monthly-sales", "月別売上", monthlyOk,
                monthlyOk ? "入力完了" : "月別合計が年間合計と一致していません"));
        checks.add(new CheckItem("check-depreciation", "減価償却", saved != null,
                saved != null ? "入力完了" : "減価償却費が未保存です"));
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
    }

    private static class BalanceSheet {
        final List<BsRow> assets = new ArrayList<>();
        final List<BsRow> liabilities = new ArrayList<>();
        BigDecimal assetsOpening = BigDecimal.ZERO;
        BigDecimal assetsClosing = BigDecimal.ZERO;
        BigDecimal liabilitiesOpening = BigDecimal.ZERO;
        BigDecimal liabilitiesClosing = BigDecimal.ZERO;
    }
}
