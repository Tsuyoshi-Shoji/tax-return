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

/** 年分ごとの扶養親族（12月31日現在の状況）。 */
@Entity
@Table(name = "tax_dependent")
public class TaxDependent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Column(name = "dependent_name", nullable = false, length = 100)
    private String dependentName;

    @Column(name = "relationship", nullable = false, length = 30)
    private String relationship;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "income", nullable = false, precision = 15, scale = 0)
    private BigDecimal income = BigDecimal.ZERO;

    @Column(name = "cohabiting", nullable = false)
    private boolean cohabiting = true;

    /** NONE / GENERAL / SPECIAL */
    @Column(name = "disability", nullable = false, length = 20)
    private String disability = "NONE";

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
    public String getDependentName() { return dependentName; }
    public void setDependentName(String dependentName) { this.dependentName = dependentName; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }
    public boolean isCohabiting() { return cohabiting; }
    public void setCohabiting(boolean cohabiting) { this.cohabiting = cohabiting; }
    public String getDisability() { return disability; }
    public void setDisability(String disability) { this.disability = disability; }
}
