package org.example.features.expense.dto;

public record ExpenseEditView(
        Long id,
        String expenseType,
        Long subcategoryId,
        String date,
        String amount,
        String details,
        boolean homeApportionment,
        Long paymentMethodId) {
}

