package org.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "retention_record")
public class RetentionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(name = "record_kind", nullable = false, length = 20)
    private String recordKind;

    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "retention_start_date", nullable = false)
    private LocalDate retentionStartDate;

    @Column(name = "retention_deadline", nullable = false)
    private LocalDate retentionDeadline;

    @Column(name = "legal_basis", nullable = false, length = 500)
    private String legalBasis;

    @Column(name = "retention_rule_code", length = 30)
    private String retentionRuleCode;

    @Column(name = "rule_version", length = 20)
    private String ruleVersion;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Column(name = "consumption_tax_applies", nullable = false)
    private boolean consumptionTaxApplies;

    @Column(name = "legal_hold", nullable = false)
    private boolean legalHold;

    @Column(name = "legal_hold_reason", length = 200)
    private String legalHoldReason;

    @Column(name = "legal_hold_at")
    private LocalDateTime legalHoldAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public FiscalYear getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(FiscalYear fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public String getRecordKind() {
        return recordKind;
    }

    public void setRecordKind(String recordKind) {
        this.recordKind = recordKind;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getRetentionStartDate() {
        return retentionStartDate;
    }

    public void setRetentionStartDate(LocalDate retentionStartDate) {
        this.retentionStartDate = retentionStartDate;
    }

    public LocalDate getRetentionDeadline() {
        return retentionDeadline;
    }

    public void setRetentionDeadline(LocalDate retentionDeadline) {
        this.retentionDeadline = retentionDeadline;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public String getRetentionRuleCode() { return retentionRuleCode; }

    public void setRetentionRuleCode(String retentionRuleCode) { this.retentionRuleCode = retentionRuleCode; }

    public String getRuleVersion() { return ruleVersion; }

    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public LocalDate getDocumentDate() { return documentDate; }

    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }

    public boolean isConsumptionTaxApplies() { return consumptionTaxApplies; }

    public void setConsumptionTaxApplies(boolean consumptionTaxApplies) {
        this.consumptionTaxApplies = consumptionTaxApplies;
    }

    public boolean isLegalHold() { return legalHold; }

    public void setLegalHold(boolean legalHold) { this.legalHold = legalHold; }

    public String getLegalHoldReason() { return legalHoldReason; }

    public void setLegalHoldReason(String legalHoldReason) { this.legalHoldReason = legalHoldReason; }

    public LocalDateTime getLegalHoldAt() { return legalHoldAt; }

    public void setLegalHoldAt(LocalDateTime legalHoldAt) { this.legalHoldAt = legalHoldAt; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
