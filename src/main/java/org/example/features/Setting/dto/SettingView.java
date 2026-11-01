package org.example.features.setting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SettingView {
    private final String displayName;
    private final String email;
    private final String status;
    private final String businessName;
    private final String industry;
    private final String phone;
    private final String address;
    private final BigDecimal apportionmentRate;
    private final String nameKana;
    private final LocalDate birthDate;
    private final String taxOffice;
    private final String householdHeadName;
    private final String householdHeadRelation;
    private final String consumptionTaxStatus;
    private final Integer simplifiedBusinessType;

    public SettingView(
            String displayName,
            String email,
            String status,
            String businessName,
            String industry,
            String phone,
            String address,
            BigDecimal apportionmentRate,
            String nameKana,
            LocalDate birthDate,
            String taxOffice,
            String householdHeadName,
            String householdHeadRelation,
            String consumptionTaxStatus,
            Integer simplifiedBusinessType) {
        this.displayName = displayName;
        this.email = email;
        this.status = status;
        this.businessName = businessName;
        this.industry = industry;
        this.phone = phone;
        this.address = address;
        this.apportionmentRate = apportionmentRate;
        this.nameKana = nameKana;
        this.birthDate = birthDate;
        this.taxOffice = taxOffice;
        this.householdHeadName = householdHeadName;
        this.householdHeadRelation = householdHeadRelation;
        this.consumptionTaxStatus = consumptionTaxStatus;
        this.simplifiedBusinessType = simplifiedBusinessType;
    }

    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public String getStatusLabel() { return "active".equalsIgnoreCase(status) ? "Active" : status; }
    public String getBusinessName() { return businessName; }
    public String getIndustry() { return industry; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public BigDecimal getApportionmentRate() { return apportionmentRate; }
    public String getNameKana() { return nameKana; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getTaxOffice() { return taxOffice; }
    public String getHouseholdHeadName() { return householdHeadName; }
    public String getHouseholdHeadRelation() { return householdHeadRelation; }
    public String getConsumptionTaxStatus() { return consumptionTaxStatus; }
    public Integer getSimplifiedBusinessType() { return simplifiedBusinessType; }
}
