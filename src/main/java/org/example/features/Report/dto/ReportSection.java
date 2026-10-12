package org.example.features.report.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReportSection {
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final List<ReportMonthRow> months;
    private final List<BigDecimal> axisTicks;
    private final List<ReportItemRow> incomeItems;
    private final List<ReportItemRow> expenseItems;
    private final String incomeGradient;
    private final String expenseGradient;

    public ReportSection(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            List<ReportMonthRow> months,
            List<BigDecimal> axisTicks,
            List<ReportItemRow> incomeItems,
            List<ReportItemRow> expenseItems,
            String incomeGradient,
            String expenseGradient) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.months = months;
        this.axisTicks = axisTicks;
        this.incomeItems = incomeItems;
        this.expenseItems = expenseItems;
        this.incomeGradient = incomeGradient;
        this.expenseGradient = expenseGradient;
    }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getTotalExpense() { return totalExpense; }
    public BigDecimal getProfit() { return totalIncome.subtract(totalExpense); }
    public List<ReportMonthRow> getMonths() { return months; }
    public List<BigDecimal> getAxisTicks() { return axisTicks; }
    public List<ReportItemRow> getIncomeItems() { return incomeItems; }
    public List<ReportItemRow> getExpenseItems() { return expenseItems; }
    public String getIncomeGradient() { return incomeGradient; }
    public String getExpenseGradient() { return expenseGradient; }
}