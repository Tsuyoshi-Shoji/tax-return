package org.example.features.home.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecentTransactionDto {
    private final LocalDate date;
    private final String type;
    private final String category;
    private final String details;
    private final BigDecimal amount;

    public RecentTransactionDto(LocalDate date, String type, String category, String details, BigDecimal amount) {
        this.date = date;
        this.type = type;
        this.category = category;
        this.details = details;
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getDetails() {
        return details;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
