package org.example.entity;

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

/** 出力帳票の生成記録。帳票本体は保存せず、生成時点・計算ルール版・様式版・ハッシュのみを残す。 */
@Entity
@Table(name = "report_output_record")
public class ReportOutputRecord {
    public static final String CURRENT = "CURRENT";
    public static final String SUPERSEDED = "SUPERSEDED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false, updatable = false)
    private FiscalYear fiscalYear;

    @Column(name = "app_user_id", nullable = false, updatable = false)
    private Long appUserId;

    @Column(name = "output_type", nullable = false, length = 30, updatable = false)
    private String outputType;

    @Column(name = "file_name", nullable = false, length = 120, updatable = false)
    private String fileName;

    @Column(name = "file_sha256", nullable = false, length = 64, updatable = false)
    private String fileSha256;

    @Column(name = "file_size", nullable = false, updatable = false)
    private Long fileSize;

    @Column(name = "data_fingerprint", nullable = false, length = 64, updatable = false)
    private String dataFingerprint;

    @Column(name = "calc_rule_version", nullable = false, length = 20, updatable = false)
    private String calcRuleVersion;

    @Column(name = "form_edition", nullable = false, length = 40, updatable = false)
    private String formEdition;

    @Column(name = "year_status", nullable = false, length = 10, updatable = false)
    private String yearStatus;

    @Column(name = "close_version", updatable = false)
    private Integer closeVersion;

    @Column(name = "record_status", nullable = false, length = 20)
    private String recordStatus;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
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

    public Long getId() { return id; }
    public FiscalYear getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(FiscalYear fiscalYear) { this.fiscalYear = fiscalYear; }
    public Long getAppUserId() { return appUserId; }
    public void setAppUserId(Long appUserId) { this.appUserId = appUserId; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileSha256() { return fileSha256; }
    public void setFileSha256(String fileSha256) { this.fileSha256 = fileSha256; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getDataFingerprint() { return dataFingerprint; }
    public void setDataFingerprint(String dataFingerprint) { this.dataFingerprint = dataFingerprint; }
    public String getCalcRuleVersion() { return calcRuleVersion; }
    public void setCalcRuleVersion(String calcRuleVersion) { this.calcRuleVersion = calcRuleVersion; }
    public String getFormEdition() { return formEdition; }
    public void setFormEdition(String formEdition) { this.formEdition = formEdition; }
    public String getYearStatus() { return yearStatus; }
    public void setYearStatus(String yearStatus) { this.yearStatus = yearStatus; }
    public Integer getCloseVersion() { return closeVersion; }
    public void setCloseVersion(Integer closeVersion) { this.closeVersion = closeVersion; }
    public String getRecordStatus() { return recordStatus; }
    public void setRecordStatus(String recordStatus) { this.recordStatus = recordStatus; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
