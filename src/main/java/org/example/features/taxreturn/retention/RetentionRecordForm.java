package org.example.features.taxreturn.retention;

public class RetentionRecordForm {
    private String recordKind;
    private String documentType;
    private String title;
    private Long journalEntryId;
    private String retentionStartDate;
    private String retentionDeadline;
    private String legalBasis;
    private String ruleCode;
    private String documentDate;
    private boolean consumptionTax;
    private String csrfToken;

    /** 保存期限ルールの区分。未指定は手入力（MANUAL）として扱う。 */
    public String getRuleCode() { return ruleCode; }

    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getDocumentDate() { return documentDate; }

    public void setDocumentDate(String documentDate) { this.documentDate = documentDate; }

    public boolean isConsumptionTax() { return consumptionTax; }

    public void setConsumptionTax(boolean consumptionTax) { this.consumptionTax = consumptionTax; }

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

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public String getRetentionStartDate() {
        return retentionStartDate;
    }

    public void setRetentionStartDate(String retentionStartDate) {
        this.retentionStartDate = retentionStartDate;
    }

    public String getRetentionDeadline() {
        return retentionDeadline;
    }

    public void setRetentionDeadline(String retentionDeadline) {
        this.retentionDeadline = retentionDeadline;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
}
