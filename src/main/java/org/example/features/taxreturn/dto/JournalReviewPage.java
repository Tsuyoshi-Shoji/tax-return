package org.example.features.taxreturn.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.example.features.report.dto.ReportYearOption;

public class JournalReviewPage {
    private final int year;
    private final List<ReportYearOption> yearOptions;
    private final boolean closed;
    private final String status;
    private final int pageNumber;
    private final int totalPages;
    private final long totalCount;
    private final long draftCount;
    private final long missingCount;
    private final List<Entry> entries;

    public JournalReviewPage(int year, List<ReportYearOption> yearOptions, boolean closed, String status,
            int pageNumber, int totalPages, long totalCount, long draftCount, long missingCount, List<Entry> entries) {
        this.year = year;
        this.yearOptions = yearOptions;
        this.closed = closed;
        this.status = status;
        this.pageNumber = pageNumber;
        this.totalPages = totalPages;
        this.totalCount = totalCount;
        this.draftCount = draftCount;
        this.missingCount = missingCount;
        this.entries = List.copyOf(entries);
    }

    public int getYear() { return year; }
    public List<ReportYearOption> getYearOptions() { return yearOptions; }
    public boolean isClosed() { return closed; }
    public String getStatus() { return status; }
    public int getPageNumber() { return pageNumber; }
    public int getTotalPages() { return totalPages; }
    public long getTotalCount() { return totalCount; }
    public long getDraftCount() { return draftCount; }
    public long getMissingCount() { return missingCount; }
    public List<Entry> getEntries() { return entries; }

    public record Entry(Long id, LocalDate date, String summary, boolean reviewed, String fingerprint,
            List<Line> lines, String error) {
        public Long getId() { return id; }
        public LocalDate getDate() { return date; }
        public String getSummary() { return summary; }
        public boolean isReviewed() { return reviewed; }
        public List<Line> getLines() { return lines; }
        public String getError() { return error; }
    }

    public record Line(String side, String accountCode, String accountName, BigDecimal amount, String memo) {
        public String getSide() { return side; }
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public BigDecimal getAmount() { return amount; }
        public String getMemo() { return memo; }
    }
}
