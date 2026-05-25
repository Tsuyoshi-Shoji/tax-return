package org.example.features.returnform.dto;

import java.math.BigDecimal;

public record ReturnFormView(
        int year,
        BigDecimal totalIncome,
        BigDecimal deductibleExpense,
        BigDecimal taxableProfit,
        BigDecimal nonDeductibleExpense) {
}

