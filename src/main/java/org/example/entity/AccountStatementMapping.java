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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "account_statement_mapping", uniqueConstraints = @UniqueConstraint(
        name = "uk_account_statement_mapping", columnNames = {"ledger_account_id", "form_code", "effective_from_year"}))
public class AccountStatementMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ledger_account_id", nullable = false)
    private LedgerAccount ledgerAccount;

    @Column(name = "form_code", nullable = false, length = 50)
    private String formCode;

    @Column(name = "line_code", nullable = false, length = 50)
    private String lineCode;

    @Column(name = "effective_from_year", nullable = false)
    private Integer effectiveFromYear;

    @Column(name = "effective_to_year")
    private Integer effectiveToYear;

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

    public LedgerAccount getLedgerAccount() {
        return ledgerAccount;
    }

    public void setLedgerAccount(LedgerAccount ledgerAccount) {
        this.ledgerAccount = ledgerAccount;
    }

    public String getFormCode() {
        return formCode;
    }

    public void setFormCode(String formCode) {
        this.formCode = formCode;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public Integer getEffectiveFromYear() {
        return effectiveFromYear;
    }

    public void setEffectiveFromYear(Integer effectiveFromYear) {
        this.effectiveFromYear = effectiveFromYear;
    }

    public Integer getEffectiveToYear() {
        return effectiveToYear;
    }

    public void setEffectiveToYear(Integer effectiveToYear) {
        this.effectiveToYear = effectiveToYear;
    }
}
