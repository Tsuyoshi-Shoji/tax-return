package org.example.features.report.dto;

import java.math.BigDecimal;

public class ReportItemRow {
    private final String name;
    private final BigDecimal amount;
    private final BigDecimal percent;
    private final String color;

    public ReportItemRow(String name, BigDecimal amount, BigDecimal percent, String color) {
        this.name = name;
        this.amount = amount;
        this.percent = percent;
        this.color = color;
    }

    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPercent() { return percent; }
    public String getColor() { return color; }
}