package org.example.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "expense_entry")
public class ExpenseEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @ManyToOne
    @JoinColumn(name = "expense_item_id")
    private ExpenseItem expenseItem;

    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @OneToOne
    @JoinColumn(name = "accounting_transaction_id", unique = true)
    private AccountingTransaction accountingTransaction;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "exclude_from_return", nullable = false)
    private boolean excludeFromReturn;

    @Column(name = "deductible_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal deductibleAmount;

    @Column(name = "home_apportionment_applied", nullable = false)
    private boolean homeApportionmentApplied;

    @Column(name = "home_apportionment_rate", precision = 5, scale = 2)
    private BigDecimal homeApportionmentRate;

    @Column(name = "details")
    private String details;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    public Long getId() {
        return id;
    }

    public FiscalYear getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(FiscalYear fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public ExpenseItem getExpenseItem() {
        return expenseItem;
    }

    public void setExpenseItem(ExpenseItem expenseItem) {
        this.expenseItem = expenseItem;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public AccountingTransaction getAccountingTransaction() {
        return accountingTransaction;
    }

    public void setAccountingTransaction(AccountingTransaction accountingTransaction) {
        this.accountingTransaction = accountingTransaction;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isExcludeFromReturn() {
        return excludeFromReturn;
    }

    public void setExcludeFromReturn(boolean excludeFromReturn) {
        this.excludeFromReturn = excludeFromReturn;
    }

    public BigDecimal getDeductibleAmount() {
        return deductibleAmount;
    }

    public void setDeductibleAmount(BigDecimal deductibleAmount) {
        this.deductibleAmount = deductibleAmount;
    }

    public boolean isHomeApportionmentApplied() {
        return homeApportionmentApplied;
    }

    public void setHomeApportionmentApplied(boolean homeApportionmentApplied) {
        this.homeApportionmentApplied = homeApportionmentApplied;
    }

    public BigDecimal getHomeApportionmentRate() {
        return homeApportionmentRate;
    }

    public void setHomeApportionmentRate(BigDecimal homeApportionmentRate) {
        this.homeApportionmentRate = homeApportionmentRate;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void markDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
