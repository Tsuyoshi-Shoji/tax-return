package org.example.features.report.dto;

import java.util.List;

public class ReportDetailPageData {
    private final int year;
    private final int month;
    private final String monthLabel;
    private final List<ReportMonthOption> monthOptions;
    private final int[] prevYearMonth;
    private final int[] nextYearMonth;
    private final ReportSection allData;
    private final ReportSection business;
    private final List<List<ReportCalendarDay>> calendarWeeks;

    public ReportDetailPageData(
            int year,
            int month,
            String monthLabel,
            List<ReportMonthOption> monthOptions,
            int[] prevYearMonth,
            int[] nextYearMonth,
            ReportSection allData,
            ReportSection business,
            List<List<ReportCalendarDay>> calendarWeeks) {
        this.year = year;
        this.month = month;
        this.monthLabel = monthLabel;
        this.monthOptions = monthOptions;
        this.prevYearMonth = prevYearMonth;
        this.nextYearMonth = nextYearMonth;
        this.allData = allData;
        this.business = business;
        this.calendarWeeks = calendarWeeks;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public String getMonthLabel() { return monthLabel; }
    public List<ReportMonthOption> getMonthOptions() { return monthOptions; }
    public boolean isHasPrev() { return prevYearMonth != null; }
    public boolean isHasNext() { return nextYearMonth != null; }
    public int getPrevYear() { return prevYearMonth == null ? 0 : prevYearMonth[0]; }
    public int getPrevMonth() { return prevYearMonth == null ? 0 : prevYearMonth[1]; }
    public int getNextYear() { return nextYearMonth == null ? 0 : nextYearMonth[0]; }
    public int getNextMonth() { return nextYearMonth == null ? 0 : nextYearMonth[1]; }
    public ReportSection getAllData() { return allData; }
    public ReportSection getBusiness() { return business; }
    public List<List<ReportCalendarDay>> getCalendarWeeks() { return calendarWeeks; }
}