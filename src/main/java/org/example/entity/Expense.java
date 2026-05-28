package org.example.entity;

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
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory expenseCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_subcategory_id")
    private ExpenseSubcategory expenseSubcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "memo")
    private String memo;

    @Column(name = "is_business_target")
    private Boolean businessTarget;

    @Column(name = "deductible_amount", precision = 15, scale = 0)
    private BigDecimal deductibleAmount;

    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (deletedFlag == null) {
            deletedFlag = Boolean.FALSE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public ExpenseCategory getExpenseCategory() { return expenseCategory; }
    public void setExpenseCategory(ExpenseCategory expenseCategory) { this.expenseCategory = expenseCategory; }
    public ExpenseSubcategory getExpenseSubcategory() { return expenseSubcategory; }
    public void setExpenseSubcategory(ExpenseSubcategory expenseSubcategory) { this.expenseSubcategory = expenseSubcategory; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public Boolean getBusinessTarget() { return businessTarget; }
    public void setBusinessTarget(Boolean businessTarget) { this.businessTarget = businessTarget; }
    public BigDecimal getDeductibleAmount() { return deductibleAmount; }
    public void setDeductibleAmount(BigDecimal deductibleAmount) { this.deductibleAmount = deductibleAmount; }
    public Boolean getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Boolean deletedFlag) { this.deletedFlag = deletedFlag; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

