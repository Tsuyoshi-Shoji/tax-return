package org.example.features.profitloss.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProfitLossListRow {
    private final Long id;
    private final LocalDate date;
    private final String type;
    private final String item;
    private final String details;
    private final BigDecimal amount;
    private final boolean taxTarget;
    private final String recordType;

    public ProfitLossListRow(
            Long id,
            LocalDate date,
            String type,
            String item,
            String details,
            BigDecimal amount,
            boolean taxTarget,
            String recordType) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.item = item;
        this.details = details;
        this.amount = amount;
        this.taxTarget = taxTarget;
        this.recordType = recordType;
    }

    public Long getId() {
        return id;
    }

    public String getDateLabel() {
        return date == null ? "" : date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    public LocalDate getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getItem() {
        return item;
    }

    public String getDetails() {
        return details;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isTaxTarget() {
        return taxTarget;
    }

    public String getRecordType() {
        return recordType;
    }
}
