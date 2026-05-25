package org.example.features.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportView(
        int year,
        List<Integer> selectableYears,
        List<MonthlyAmountRow> monthlyRows,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal profitAmount) {
}

