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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** 年分ごとの確定申告用入力（事業所得以外の所得、本人・配偶者の状況、予定納税額、消費税の基準期間）。 */
@Entity
@Table(name = "tax_return_input")
public class TaxReturnInput {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", nullable = false, unique = true)
    private FiscalYear fiscalYear;

    @Column(name = "salary_revenue", nullable = false, precision = 15, scale = 0)
    private BigDecimal salaryRevenue = BigDecimal.ZERO;

    @Column(name = "salary_withholding", nullable = false, precision = 15, scale = 0)
    private BigDecimal salaryWithholding = BigDecimal.ZERO;

    @Column(name = "salary_payer_name", length = 100)
    private String salaryPayerName;

    @Column(name = "salary_payer_address", length = 255)
    private String salaryPayerAddress;

    @Column(name = "misc_revenue", nullable = false, precision = 15, scale = 0)
    private BigDecimal miscRevenue = BigDecimal.ZERO;

    @Column(name = "misc_expense", nullable = false, precision = 15, scale = 0)
    private BigDecimal miscExpense = BigDecimal.ZERO;

    @Column(name = "misc_withholding", nullable = false, precision = 15, scale = 0)
    private BigDecimal miscWithholding = BigDecimal.ZERO;

    @Column(name = "misc_payer_name", length = 100)
    private String miscPayerName;

    @Column(name = "prepaid_tax", nullable = false, precision = 15, scale = 0)
    private BigDecimal prepaidTax = BigDecimal.ZERO;

    /** NONE / GENERAL / SPECIAL */
    @Column(name = "self_disability", nullable = false, length = 20)
    private String selfDisability = "NONE";

    /** NONE / WIDOW / SINGLE_PARENT */
    @Column(name = "widow_status", nullable = false, length = 20)
    private String widowStatus = "NONE";

    @Column(name = "working_student", nullable = false)
    private boolean workingStudent;

    @Column(name = "has_spouse", nullable = false)
    private boolean hasSpouse;

    @Column(name = "spouse_name", length = 100)
    private String spouseName;

    @Column(name = "spouse_birth_date")
    private LocalDate spouseBirthDate;

    @Column(name = "spouse_income", nullable = false, precision = 15, scale = 0)
    private BigDecimal spouseIncome = BigDecimal.ZERO;

    /** NONE / GENERAL / SPECIAL */
    @Column(name = "spouse_disability", nullable = false, length = 20)
    private String spouseDisability = "NONE";

    @Column(name = "spouse_cohabiting", nullable = false)
    private boolean spouseCohabiting = true;

    @Column(name = "base_period_taxable_sales", precision = 15, scale = 0)
    private BigDecimal basePeriodTaxableSales;

    @Column(name = "depreciation_rule_version", length = 40)
    private String depreciationRuleVersion;

    @Column(name = "depreciation_method_basis", nullable = false, length = 20)
    private String depreciationMethodBasis = "UNCONFIRMED";

    @Column(name = "depreciation_special_confirmed", nullable = false)
    private boolean depreciationSpecialConfirmed;

    @Column(name = "depreciation_business_months", nullable = false)
    private int depreciationBusinessMonths = 12;

    @Column(name = "depreciation_evidence", length = 500)
    private String depreciationEvidence;

    @Column(name = "depreciation_confirmed_at")
    private LocalDateTime depreciationConfirmedAt;

    public String getDepreciationRuleVersion() { return depreciationRuleVersion; }
    public void setDepreciationRuleVersion(String v) { depreciationRuleVersion = v; }
    public String getDepreciationMethodBasis() { return depreciationMethodBasis; }
    public void setDepreciationMethodBasis(String v) { depreciationMethodBasis = v; }
    public boolean isDepreciationSpecialConfirmed() { return depreciationSpecialConfirmed; }
    public void setDepreciationSpecialConfirmed(boolean v) { depreciationSpecialConfirmed = v; }
    public int getDepreciationBusinessMonths() { return depreciationBusinessMonths; }
    public void setDepreciationBusinessMonths(int v) { depreciationBusinessMonths = v; }
    public String getDepreciationEvidence() { return depreciationEvidence; }
    public void setDepreciationEvidence(String v) { depreciationEvidence = v; }
    public LocalDateTime getDepreciationConfirmedAt() { return depreciationConfirmedAt; }
    public void setDepreciationConfirmedAt(LocalDateTime v) { depreciationConfirmedAt = v; }

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
    public BigDecimal getSalaryRevenue() { return salaryRevenue; }
    public void setSalaryRevenue(BigDecimal salaryRevenue) { this.salaryRevenue = salaryRevenue; }
    public BigDecimal getSalaryWithholding() { return salaryWithholding; }
    public void setSalaryWithholding(BigDecimal salaryWithholding) { this.salaryWithholding = salaryWithholding; }
    public String getSalaryPayerName() { return salaryPayerName; }
    public void setSalaryPayerName(String salaryPayerName) { this.salaryPayerName = salaryPayerName; }
    public String getSalaryPayerAddress() { return salaryPayerAddress; }
    public void setSalaryPayerAddress(String salaryPayerAddress) { this.salaryPayerAddress = salaryPayerAddress; }
    public BigDecimal getMiscRevenue() { return miscRevenue; }
    public void setMiscRevenue(BigDecimal miscRevenue) { this.miscRevenue = miscRevenue; }
    public BigDecimal getMiscExpense() { return miscExpense; }
    public void setMiscExpense(BigDecimal miscExpense) { this.miscExpense = miscExpense; }
    public BigDecimal getMiscWithholding() { return miscWithholding; }
    public void setMiscWithholding(BigDecimal miscWithholding) { this.miscWithholding = miscWithholding; }
    public String getMiscPayerName() { return miscPayerName; }
    public void setMiscPayerName(String miscPayerName) { this.miscPayerName = miscPayerName; }
    public BigDecimal getPrepaidTax() { return prepaidTax; }
    public void setPrepaidTax(BigDecimal prepaidTax) { this.prepaidTax = prepaidTax; }
    public String getSelfDisability() { return selfDisability; }
    public void setSelfDisability(String selfDisability) { this.selfDisability = selfDisability; }
    public String getWidowStatus() { return widowStatus; }
    public void setWidowStatus(String widowStatus) { this.widowStatus = widowStatus; }
    public boolean isWorkingStudent() { return workingStudent; }
    public void setWorkingStudent(boolean workingStudent) { this.workingStudent = workingStudent; }
    public boolean isHasSpouse() { return hasSpouse; }
    public void setHasSpouse(boolean hasSpouse) { this.hasSpouse = hasSpouse; }
    public String getSpouseName() { return spouseName; }
    public void setSpouseName(String spouseName) { this.spouseName = spouseName; }
    public LocalDate getSpouseBirthDate() { return spouseBirthDate; }
    public void setSpouseBirthDate(LocalDate spouseBirthDate) { this.spouseBirthDate = spouseBirthDate; }
    public BigDecimal getSpouseIncome() { return spouseIncome; }
    public void setSpouseIncome(BigDecimal spouseIncome) { this.spouseIncome = spouseIncome; }
    public String getSpouseDisability() { return spouseDisability; }
    public void setSpouseDisability(String spouseDisability) { this.spouseDisability = spouseDisability; }
    public boolean isSpouseCohabiting() { return spouseCohabiting; }
    public void setSpouseCohabiting(boolean spouseCohabiting) { this.spouseCohabiting = spouseCohabiting; }
    public BigDecimal getBasePeriodTaxableSales() { return basePeriodTaxableSales; }
    public void setBasePeriodTaxableSales(BigDecimal basePeriodTaxableSales) { this.basePeriodTaxableSales = basePeriodTaxableSales; }
    public Long getVersion() { return version; }
}
