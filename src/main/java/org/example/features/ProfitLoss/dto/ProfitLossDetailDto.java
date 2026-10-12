package org.example.features.profitloss.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProfitLossDetailDto {
    private final String recordType;
    private final Long id;
    private final String categoryLabel;
    private final String itemName;
    private final LocalDate transactionDate;
    private final BigDecimal amount;
    private final String methodLabel;
    private final String methodName;
    private final boolean excludeFromReturn;
    private final boolean apportionmentApplied;
    private final BigDecimal apportionmentRate;
    private final BigDecimal deductibleAmount;
    private final String details;
    private final String lockMessage;

    public ProfitLossDetailDto(
            String recordType,
            Long id,
            String categoryLabel,
            String itemName,
            LocalDate transactionDate,
            BigDecimal amount,
            String methodLabel,
            String methodName,
            boolean excludeFromReturn,
            boolean apportionmentApplied,
            BigDecimal apportionmentRate,
            BigDecimal deductibleAmount,
            String details,
            String lockMessage) {
        this.recordType = recordType;
        this.id = id;
        this.categoryLabel = categoryLabel;
        this.itemName = itemName;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.methodLabel = methodLabel;
        this.methodName = methodName;
        this.excludeFromReturn = excludeFromReturn;
        this.apportionmentApplied = apportionmentApplied;
        this.apportionmentRate = apportionmentRate;
        this.deductibleAmount = deductibleAmount;
        this.details = details;
        this.lockMessage = lockMessage;
    }

    public String getRecordType() { return recordType; }
    public Long getId() { return id; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getItemName() { return itemName; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public String getTransactionDateText() {
        return transactionDate == null ? "-" : transactionDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    public BigDecimal getAmount() { return amount; }
    public String getMethodLabel() { return methodLabel; }
    public String getMethodName() { return methodName; }
    public boolean isExcludeFromReturn() { return excludeFromReturn; }
    public boolean isApportionmentApplied() { return apportionmentApplied; }
    public BigDecimal getApportionmentRate() { return apportionmentRate; }
    public BigDecimal getDeductibleAmount() { return deductibleAmount; }
    public String getDetails() { return details; }
    public String getLockMessage() { return lockMessage; }
    public boolean isEditable() { return lockMessage == null; }
    public boolean isExpense() { return "expense".equals(recordType); }
}