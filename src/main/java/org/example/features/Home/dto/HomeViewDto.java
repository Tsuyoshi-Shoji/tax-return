package org.example.features.home.dto;

import java.time.LocalDate;
import java.util.List;

public class HomeViewDto {
    private final List<FiscalYearOptionDto> fiscalYearOptions;
    private final Long selectedFiscalYearId;
    private final HomeSummaryDto summary;
    private final List<RecentTransactionDto> recentTransactions;
    private final long daysUntilDeadline;
    private final LocalDate deadlineDate;

    public HomeViewDto(
            List<FiscalYearOptionDto> fiscalYearOptions,
            Long selectedFiscalYearId,
            HomeSummaryDto summary,
            List<RecentTransactionDto> recentTransactions,
            long daysUntilDeadline,
            LocalDate deadlineDate) {
        this.fiscalYearOptions = fiscalYearOptions;
        this.selectedFiscalYearId = selectedFiscalYearId;
        this.summary = summary;
        this.recentTransactions = recentTransactions;
        this.daysUntilDeadline = daysUntilDeadline;
        this.deadlineDate = deadlineDate;
    }

    public List<FiscalYearOptionDto> getFiscalYearOptions() {
        return fiscalYearOptions;
    }

    public Long getSelectedFiscalYearId() {
        return selectedFiscalYearId;
    }

    public HomeSummaryDto getSummary() {
        return summary;
    }

    public List<RecentTransactionDto> getRecentTransactions() {
        return recentTransactions;
    }

    public long getDaysUntilDeadline() {
        return daysUntilDeadline;
    }

    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }
}
