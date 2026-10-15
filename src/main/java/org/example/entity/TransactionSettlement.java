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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_settlement")
public class TransactionSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_transaction_id", nullable = false)
    private AccountingTransaction sourceTransaction;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_adjustment_id", nullable = false)
    private SettlementAdjustment settlementAdjustment;

    @Column(name = "settled_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal settledAmount;

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

    public AccountingTransaction getSourceTransaction() {
        return sourceTransaction;
    }

    public void setSourceTransaction(AccountingTransaction sourceTransaction) {
        this.sourceTransaction = sourceTransaction;
    }

    public SettlementAdjustment getSettlementAdjustment() {
        return settlementAdjustment;
    }

    public void setSettlementAdjustment(SettlementAdjustment settlementAdjustment) {
        this.settlementAdjustment = settlementAdjustment;
    }

    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(BigDecimal settledAmount) {
        this.settledAmount = settledAmount;
    }
}

