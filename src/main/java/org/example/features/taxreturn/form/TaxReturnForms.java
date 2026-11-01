package org.example.features.taxreturn.form;

/** 確定申告画面の各入力フォーム（固定資産・申告情報・扶養親族・所得控除）。値はサーバー側で検証する。 */
public final class TaxReturnForms {
    private TaxReturnForms() {
    }

    public static class FixedAssetForm {
        private String assetName;
        private String accountCode;
        private String acquisitionDate;
        private String serviceStartDate;
        private String acquisitionCost;
        private String usefulLife;
        private String depreciationMethod;
        private String businessRatio;
        private String acquisitionFunding;
        private String notes;
        private String csrfToken;

        public String getAssetName() { return assetName; }
        public void setAssetName(String v) { assetName = v; }
        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String v) { accountCode = v; }
        public String getAcquisitionDate() { return acquisitionDate; }
        public void setAcquisitionDate(String v) { acquisitionDate = v; }
        public String getServiceStartDate() { return serviceStartDate; }
        public void setServiceStartDate(String v) { serviceStartDate = v; }
        public String getAcquisitionCost() { return acquisitionCost; }
        public void setAcquisitionCost(String v) { acquisitionCost = v; }
        public String getUsefulLife() { return usefulLife; }
        public void setUsefulLife(String v) { usefulLife = v; }
        public String getDepreciationMethod() { return depreciationMethod; }
        public void setDepreciationMethod(String v) { depreciationMethod = v; }
        public String getBusinessRatio() { return businessRatio; }
        public void setBusinessRatio(String v) { businessRatio = v; }
        public String getAcquisitionFunding() { return acquisitionFunding; }
        public void setAcquisitionFunding(String v) { acquisitionFunding = v; }
        public String getNotes() { return notes; }
        public void setNotes(String v) { notes = v; }
        public String getCsrfToken() { return csrfToken; }
        public void setCsrfToken(String v) { csrfToken = v; }
    }

    public static class ReturnInputForm {
        private String salaryRevenue;
        private String salaryWithholding;
        private String salaryPayerName;
        private String salaryPayerAddress;
        private String miscRevenue;
        private String miscExpense;
        private String miscWithholding;
        private String miscPayerName;
        private String prepaidTax;
        private String selfDisability;
        private String widowStatus;
        private boolean workingStudent;
        private boolean hasSpouse;
        private String spouseName;
        private String spouseBirthDate;
        private String spouseIncome;
        private String spouseDisability;
        private boolean spouseCohabiting;
        private String basePeriodTaxableSales;
        private String csrfToken;

        public String getSalaryRevenue() { return salaryRevenue; }
        public void setSalaryRevenue(String v) { salaryRevenue = v; }
        public String getSalaryWithholding() { return salaryWithholding; }
        public void setSalaryWithholding(String v) { salaryWithholding = v; }
        public String getSalaryPayerName() { return salaryPayerName; }
        public void setSalaryPayerName(String v) { salaryPayerName = v; }
        public String getSalaryPayerAddress() { return salaryPayerAddress; }
        public void setSalaryPayerAddress(String v) { salaryPayerAddress = v; }
        public String getMiscRevenue() { return miscRevenue; }
        public void setMiscRevenue(String v) { miscRevenue = v; }
        public String getMiscExpense() { return miscExpense; }
        public void setMiscExpense(String v) { miscExpense = v; }
        public String getMiscWithholding() { return miscWithholding; }
        public void setMiscWithholding(String v) { miscWithholding = v; }
        public String getMiscPayerName() { return miscPayerName; }
        public void setMiscPayerName(String v) { miscPayerName = v; }
        public String getPrepaidTax() { return prepaidTax; }
        public void setPrepaidTax(String v) { prepaidTax = v; }
        public String getSelfDisability() { return selfDisability; }
        public void setSelfDisability(String v) { selfDisability = v; }
        public String getWidowStatus() { return widowStatus; }
        public void setWidowStatus(String v) { widowStatus = v; }
        public boolean isWorkingStudent() { return workingStudent; }
        public void setWorkingStudent(boolean v) { workingStudent = v; }
        public boolean isHasSpouse() { return hasSpouse; }
        public void setHasSpouse(boolean v) { hasSpouse = v; }
        public String getSpouseName() { return spouseName; }
        public void setSpouseName(String v) { spouseName = v; }
        public String getSpouseBirthDate() { return spouseBirthDate; }
        public void setSpouseBirthDate(String v) { spouseBirthDate = v; }
        public String getSpouseIncome() { return spouseIncome; }
        public void setSpouseIncome(String v) { spouseIncome = v; }
        public String getSpouseDisability() { return spouseDisability; }
        public void setSpouseDisability(String v) { spouseDisability = v; }
        public boolean isSpouseCohabiting() { return spouseCohabiting; }
        public void setSpouseCohabiting(boolean v) { spouseCohabiting = v; }
        public String getBasePeriodTaxableSales() { return basePeriodTaxableSales; }
        public void setBasePeriodTaxableSales(String v) { basePeriodTaxableSales = v; }
        public String getCsrfToken() { return csrfToken; }
        public void setCsrfToken(String v) { csrfToken = v; }
    }

    public static class DependentForm {
        private String dependentName;
        private String relationship;
        private String birthDate;
        private String income;
        private boolean cohabiting;
        private String disability;
        private String csrfToken;

        public String getDependentName() { return dependentName; }
        public void setDependentName(String v) { dependentName = v; }
        public String getRelationship() { return relationship; }
        public void setRelationship(String v) { relationship = v; }
        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String v) { birthDate = v; }
        public String getIncome() { return income; }
        public void setIncome(String v) { income = v; }
        public boolean isCohabiting() { return cohabiting; }
        public void setCohabiting(boolean v) { cohabiting = v; }
        public String getDisability() { return disability; }
        public void setDisability(String v) { disability = v; }
        public String getCsrfToken() { return csrfToken; }
        public void setCsrfToken(String v) { csrfToken = v; }
    }

    public static class DeductionForm {
        private String deductionType;
        private String paidDate;
        private String paidAmount;
        private String reimbursedAmount;
        private String payer;
        private String notes;
        private String csrfToken;

        public String getDeductionType() { return deductionType; }
        public void setDeductionType(String v) { deductionType = v; }
        public String getPaidDate() { return paidDate; }
        public void setPaidDate(String v) { paidDate = v; }
        public String getPaidAmount() { return paidAmount; }
        public void setPaidAmount(String v) { paidAmount = v; }
        public String getReimbursedAmount() { return reimbursedAmount; }
        public void setReimbursedAmount(String v) { reimbursedAmount = v; }
        public String getPayer() { return payer; }
        public void setPayer(String v) { payer = v; }
        public String getNotes() { return notes; }
        public void setNotes(String v) { notes = v; }
        public String getCsrfToken() { return csrfToken; }
        public void setCsrfToken(String v) { csrfToken = v; }
    }
}
