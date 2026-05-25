package org.example.features.income.dto;

import java.math.BigDecimal;
import java.util.List;

public class IncomeRegistrationResult {

    private final boolean success;
    private final String message;
    private final List<String> errors;
    private final BigDecimal amount;

    private IncomeRegistrationResult(boolean success, String message, List<String> errors, BigDecimal amount) {
        this.success = success;
        this.message = message;
        this.errors = List.copyOf(errors);
        this.amount = amount;
    }

    public static IncomeRegistrationResult success(String message, BigDecimal amount) {
        return new IncomeRegistrationResult(true, message, List.of(), amount);
    }

    public static IncomeRegistrationResult failure(List<String> errors) {
        return new IncomeRegistrationResult(false, "", errors, BigDecimal.ZERO);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<String> getErrors() { return errors; }
    public String getFirstErrorMessage() { return errors.isEmpty() ? "入力内容を確認してください。" : errors.getFirst(); }
    public BigDecimal getAmount() { return amount; }
}

