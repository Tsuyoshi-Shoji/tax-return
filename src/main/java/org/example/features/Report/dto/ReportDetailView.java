package org.example.features.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportDetailView(
        int year,
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal profitAmount,
        List<ItemAmountRow> incomeBreakdown,
        List<ItemAmountRow> expenseBreakdown,
        List<CalendarEvent> calendarEvents) {
}

