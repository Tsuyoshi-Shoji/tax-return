package org.example.features.home.dto;

import java.math.BigDecimal;

public class ProfitLossSummaryDto {
    private final BigDecimal income;
    private final BigDecimal expense;
    private final BigDecimal profit;

    public ProfitLossSummaryDto(BigDecimal income, BigDecimal expense) {
        this.income = income;
        this.expense = expense;
        this.profit = income.subtract(expense);
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public BigDecimal getProfit() {
        return profit;
    }
}
