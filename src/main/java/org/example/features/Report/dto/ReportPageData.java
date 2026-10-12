package org.example.features.report.dto;

import java.util.List;

public class ReportPageData {
    private final List<ReportYearOption> yearOptions;
    private final int selectedYear;
    private final String selectedYearLabel;
    private final ReportSection allData;
    private final ReportSection business;

    public ReportPageData(
            List<ReportYearOption> yearOptions,
            int selectedYear,
            String selectedYearLabel,
            ReportSection allData,
            ReportSection business) {
        this.yearOptions = yearOptions;
        this.selectedYear = selectedYear;
        this.selectedYearLabel = selectedYearLabel;
        this.allData = allData;
        this.business = business;
    }

    public List<ReportYearOption> getYearOptions() { return yearOptions; }
    public int getSelectedYear() { return selectedYear; }
    public String getSelectedYearLabel() { return selectedYearLabel; }
    public ReportSection getAllData() { return allData; }
    public ReportSection getBusiness() { return business; }
}