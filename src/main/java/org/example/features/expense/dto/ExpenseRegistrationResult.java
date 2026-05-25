package org.example.features.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public class ExpenseRegistrationResult {

    private final boolean success;
    private final List<String> errors;
    private final BigDecimal amount;
    private final BigDecimal deductibleAmount;
    private final BigDecimal businessUseRatio;

    private ExpenseRegistrationResult(
            boolean success,
            List<String> errors,
            BigDecimal amount,
            BigDecimal deductibleAmount,
            BigDecimal businessUseRatio) {
        this.success = success;
        this.errors = List.copyOf(errors);
        this.amount = amount;
        this.deductibleAmount = deductibleAmount;
        this.businessUseRatio = businessUseRatio;
    }

    public static ExpenseRegistrationResult failure(List<String> errors, BigDecimal businessUseRatio) {
        return new ExpenseRegistrationResult(false, errors, BigDecimal.ZERO, BigDecimal.ZERO, businessUseRatio);
    }

    public static ExpenseRegistrationResult success(
            BigDecimal amount,
            BigDecimal deductibleAmount,
            BigDecimal businessUseRatio) {
        return new ExpenseRegistrationResult(true, List.of(), amount, deductibleAmount, businessUseRatio);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getFirstErrorMessage() {
        return errors.isEmpty() ? "入力内容を確認してください。" : errors.getFirst();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getDeductibleAmount() {
        return deductibleAmount;
    }

    public BigDecimal getBusinessUseRatio() {
        return businessUseRatio;
    }
}

