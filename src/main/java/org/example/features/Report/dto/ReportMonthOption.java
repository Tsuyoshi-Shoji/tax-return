package org.example.features.report.dto;

public class ReportMonthOption {
    private final int month;
    private final String label;

    public ReportMonthOption(int month, String label) {
        this.month = month;
        this.label = label;
    }

    public int getMonth() { return month; }
    public String getLabel() { return label; }
}