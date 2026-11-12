package org.example.features.taxreturn.retention;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RetentionRecordView {
    private final Long id;
    private final String recordKind;
    private final String documentType;
    private final String title;
    private final Long journalEntryId;
    private final String journalSummary;
    private final LocalDate retentionStartDate;
    private final LocalDate retentionDeadline;
    private final String legalBasis;
    private final boolean expired;
    private final FileInfo file;
    private final Guidance guidance;

    public RetentionRecordView(Long id, String recordKind, String documentType, String title,
            Long journalEntryId, String journalSummary, LocalDate retentionStartDate,
            LocalDate retentionDeadline, String legalBasis) {
        this(id, recordKind, documentType, title, journalEntryId, journalSummary, retentionStartDate,
                retentionDeadline, legalBasis, false, null);
    }

    public RetentionRecordView(Long id, String recordKind, String documentType, String title,
            Long journalEntryId, String journalSummary, LocalDate retentionStartDate,
            LocalDate retentionDeadline, String legalBasis, boolean expired, FileInfo file) {
        this(id, recordKind, documentType, title, journalEntryId, journalSummary, retentionStartDate,
                retentionDeadline, legalBasis, expired, file, null);
    }

    public RetentionRecordView(Long id, String recordKind, String documentType, String title,
            Long journalEntryId, String journalSummary, LocalDate retentionStartDate,
            LocalDate retentionDeadline, String legalBasis, boolean expired, FileInfo file, Guidance guidance) {
        this.id = id;
        this.recordKind = recordKind;
        this.documentType = documentType;
        this.title = title;
        this.journalEntryId = journalEntryId;
        this.journalSummary = journalSummary;
        this.retentionStartDate = retentionStartDate;
        this.retentionDeadline = retentionDeadline;
        this.legalBasis = legalBasis;
        this.expired = expired;
        this.file = file;
        this.guidance = guidance != null ? guidance
                : new Guidance(null, null, false, false, null, null, retentionDeadline, 0);
    }

    public Long getId() {
        return id;
    }

    public String getRecordKind() {
        return recordKind;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getTitle() {
        return title;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public String getJournalSummary() {
        return journalSummary;
    }

    public LocalDate getRetentionStartDate() {
        return retentionStartDate;
    }

    public LocalDate getRetentionDeadline() {
        return retentionDeadline;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    /** 保存期限を経過し、削除可能な状態か。 */
    public boolean isExpired() {
        return expired;
    }

    public FileInfo getFile() {
        return file;
    }

    public boolean isHasFile() {
        return file != null;
    }

    public Guidance getGuidance() {
        return guidance;
    }

    /** 期限経過かつ保全中でなく、削除できる状態か。 */
    public boolean isDeletable() {
        return expired && !guidance.legalHold();
    }

    /** 期限案内の状態：HELD（保全中）／EXPIRED（期限経過）／DUE_SOON（期限間近）／ACTIVE（保存期間中）。 */
    public String getDeadlineStatus() {
        if (guidance.legalHold()) {
            return "HELD";
        }
        if (expired) {
            return "EXPIRED";
        }
        return guidance.daysRemaining() <= RetentionRules.DUE_SOON_DAYS ? "DUE_SOON" : "ACTIVE";
    }

    public record Guidance(String ruleCode, LocalDate documentDate, boolean consumptionTaxApplies,
            boolean legalHold, String legalHoldReason, LocalDateTime legalHoldAt, LocalDate effectiveDeadline,
            long daysRemaining) {
        public String getRuleCode() { return ruleCode; }

        public String getRuleLabel() { return RetentionRules.label(ruleCode); }

        public boolean isRuleApplied() { return ruleCode != null && !RetentionRules.MANUAL.equals(ruleCode); }

        public LocalDate getDocumentDate() { return documentDate; }

        public boolean isConsumptionTaxApplies() { return consumptionTaxApplies; }

        public boolean isLegalHold() { return legalHold; }

        public String getLegalHoldReason() { return legalHoldReason; }

        public LocalDateTime getLegalHoldAt() { return legalHoldAt; }

        public LocalDate getEffectiveDeadline() { return effectiveDeadline; }

        public long getDaysRemaining() { return daysRemaining; }
    }

    public record FileInfo(String originalFileName, String mimeType, long size, LocalDateTime uploadedAt,
            String verificationStatus, LocalDateTime lastVerifiedAt) {
        public String getOriginalFileName() {
            return originalFileName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getSize() {
            return size;
        }

        public String getSizeLabel() {
            return size >= 1024 * 1024 ? String.format("%.1f MB", size / 1024.0 / 1024.0)
                    : Math.max(1, size / 1024) + " KB";
        }

        public LocalDateTime getUploadedAt() {
            return uploadedAt;
        }

        public String getVerificationStatus() {
            return verificationStatus;
        }

        public LocalDateTime getLastVerifiedAt() {
            return lastVerifiedAt;
        }

        public boolean isAlert() {
            return "MISSING".equals(verificationStatus) || "TRASHED".equals(verificationStatus)
                    || "MISMATCH".equals(verificationStatus);
        }
    }
}
