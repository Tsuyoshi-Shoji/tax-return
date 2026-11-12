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
@Table(name = "fixed_asset")
public class FixedAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "business_profile_id", nullable = false)
    private BusinessProfile businessProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_asset_id")
    private FixedAsset parentAsset;

    @Column(name = "asset_origin", nullable = false, length = 30)
    private String assetOrigin = "ACQUISITION";

    @Column(name = "asset_name", nullable = false, length = 100)
    private String assetName;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "service_start_date", nullable = false)
    private LocalDate serviceStartDate;

    @Column(name = "acquisition_cost", nullable = false, precision = 15, scale = 0)
    private BigDecimal acquisitionCost;

    @Column(name = "useful_life", nullable = false)
    private Integer usefulLife;

    /** 償却方法コードは DepreciationCalculator で管理。 */
    @Column(name = "depreciation_method", nullable = false, length = 30)
    private String depreciationMethod;

    @Column(name = "life_basis", nullable = false, length = 30)
    private String lifeBasis = "STATUTORY";

    @Column(name = "statutory_useful_life")
    private Integer statutoryUsefulLife;

    @Column(name = "used_elapsed_months")
    private Integer usedElapsedMonths;

    @Column(name = "used_requirements_confirmed", nullable = false)
    private boolean usedRequirementsConfirmed;

    @Column(name = "private_use_conversion", nullable = false)
    private boolean privateUseConversion;

    @Column(name = "rate_transition_confirmed", nullable = false)
    private boolean rateTransitionConfirmed;

    @Column(name = "business_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal businessRatio;

    /** EXPENSE_ENTRY（支出登録の固定資産購入で支払済み）/ OWNER_FUNDS（私的資金で購入） */
    @Column(name = "acquisition_funding", nullable = false, length = 30)
    private String acquisitionFunding;

    @Column(name = "notes", length = 255)
    private String notes;

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
    public BusinessProfile getBusinessProfile() { return businessProfile; }
    public void setBusinessProfile(BusinessProfile businessProfile) { this.businessProfile = businessProfile; }
    public FixedAsset getParentAsset() { return parentAsset; }
    public void setParentAsset(FixedAsset parentAsset) { this.parentAsset = parentAsset; }
    public String getAssetOrigin() { return assetOrigin; }
    public void setAssetOrigin(String assetOrigin) { this.assetOrigin = assetOrigin; }
    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }
    public LocalDate getServiceStartDate() { return serviceStartDate; }
    public void setServiceStartDate(LocalDate serviceStartDate) { this.serviceStartDate = serviceStartDate; }
    public BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public void setAcquisitionCost(BigDecimal acquisitionCost) { this.acquisitionCost = acquisitionCost; }
    public Integer getUsefulLife() { return usefulLife; }
    public void setUsefulLife(Integer usefulLife) { this.usefulLife = usefulLife; }
    public String getDepreciationMethod() { return depreciationMethod; }
    public void setDepreciationMethod(String depreciationMethod) { this.depreciationMethod = depreciationMethod; }
    public String getLifeBasis() { return lifeBasis; }
    public void setLifeBasis(String v) { lifeBasis = v; }
    public Integer getStatutoryUsefulLife() { return statutoryUsefulLife; }
    public void setStatutoryUsefulLife(Integer v) { statutoryUsefulLife = v; }
    public Integer getUsedElapsedMonths() { return usedElapsedMonths; }
    public void setUsedElapsedMonths(Integer v) { usedElapsedMonths = v; }
    public boolean isUsedRequirementsConfirmed() { return usedRequirementsConfirmed; }
    public void setUsedRequirementsConfirmed(boolean v) { usedRequirementsConfirmed = v; }
    public boolean isPrivateUseConversion() { return privateUseConversion; }
    public void setPrivateUseConversion(boolean v) { privateUseConversion = v; }
    public boolean isRateTransitionConfirmed() { return rateTransitionConfirmed; }
    public void setRateTransitionConfirmed(boolean v) { rateTransitionConfirmed = v; }
    public BigDecimal getBusinessRatio() { return businessRatio; }
    public void setBusinessRatio(BigDecimal businessRatio) { this.businessRatio = businessRatio; }
    public String getAcquisitionFunding() { return acquisitionFunding; }
    public void setAcquisitionFunding(String acquisitionFunding) { this.acquisitionFunding = acquisitionFunding; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getVersion() { return version; }
}
