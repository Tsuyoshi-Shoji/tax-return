package org.example.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.Table;

/** 年度の締め（CLOSE）と締めの取消（REOPEN）の履歴。追記のみで更新・削除しない。 */
@Entity
@Table(name = "fiscal_year_closing_event")
public class FiscalYearClosingEvent {
    public static final String CLOSE = "CLOSE";
    public static final String REOPEN = "REOPEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false, updatable = false)
    private FiscalYear fiscalYear;

    @Column(name = "app_user_id", nullable = false, updatable = false)
    private Long appUserId;

    @Column(name = "event_type", nullable = false, length = 10, updatable = false)
    private String eventType;

    @Column(name = "close_version", nullable = false, updatable = false)
    private Integer closeVersion;

    @Column(name = "reason", length = 200, updatable = false)
    private String reason;

    @Column(name = "sales", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal sales;

    @Column(name = "expenses", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal expenses;

    @Column(name = "income", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal income;

    @Column(name = "income_after_deduction", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal incomeAfterDeduction;

    @Column(name = "blue_deduction", nullable = false, updatable = false)
    private Integer blueDeduction;

    @Column(name = "assets_closing", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal assetsClosing;

    @Column(name = "liabilities_closing", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal liabilitiesClosing;

    @Column(name = "journal_line_count", nullable = false, updatable = false)
    private Integer journalLineCount;

    @Column(name = "debit_total", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal debitTotal;

    @Column(name = "credit_total", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal creditTotal;

    @Column(name = "data_fingerprint", nullable = false, length = 64, updatable = false)
    private String dataFingerprint;

    @Column(name = "calc_rule_version", nullable = false, length = 20, updatable = false)
    private String calcRuleVersion;

    @Column(name = "form_edition", nullable = false, length = 40, updatable = false)
    private String formEdition;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public FiscalYear getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(FiscalYear fiscalYear) { this.fiscalYear = fiscalYear; }
    public Long getAppUserId() { return appUserId; }
    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getCloseVersion() { return closeVersion; }
    public void setCloseVersion(Integer closeVersion) { this.closeVersion = closeVersion; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getSales() { return sales; }
    public void setSales(BigDecimal sales) { this.sales = sales; }
    public BigDecimal getExpenses() { return expenses; }
    public void setExpenses(BigDecimal expenses) { this.expenses = expenses; }
    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }
    public BigDecimal getIncomeAfterDeduction() { return incomeAfterDeduction; }
    public void setIncomeAfterDeduction(BigDecimal incomeAfterDeduction) { this.incomeAfterDeduction = incomeAfterDeduction; }
    public Integer getBlueDeduction() { return blueDeduction; }
    public void setBlueDeduction(Integer blueDeduction) { this.blueDeduction = blueDeduction; }
    public BigDecimal getAssetsClosing() { return assetsClosing; }
    public void setAssetsClosing(BigDecimal assetsClosing) { this.assetsClosing = assetsClosing; }
    public BigDecimal getLiabilitiesClosing() { return liabilitiesClosing; }
    public void setLiabilitiesClosing(BigDecimal liabilitiesClosing) { this.liabilitiesClosing = liabilitiesClosing; }
    public Integer getJournalLineCount() { return journalLineCount; }
    public void setJournalLineCount(Integer journalLineCount) { this.journalLineCount = journalLineCount; }
    public BigDecimal getDebitTotal() { return debitTotal; }
    public void setDebitTotal(BigDecimal debitTotal) { this.debitTotal = debitTotal; }
    public BigDecimal getCreditTotal() { return creditTotal; }
    public void setCreditTotal(BigDecimal creditTotal) { this.creditTotal = creditTotal; }
    public String getDataFingerprint() { return dataFingerprint; }
    public void setDataFingerprint(String dataFingerprint) { this.dataFingerprint = dataFingerprint; }
    public String getCalcRuleVersion() { return calcRuleVersion; }
    public void setCalcRuleVersion(String calcRuleVersion) { this.calcRuleVersion = calcRuleVersion; }
    public String getFormEdition() { return formEdition; }
    public void setFormEdition(String formEdition) { this.formEdition = formEdition; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
