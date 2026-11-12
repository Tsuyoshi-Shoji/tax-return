package org.example.features.taxreturn.retention;

import java.util.List;

import org.example.features.taxreturn.evidence.EvidenceFileService.EvidenceDriveStatus;

public class RetentionPageData {
    private final List<RetentionRecordView> records;
    private final List<RetentionJournalOption> journalEntries;
    private final RetentionSearch search;
    private final int totalCount;
    private final int expiredCount;
    private final int fileAlertCount;
    private EvidenceDriveStatus driveStatus = EvidenceDriveStatus.disabled();
    private int dueSoonCount;
    private int heldCount;
    private int manualRuleCount;
    private boolean blueFiling = true;
    private boolean ruleYearSupported = true;
    private java.time.LocalDate minimumDeadline;

    public List<RetentionRules.Category> getRuleCategories() { return RetentionRules.categories(); }

    public String getRuleCheckedOn() { return RetentionRules.CHECKED_ON; }

    public int getDueSoonDays() { return RetentionRules.DUE_SOON_DAYS; }

    public int getDueSoonCount() { return dueSoonCount; }

    public int getHeldCount() { return heldCount; }

    public int getManualRuleCount() { return manualRuleCount; }

    public boolean isBlueFiling() { return blueFiling; }

    public boolean isRuleYearSupported() { return ruleYearSupported; }

    public java.time.LocalDate getMinimumDeadline() { return minimumDeadline; }

    void setGuidance(int dueSoonCount, int heldCount, int manualRuleCount, boolean blueFiling,
            boolean ruleYearSupported, java.time.LocalDate minimumDeadline) {
        this.dueSoonCount = dueSoonCount;
        this.heldCount = heldCount;
        this.manualRuleCount = manualRuleCount;
        this.blueFiling = blueFiling;
        this.ruleYearSupported = ruleYearSupported;
        this.minimumDeadline = minimumDeadline;
    }

    public RetentionPageData(List<RetentionRecordView> records, List<RetentionJournalOption> journalEntries) {
        this(records, journalEntries, RetentionSearch.all(), records.size(), 0, 0);
    }

    public RetentionPageData(List<RetentionRecordView> records, List<RetentionJournalOption> journalEntries,
            RetentionSearch search, int totalCount, int expiredCount, int fileAlertCount) {
        this.records = List.copyOf(records);
        this.journalEntries = List.copyOf(journalEntries);
        this.search = search;
        this.totalCount = totalCount;
        this.expiredCount = expiredCount;
        this.fileAlertCount = fileAlertCount;
    }

    public List<RetentionRecordView> getRecords() {
        return records;
    }

    public List<RetentionJournalOption> getJournalEntries() {
        return journalEntries;
    }

    public RetentionSearch getSearch() {
        return search;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getExpiredCount() {
        return expiredCount;
    }

    public int getFileAlertCount() {
        return fileAlertCount;
    }

    public EvidenceDriveStatus getDriveStatus() {
        return driveStatus;
    }

    public void setDriveStatus(EvidenceDriveStatus driveStatus) {
        this.driveStatus = driveStatus;
    }
}
