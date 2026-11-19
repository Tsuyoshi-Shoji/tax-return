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
    private final List<MissingTransaction> missingTransactions;

    public JournalReviewPage(int year, List<ReportYearOption> yearOptions, boolean closed, String status,
            int pageNumber, int totalPages, long totalCount, long draftCount, long missingCount, List<Entry> entries,
            List<MissingTransaction> missingTransactions) {
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
        this.missingTransactions = List.copyOf(missingTransactions);
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
    public List<MissingTransaction> getMissingTransactions() { return missingTransactions; }

    public boolean isHasConfirmable() {
        return !closed && entries.stream().anyMatch(Entry::isConfirmable);
    }

    public record Entry(Long id, LocalDate date, String summary, boolean reviewed, String fingerprint,
            List<Line> lines, String error, String sourceType) {
        public Long getId() { return id; }
        public LocalDate getDate() { return date; }
        public String getSummary() { return summary; }
        public boolean isReviewed() { return reviewed; }
        public List<Line> getLines() { return lines; }
        public String getError() { return error; }
        public String getSourceType() { return sourceType; }
        public boolean isConfirmable() { return !reviewed && error == null; }
    }

    /**
     * 仕訳が未生成の未確認取引。kind は元データの種類（income / expense）、元データがない場合は null。
     */
    public record MissingTransaction(Long transactionId, LocalDate date, String summary, BigDecimal amount,
            String kind, Long entryId) {
        public Long getTransactionId() { return transactionId; }
        public LocalDate getDate() { return date; }
        public String getSummary() { return summary; }
        public BigDecimal getAmount() { return amount; }
        public String getKind() { return kind; }
        public Long getEntryId() { return entryId; }
        public boolean isGeneratable() { return kind != null; }
    }

    public record Line(String side, String accountCode, String accountName, BigDecimal amount, String memo) {
        public String getSide() { return side; }
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public BigDecimal getAmount() { return amount; }
        public String getMemo() { return memo; }
    }
}
