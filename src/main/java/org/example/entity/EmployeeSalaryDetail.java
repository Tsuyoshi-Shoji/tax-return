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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "employee_salary_detail")
public class EmployeeSalaryDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Column(name = "salary_kind", nullable = false, length = 20)
    private String salaryKind;

    @Column(name = "person_name", nullable = false, length = 100)
    private String personName;

    @Column(name = "job_description", nullable = false, length = 100)
    private String jobDescription;

    @Column(name = "work_period_start", nullable = false)
    private LocalDate workPeriodStart;

    @Column(name = "work_period_end", nullable = false)
    private LocalDate workPeriodEnd;

    @Column(name = "payment_period", nullable = false, length = 30)
    private String paymentPeriod;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

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
    public String getSalaryKind() { return salaryKind; }
    public void setSalaryKind(String salaryKind) { this.salaryKind = salaryKind; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }
    public LocalDate getWorkPeriodStart() { return workPeriodStart; }
    public void setWorkPeriodStart(LocalDate workPeriodStart) { this.workPeriodStart = workPeriodStart; }
    public LocalDate getWorkPeriodEnd() { return workPeriodEnd; }
    public void setWorkPeriodEnd(LocalDate workPeriodEnd) { this.workPeriodEnd = workPeriodEnd; }
    public String getPaymentPeriod() { return paymentPeriod; }
    public void setPaymentPeriod(String paymentPeriod) { this.paymentPeriod = paymentPeriod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
