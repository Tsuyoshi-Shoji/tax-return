package org.example.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tax_deduction_record")
public class TaxDeductionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_transaction_id", unique = true)
    private AccountingTransaction sourceTransaction;

    @Column(name = "deduction_type", nullable = false, length = 60)
    private String deductionType;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payer", length = 100)
    private String payer;

    @Column(name = "insured_person_or_beneficiary", length = 100)
    private String insuredPersonOrBeneficiary;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "eligible_amount", precision = 15, scale = 2)
    private BigDecimal eligibleAmount;

    @Column(name = "calculated_deduction_amount", precision = 15, scale = 2)
    private BigDecimal calculatedDeductionAmount;

    @Column(name = "evidence_id", length = 120)
    private String evidenceId;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "applied_return_id")
    private Long appliedReturnId;

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

    public AccountingTransaction getSourceTransaction() {
        return sourceTransaction;
    }

    public void setSourceTransaction(AccountingTransaction sourceTransaction) {
        this.sourceTransaction = sourceTransaction;
    }

    public String getDeductionType() {
        return deductionType;
    }

    public void setDeductionType(String deductionType) {
        this.deductionType = deductionType;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getInsuredPersonOrBeneficiary() {
        return insuredPersonOrBeneficiary;
    }

    public void setInsuredPersonOrBeneficiary(String insuredPersonOrBeneficiary) {
        this.insuredPersonOrBeneficiary = insuredPersonOrBeneficiary;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getEligibleAmount() {
        return eligibleAmount;
    }

    public void setEligibleAmount(BigDecimal eligibleAmount) {
        this.eligibleAmount = eligibleAmount;
    }

    public BigDecimal getCalculatedDeductionAmount() {
        return calculatedDeductionAmount;
    }

    public void setCalculatedDeductionAmount(BigDecimal calculatedDeductionAmount) {
        this.calculatedDeductionAmount = calculatedDeductionAmount;
    }

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getAppliedReturnId() {
        return appliedReturnId;
    }

    public void setAppliedReturnId(Long appliedReturnId) {
        this.appliedReturnId = appliedReturnId;
    }
}

