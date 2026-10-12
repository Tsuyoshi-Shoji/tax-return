package org.example.features.taxreturn.dto;

import java.math.BigDecimal;
import java.util.List;

import org.example.features.report.dto.ReportMonthRow;
import org.example.features.report.dto.ReportYearOption;
import org.example.features.taxreturn.form.SettlementForm;

public class TaxReturnPageData {
    private final List<ReportYearOption> yearOptions;
    private final int selectedYear;
    private final String yearLabel;
    private final boolean closed;
    private final BasicInfo basicInfo;
    private final int deduction;
    private final BigDecimal sales;
    private final BigDecimal expenses;
    private final BigDecimal income;
    private final BigDecimal incomeAfterDeduction;
    private final Diff salesDiff;
    private final Diff expensesDiff;
    private final Diff incomeDiff;
    private final List<ReportMonthRow> months;
    private final List<PlRow> plRows;
    private final SettlementForm settlement;
    private final List<BsRow> assets;
    private final List<BsRow> liabilities;
    private final BigDecimal assetsOpening;
    private final BigDecimal assetsClosing;
    private final BigDecimal liabilitiesOpening;
    private final BigDecimal liabilitiesClosing;
    private final List<JournalLine> journalPreview;
    private final List<JournalLine> journalAll;
    private final List<LedgerAccount> ledger;
    private final List<CheckItem> checks;
    private final List<String> errors;
    private final List<String> warnings;

    public TaxReturnPageData(
            List<ReportYearOption> yearOptions, int selectedYear, String yearLabel, boolean closed,
            BasicInfo basicInfo, int deduction,
            BigDecimal sales, BigDecimal expenses, BigDecimal income, BigDecimal incomeAfterDeduction,
            Diff salesDiff, Diff expensesDiff, Diff incomeDiff,
            List<ReportMonthRow> months, List<PlRow> plRows, SettlementForm settlement,
            List<BsRow> assets, List<BsRow> liabilities,
            BigDecimal assetsOpening, BigDecimal assetsClosing,
            BigDecimal liabilitiesOpening, BigDecimal liabilitiesClosing,
            List<JournalLine> journalPreview, List<JournalLine> journalAll, List<LedgerAccount> ledger,
            List<CheckItem> checks, List<String> errors, List<String> warnings) {
        this.yearOptions = yearOptions;
        this.selectedYear = selectedYear;
        this.yearLabel = yearLabel;
        this.closed = closed;
        this.basicInfo = basicInfo;
        this.deduction = deduction;
        this.sales = sales;
        this.expenses = expenses;
        this.income = income;
        this.incomeAfterDeduction = incomeAfterDeduction;
        this.salesDiff = salesDiff;
        this.expensesDiff = expensesDiff;
        this.incomeDiff = incomeDiff;
        this.months = months;
        this.plRows = plRows;
        this.settlement = settlement;
        this.assets = assets;
        this.liabilities = liabilities;
        this.assetsOpening = assetsOpening;
        this.assetsClosing = assetsClosing;
        this.liabilitiesOpening = liabilitiesOpening;
        this.liabilitiesClosing = liabilitiesClosing;
        this.journalPreview = journalPreview;
        this.journalAll = journalAll;
        this.ledger = ledger;
        this.checks = checks;
        this.errors = errors;
        this.warnings = warnings;
    }

    public List<ReportYearOption> getYearOptions() { return yearOptions; }
    public int getSelectedYear() { return selectedYear; }
    public String getYearLabel() { return yearLabel; }
    public boolean isClosed() { return closed; }
    public BasicInfo getBasicInfo() { return basicInfo; }
    public int getDeduction() { return deduction; }
    public BigDecimal getSales() { return sales; }
    public BigDecimal getExpenses() { return expenses; }
    public BigDecimal getIncome() { return income; }
    public BigDecimal getIncomeAfterDeduction() { return incomeAfterDeduction; }
    public Diff getSalesDiff() { return salesDiff; }
    public Diff getExpensesDiff() { return expensesDiff; }
    public Diff getIncomeDiff() { return incomeDiff; }
    public List<ReportMonthRow> getMonths() { return months; }
    public List<PlRow> getPlRows() { return plRows; }
    public SettlementForm getSettlement() { return settlement; }
    public List<BsRow> getAssets() { return assets; }
    public List<BsRow> getLiabilities() { return liabilities; }
    public BigDecimal getAssetsOpening() { return assetsOpening; }
    public BigDecimal getAssetsClosing() { return assetsClosing; }
    public BigDecimal getLiabilitiesOpening() { return liabilitiesOpening; }
    public BigDecimal getLiabilitiesClosing() { return liabilitiesClosing; }
    public List<JournalLine> getJournalPreview() { return journalPreview; }
    public List<JournalLine> getJournalAll() { return journalAll; }
    public List<LedgerAccount> getLedger() { return ledger; }
    public List<CheckItem> getChecks() { return checks; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public boolean isBalanced() {
        return assetsOpening.compareTo(liabilitiesOpening) == 0 && assetsClosing.compareTo(liabilitiesClosing) == 0;
    }
    public BigDecimal getBalanceDifference() { return assetsClosing.subtract(liabilitiesClosing); }
    public BigDecimal getMonthsIncomeTotal() {
        return months.stream().map(ReportMonthRow::getIncome).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public BigDecimal getMonthsExpenseTotal() {
        return months.stream().map(ReportMonthRow::getExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static class BasicInfo {
        private final String name;
        private final String businessName;
        private final String industry;
        private final String phone;
        private final String address;
        private final String email;
        private final String filingTypeLabel;

        public BasicInfo(String name, String businessName, String industry, String phone, String address,
                String email, String filingTypeLabel) {
            this.name = name;
            this.businessName = businessName;
            this.industry = industry;
            this.phone = phone;
            this.address = address;
            this.email = email;
            this.filingTypeLabel = filingTypeLabel;
        }

        public String getName() { return name; }
        public String getBusinessName() { return businessName; }
        public String getIndustry() { return industry; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
        public String getEmail() { return email; }
        public String getFilingTypeLabel() { return filingTypeLabel; }
    }

    public static class Diff {
        private final boolean available;
        private final String amountText;
        private final String rateText;
        private final boolean up;
        private final boolean good;

        public Diff(boolean available, String amountText, String rateText, boolean up, boolean good) {
            this.available = available;
            this.amountText = amountText;
            this.rateText = rateText;
            this.up = up;
            this.good = good;
        }

        public boolean isAvailable() { return available; }
        public String getAmountText() { return amountText; }
        public String getRateText() { return rateText; }
        public boolean isUp() { return up; }
        public boolean isGood() { return good; }
    }

    public static class PlRow {
        private final String label;
        private final BigDecimal amount;

        public PlRow(String label, BigDecimal amount) {
            this.label = label;
            this.amount = amount;
        }

        public String getLabel() { return label; }
        public BigDecimal getAmount() { return amount; }
    }

    public static class BsRow {
        private final String label;
        private final BigDecimal opening;
        private final BigDecimal closing;

        public BsRow(String label, BigDecimal opening, BigDecimal closing) {
            this.label = label;
            this.opening = opening;
            this.closing = closing;
        }

        public String getLabel() { return label; }
        public BigDecimal getOpening() { return opening; }
        public BigDecimal getClosing() { return closing; }
    }

    public static class JournalLine {
        private final String date;
        private final String summary;
        private final String debitAccount;
        private final BigDecimal debitAmount;
        private final String creditAccount;
        private final BigDecimal creditAmount;
        /** 複合仕訳の2行目以降（日付・摘要・貸方を表示しない行）。 */
        private final boolean continuation;

        public JournalLine(String date, String summary, String debitAccount, BigDecimal debitAmount,
                String creditAccount, BigDecimal creditAmount) {
            this(date, summary, debitAccount, debitAmount, creditAccount, creditAmount, false);
        }

        public JournalLine(String date, String summary, String debitAccount, BigDecimal debitAmount,
                String creditAccount, BigDecimal creditAmount, boolean continuation) {
            this.date = date;
            this.summary = summary;
            this.debitAccount = debitAccount;
            this.debitAmount = debitAmount;
            this.creditAccount = creditAccount;
            this.creditAmount = creditAmount;
            this.continuation = continuation;
        }

        public boolean isContinuation() { return continuation; }

        public String getDate() { return date; }
        public String getSummary() { return summary; }
        public String getDebitAccount() { return debitAccount; }
        public BigDecimal getDebitAmount() { return debitAmount; }
        public String getCreditAccount() { return creditAccount; }
        public BigDecimal getCreditAmount() { return creditAmount; }
    }

    public static class LedgerLine {
        private final String date;
        private final String summary;
        private final String counterAccount;
        private final BigDecimal debit;
        private final BigDecimal credit;

        public LedgerLine(String date, String summary, String counterAccount, BigDecimal debit, BigDecimal credit) {
            this.date = date;
            this.summary = summary;
            this.counterAccount = counterAccount;
            this.debit = debit;
            this.credit = credit;
        }

        public String getDate() { return date; }
        public String getSummary() { return summary; }
        public String getCounterAccount() { return counterAccount; }
        public BigDecimal getDebit() { return debit; }
        public BigDecimal getCredit() { return credit; }
    }

    public static class LedgerAccount {
        private final String name;
        private final List<LedgerLine> lines;

        public LedgerAccount(String name, List<LedgerLine> lines) {
            this.name = name;
            this.lines = lines;
        }

        public String getName() { return name; }
        public List<LedgerLine> getLines() { return lines; }
    }

    public static class CheckItem {
        private final String id;
        private final String label;
        private final boolean ok;
        private final String message;

        public CheckItem(String id, String label, boolean ok, String message) {
            this.id = id;
            this.label = label;
            this.ok = ok;
            this.message = message;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public boolean isOk() { return ok; }
        public String getMessage() { return message; }
    }
}
