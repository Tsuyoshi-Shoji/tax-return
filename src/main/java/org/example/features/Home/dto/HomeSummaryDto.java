package org.example.features.home.dto;

import java.math.BigDecimal;

public class HomeSummaryDto {
    private final BigDecimal totalAssets;
    private final BigDecimal prevMonthDiff;
    private final BigDecimal annualProfitLoss;
    private final BigDecimal annualIncome;
    private final BigDecimal monthlyIncome;
    private final BigDecimal annualExpense;
    private final BigDecimal monthlyExpense;

    public HomeSummaryDto(
            BigDecimal totalAssets,
            BigDecimal prevMonthDiff,
            BigDecimal annualProfitLoss,
            BigDecimal annualIncome,
            BigDecimal monthlyIncome,
            BigDecimal annualExpense,
            BigDecimal monthlyExpense) {
        this.totalAssets = totalAssets;
        this.prevMonthDiff = prevMonthDiff;
        this.annualProfitLoss = annualProfitLoss;
        this.annualIncome = annualIncome;
        this.monthlyIncome = monthlyIncome;
        this.annualExpense = annualExpense;
        this.monthlyExpense = monthlyExpense;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public BigDecimal getPrevMonthDiff() {
        return prevMonthDiff;
    }

    public BigDecimal getAnnualProfitLoss() {
        return annualProfitLoss;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public BigDecimal getAnnualExpense() {
        return annualExpense;
    }

    public BigDecimal getMonthlyExpense() {
        return monthlyExpense;
    }
}
