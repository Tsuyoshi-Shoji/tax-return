package org.example.features.taxreturn.export;

import java.time.LocalDate;

public record BookExportFilter(LocalDate fromDate, LocalDate toDate, String accountCode, boolean fullPeriod) {
}
