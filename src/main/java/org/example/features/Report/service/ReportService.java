package org.example.features.report.service;

import org.example.features.report.dto.ReportDetailView;
import org.example.features.report.dto.ReportView;

public interface ReportService {

    ReportView getYearlyReport(int year);

    ReportView getYearlyReport(int year, boolean includeTaxExcluded);

    ReportDetailView getMonthlyReport(int year, int month);

    ReportDetailView getMonthlyReport(int year, int month, boolean includeTaxExcluded);
}

