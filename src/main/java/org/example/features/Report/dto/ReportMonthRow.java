package org.example.features.report.dto;

import java.math.BigDecimal;

public class ReportMonthRow {
    private final int month;
    private final BigDecimal income;
    private final BigDecimal expense;
    private final int incomeBarPercent;
    private final int expenseBarPercent;

    public ReportMonthRow(int month, BigDecimal income, BigDecimal expense, int incomeBarPercent, int expenseBarPercent) {
        this.month = month;
        this.income = income;
        this.expense = expense;
        this.incomeBarPercent = incomeBarPercent;
        this.expenseBarPercent = expenseBarPercent;
    }

    public int getMonth() { return month; }
    public BigDecimal getIncome() { return income; }
    public BigDecimal getExpense() { return expense; }
    public BigDecimal getProfit() { return income.subtract(expense); }
    public int getIncomeBarPercent() { return incomeBarPercent; }
    public int getExpenseBarPercent() { return expenseBarPercent; }
}