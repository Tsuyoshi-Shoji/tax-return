package org.example.features.expense.dto;

public record ExpenseEditView(
        Long id,
        String expenseType,
        Long subcategoryId,
        String date,
        String amount,
        String details,
        Boolean businessTarget,
        boolean homeApportionment,
        Long paymentMethodId) {
}

