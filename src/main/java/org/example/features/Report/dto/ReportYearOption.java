package org.example.features.report.dto;

public class ReportYearOption {
    private final int year;
    private final String label;

    public ReportYearOption(int year, String label) {
        this.year = year;
        this.label = label;
    }

    public int getYear() { return year; }
    public String getLabel() { return label; }
}