package org.example.features.report.dto;

public class ReportCalendarDay {
    private final int day;
    private final boolean inMonth;
    private final boolean hasIncome;
    private final boolean hasExpense;
    private final boolean hasExcluded;

    public ReportCalendarDay(int day, boolean inMonth, boolean hasIncome, boolean hasExpense, boolean hasExcluded) {
        this.day = day;
        this.inMonth = inMonth;
        this.hasIncome = hasIncome;
        this.hasExpense = hasExpense;
        this.hasExcluded = hasExcluded;
    }

    public int getDay() { return day; }
    public boolean isInMonth() { return inMonth; }
    public boolean isHasIncome() { return hasIncome; }
    public boolean isHasExpense() { return hasExpense; }
    public boolean isHasExcluded() { return hasExcluded; }
}