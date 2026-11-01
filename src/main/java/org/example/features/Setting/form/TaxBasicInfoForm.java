package org.example.features.setting.form;

public class TaxBasicInfoForm {
    private String businessName;
    private String industry;
    private String phone;
    private String address;
    private String nameKana;
    private String birthDate;
    private String taxOffice;
    private String householdHeadName;
    private String householdHeadRelation;
    private String consumptionTaxStatus;
    private String simplifiedBusinessType;
    private String csrfToken;

    public String getNameKana() {
        return nameKana;
    }

    public void setNameKana(String nameKana) {
        this.nameKana = nameKana;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getTaxOffice() {
        return taxOffice;
    }

    public void setTaxOffice(String taxOffice) {
        this.taxOffice = taxOffice;
    }

    public String getHouseholdHeadName() {
        return householdHeadName;
    }

    public void setHouseholdHeadName(String householdHeadName) {
        this.householdHeadName = householdHeadName;
    }

    public String getHouseholdHeadRelation() {
        return householdHeadRelation;
    }

    public void setHouseholdHeadRelation(String householdHeadRelation) {
        this.householdHeadRelation = householdHeadRelation;
    }

    public String getConsumptionTaxStatus() {
        return consumptionTaxStatus;
    }

    public void setConsumptionTaxStatus(String consumptionTaxStatus) {
        this.consumptionTaxStatus = consumptionTaxStatus;
    }

    public String getSimplifiedBusinessType() {
        return simplifiedBusinessType;
    }

    public void setSimplifiedBusinessType(String simplifiedBusinessType) {
        this.simplifiedBusinessType = simplifiedBusinessType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }
}

