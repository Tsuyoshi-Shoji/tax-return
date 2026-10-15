package org.example.features.entry.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.example.entity.ExpenseItem;
import org.example.entity.FiscalYear;
import org.example.entity.IncomeItem;
import org.example.entity.PaymentMethod;
import org.example.entity.ReceiveMethod;
import org.example.features.entry.EntryKind;

public class ValidatedEntryCommand {
    private final EntryKind kind;
    private final Long appUserId;
    private final Long editId;
    private final Long version;
    private final FiscalYear fiscalYear;
    private final IncomeItem incomeItem;
    private final ExpenseItem expenseItem;
    private final ReceiveMethod receiveMethod;
    private final PaymentMethod paymentMethod;
    private final LocalDate transactionDate;
    private final BigDecimal amount;
    private final boolean excludeFromReturn;
    private final boolean homeApportionment;
    private final BigDecimal homeApportionmentRate;
    private final BigDecimal deductibleAmount;
    private final String details;
    private final String payloadHash;
    private final boolean confirmationRequired;
    private final String targetMonth;
    private final List<ValidatedEntryCommand> monthlyEntries;

    public ValidatedEntryCommand(
            EntryKind kind,
            Long appUserId,
            Long editId,
            Long version,
            FiscalYear fiscalYear,
            IncomeItem incomeItem,
            ExpenseItem expenseItem,
            ReceiveMethod receiveMethod,
            PaymentMethod paymentMethod,
            LocalDate transactionDate,
            BigDecimal amount,
            boolean excludeFromReturn,
            boolean homeApportionment,
            BigDecimal homeApportionmentRate,
            BigDecimal deductibleAmount,
            String details,
            String payloadHash,
            boolean confirmationRequired) {
        this(
                kind,
                appUserId,
                editId,
                version,
                fiscalYear,
                incomeItem,
                expenseItem,
                receiveMethod,
                paymentMethod,
                transactionDate,
                amount,
                excludeFromReturn,
                homeApportionment,
                homeApportionmentRate,
                deductibleAmount,
                details,
                payloadHash,
                confirmationRequired,
                "",
                List.of());
    }

    private ValidatedEntryCommand(
            EntryKind kind,
            Long appUserId,
            Long editId,
            Long version,
            FiscalYear fiscalYear,
            IncomeItem incomeItem,
            ExpenseItem expenseItem,
            ReceiveMethod receiveMethod,
            PaymentMethod paymentMethod,
            LocalDate transactionDate,
            BigDecimal amount,
            boolean excludeFromReturn,
            boolean homeApportionment,
            BigDecimal homeApportionmentRate,
            BigDecimal deductibleAmount,
            String details,
            String payloadHash,
            boolean confirmationRequired,
            String targetMonth,
            List<ValidatedEntryCommand> monthlyEntries) {
        this.kind = kind;
        this.appUserId = appUserId;
        this.editId = editId;
        this.version = version;
        this.fiscalYear = fiscalYear;
        this.incomeItem = incomeItem;
        this.expenseItem = expenseItem;
        this.receiveMethod = receiveMethod;
        this.paymentMethod = paymentMethod;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.excludeFromReturn = excludeFromReturn;
        this.homeApportionment = homeApportionment;
        this.homeApportionmentRate = homeApportionmentRate;
        this.deductibleAmount = deductibleAmount;
        this.details = details;
        this.payloadHash = payloadHash;
        this.confirmationRequired = confirmationRequired;
        this.targetMonth = targetMonth == null ? "" : targetMonth;
        this.monthlyEntries = monthlyEntries == null ? List.of() : List.copyOf(monthlyEntries);
    }

    public static ValidatedEntryCommand monthlyExpenseBatch(
            Long appUserId,
            String targetMonth,
            List<ValidatedEntryCommand> monthlyEntries,
            String payloadHash,
            boolean confirmationRequired) {
        return new ValidatedEntryCommand(
                EntryKind.EXPENSE,
                appUserId,
                null,
                0L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "",
                payloadHash,
                confirmationRequired,
                targetMonth,
                monthlyEntries);
    }

    public EntryKind getKind() {
        return kind;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public Long getEditId() {
        return editId;
    }

    public Long getVersion() {
        return version;
    }

    public FiscalYear getFiscalYear() {
        return fiscalYear;
    }

    public IncomeItem getIncomeItem() {
        return incomeItem;
    }

    public ExpenseItem getExpenseItem() {
        return expenseItem;
    }

    public ReceiveMethod getReceiveMethod() {
        return receiveMethod;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isExcludeFromReturn() {
        return excludeFromReturn;
    }

    public boolean isHomeApportionment() {
        return homeApportionment;
    }

    public BigDecimal getHomeApportionmentRate() {
        return homeApportionmentRate;
    }

    public BigDecimal getDeductibleAmount() {
        return deductibleAmount;
    }

    public String getDetails() {
        return details;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public String getTargetMonth() {
        return targetMonth;
    }

    public List<ValidatedEntryCommand> getMonthlyEntries() {
        return monthlyEntries;
    }

    public boolean isMonthlyMode() {
        return !monthlyEntries.isEmpty();
    }
}
