package org.example.features.report.service;

import org.example.features.report.dto.ReportDetailPageData;
import org.example.features.report.dto.ReportPageData;

public interface ReportService {
    ReportPageData loadReport(Long appUserId, Integer year);

    ReportDetailPageData loadDetail(Long appUserId, Integer year, Integer month);
}