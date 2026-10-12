package org.example.features.entry.service;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
