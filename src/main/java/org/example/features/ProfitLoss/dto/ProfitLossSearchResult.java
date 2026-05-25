package org.example.features.profitloss.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProfitLossSearchResult(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal profitAmount,
        List<ProfitLossListRow> incomeRows,
        List<ProfitLossListRow> expenseRows,
        int incomeCurrentPage,
        int incomeTotalPages,
        int expenseCurrentPage,
        int expenseTotalPages) {
}

