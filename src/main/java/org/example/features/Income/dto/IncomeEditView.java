package org.example.features.income.dto;

public record IncomeEditView(
        Long id,
        String incomeType,
        Long subcategoryId,
        String date,
        String amount,
        String details,
        Long receiveMethodId,
        Boolean businessTartget) {
}

