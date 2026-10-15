package org.example.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "settlement_adjustment")
public class SettlementAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "fiscal_year_id", nullable = false, unique = true)
    private FiscalYear fiscalYear;

    @Column(name = "opening_inventory", nullable = false, precision = 15, scale = 0)
    private BigDecimal openingInventory = BigDecimal.ZERO;

    @Column(name = "closing_inventory", nullable = false, precision = 15, scale = 0)
    private BigDecimal closingInventory = BigDecimal.ZERO;

    @Column(name = "depreciation", nullable = false, precision = 15, scale = 0)
    private BigDecimal depreciation = BigDecimal.ZERO;

    @Column(name = "accounts_receivable", nullable = false, precision = 15, scale = 0)
    private BigDecimal accountsReceivable = BigDecimal.ZERO;

    @Column(name = "accounts_payable", nullable = false, precision = 15, scale = 0)
    private BigDecimal accountsPayable = BigDecimal.ZERO;

    @Column(name = "unpaid_expenses", nullable = false, precision = 15, scale = 0)
    private BigDecimal unpaidExpenses = BigDecimal.ZERO;

    @Column(name = "unreceived_income", nullable = false, precision = 15, scale = 0)
    private BigDecimal unreceivedIncome = BigDecimal.ZERO;

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 0)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_cash", nullable = false, precision = 15, scale = 0)
    private BigDecimal closingCash = BigDecimal.ZERO;

    @Column(name = "blue_deduction", nullable = false)
    private int blueDeduction = 65;

    @Column(name = "settlement_status", length = 20)
    private String settlementStatus = "UNSETTLED";

    @Version
    @Column(name = "version")
    private Long version;

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

    public Long getId() { return id; }
    public FiscalYear getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(FiscalYear fiscalYear) { this.fiscalYear = fiscalYear; }
    public BigDecimal getOpeningInventory() { return openingInventory; }
    public void setOpeningInventory(BigDecimal v) { this.openingInventory = v; }
    public BigDecimal getClosingInventory() { return closingInventory; }
    public void setClosingInventory(BigDecimal v) { this.closingInventory = v; }
    public BigDecimal getDepreciation() { return depreciation; }
    public void setDepreciation(BigDecimal v) { this.depreciation = v; }
    public BigDecimal getAccountsReceivable() { return accountsReceivable; }
    public void setAccountsReceivable(BigDecimal v) { this.accountsReceivable = v; }
    public BigDecimal getAccountsPayable() { return accountsPayable; }
    public void setAccountsPayable(BigDecimal v) { this.accountsPayable = v; }
    public BigDecimal getUnpaidExpenses() { return unpaidExpenses; }
    public void setUnpaidExpenses(BigDecimal v) { this.unpaidExpenses = v; }
    public BigDecimal getUnreceivedIncome() { return unreceivedIncome; }
    public void setUnreceivedIncome(BigDecimal v) { this.unreceivedIncome = v; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal v) { this.openingBalance = v; }
    public BigDecimal getClosingCash() { return closingCash; }
    public void setClosingCash(BigDecimal v) { this.closingCash = v; }
    public int getBlueDeduction() { return blueDeduction; }
    public void setBlueDeduction(int blueDeduction) { this.blueDeduction = blueDeduction; }
    public String getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(String settlementStatus) { this.settlementStatus = settlementStatus; }
    public Long getVersion() { return version; }
}
