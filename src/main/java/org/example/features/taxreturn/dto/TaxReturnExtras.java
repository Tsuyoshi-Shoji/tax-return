package org.example.features.taxreturn.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.example.features.taxreturn.consumption.ConsumptionTaxCalculator;
import org.example.features.taxreturn.form.TaxReturnForms.ReturnInputForm;
import org.example.features.taxreturn.incometax.IncomeTaxCalculator;

/** 確定申告画面の拡張データ（固定資産台帳・決算書2〜3ページ・所得控除・税額・第一表/第二表・消費税）。 */
public class TaxReturnExtras {
    private Filer filer = new Filer();
    private final List<AssetRow> assets = new ArrayList<>();
    private BigDecimal depreciationTotal = BigDecimal.ZERO;
    private BigDecimal depreciationBusinessTotal = BigDecimal.ZERO;
    private BigDecimal assetOpeningTotal = BigDecimal.ZERO;
    private BigDecimal assetClosingTotal = BigDecimal.ZERO;
    private final List<MonthRow> monthlySalesPurchases = new ArrayList<>();
    private BigDecimal salesTotal = BigDecimal.ZERO;
    private BigDecimal purchaseTotal = BigDecimal.ZERO;
    private BigDecimal miscIncome = BigDecimal.ZERO;
    private final List<PayeeRow> rentRows = new ArrayList<>();
    private final List<PayeeRow> interestRows = new ArrayList<>();
    private final List<PayeeRow> feeRows = new ArrayList<>();
    private BigDecimal salaries = BigDecimal.ZERO;
    private BigDecimal dedicatedSalary = BigDecimal.ZERO;
    private BigDecimal incomeBeforeBlue = BigDecimal.ZERO;
    private BigDecimal blueDeductionApplied = BigDecimal.ZERO;
    private BigDecimal businessIncome = BigDecimal.ZERO;
    private BigDecimal businessRevenue = BigDecimal.ZERO;
    private BigDecimal businessWithholding = BigDecimal.ZERO;
    private ReturnInputForm returnInput = new ReturnInputForm();
    private final List<DeductionRow> deductions = new ArrayList<>();
    private IncomeTaxCalculator.Result tax;
    private String taxUnsupportedReason;
    private final List<IncomeBreakdownRow> incomeBreakdown = new ArrayList<>();
    private final List<org.example.entity.TaxDependent> dependents = new ArrayList<>();

    public List<org.example.entity.TaxDependent> getDependents() { return dependents; }
    private final List<LabelAmount> socialInsuranceRows = new ArrayList<>();
    private String consumptionStatus = "EXEMPT";
    private ConsumptionTaxCalculator.Result consumption;
    private BigDecimal basePeriodSales;
    private String basePeriodSource;
    private final List<String> consumptionNotes = new ArrayList<>();
    private final List<Option> assetAccountOptions = new ArrayList<>();
    private final List<Option> deductionTypeOptions = new ArrayList<>();
    private String ruleNote;

    public Filer getFiler() { return filer; }
    public void setFiler(Filer filer) { this.filer = filer; }
    public List<AssetRow> getAssets() { return assets; }
    public BigDecimal getDepreciationTotal() { return depreciationTotal; }
    public void setDepreciationTotal(BigDecimal v) { depreciationTotal = v; }
    public BigDecimal getDepreciationBusinessTotal() { return depreciationBusinessTotal; }
    public void setDepreciationBusinessTotal(BigDecimal v) { depreciationBusinessTotal = v; }
    public BigDecimal getAssetOpeningTotal() { return assetOpeningTotal; }
    public void setAssetOpeningTotal(BigDecimal v) { assetOpeningTotal = v; }
    public BigDecimal getAssetClosingTotal() { return assetClosingTotal; }
    public void setAssetClosingTotal(BigDecimal v) { assetClosingTotal = v; }
    public List<MonthRow> getMonthlySalesPurchases() { return monthlySalesPurchases; }
    public BigDecimal getSalesTotal() { return salesTotal; }
    public void setSalesTotal(BigDecimal v) { salesTotal = v; }
    public BigDecimal getPurchaseTotal() { return purchaseTotal; }
    public void setPurchaseTotal(BigDecimal v) { purchaseTotal = v; }
    public BigDecimal getMiscIncome() { return miscIncome; }
    public void setMiscIncome(BigDecimal v) { miscIncome = v; }
    public List<PayeeRow> getRentRows() { return rentRows; }
    public List<PayeeRow> getInterestRows() { return interestRows; }
    public List<PayeeRow> getFeeRows() { return feeRows; }
    public BigDecimal getSalaries() { return salaries; }
    public void setSalaries(BigDecimal v) { salaries = v; }
    public BigDecimal getDedicatedSalary() { return dedicatedSalary; }
    public void setDedicatedSalary(BigDecimal v) { dedicatedSalary = v; }
    public BigDecimal getIncomeBeforeBlue() { return incomeBeforeBlue; }
    public void setIncomeBeforeBlue(BigDecimal v) { incomeBeforeBlue = v; }
    public BigDecimal getBlueDeductionApplied() { return blueDeductionApplied; }
    public void setBlueDeductionApplied(BigDecimal v) { blueDeductionApplied = v; }
    public BigDecimal getBusinessIncome() { return businessIncome; }
    public void setBusinessIncome(BigDecimal v) { businessIncome = v; }
    public BigDecimal getBusinessRevenue() { return businessRevenue; }
    public void setBusinessRevenue(BigDecimal v) { businessRevenue = v; }
    public BigDecimal getBusinessWithholding() { return businessWithholding; }
    public void setBusinessWithholding(BigDecimal v) { businessWithholding = v; }
    public ReturnInputForm getReturnInput() { return returnInput; }
    public void setReturnInput(ReturnInputForm v) { returnInput = v; }
    public List<DeductionRow> getDeductions() { return deductions; }
    public IncomeTaxCalculator.Result getTax() { return tax; }
    public void setTax(IncomeTaxCalculator.Result v) { tax = v; }
    public String getTaxUnsupportedReason() { return taxUnsupportedReason; }
    public void setTaxUnsupportedReason(String v) { taxUnsupportedReason = v; }
    public List<IncomeBreakdownRow> getIncomeBreakdown() { return incomeBreakdown; }
    public List<LabelAmount> getSocialInsuranceRows() { return socialInsuranceRows; }
    public String getConsumptionStatus() { return consumptionStatus; }
    public void setConsumptionStatus(String v) { consumptionStatus = v; }
    public boolean isConsumptionTaxable() { return "SIMPLIFIED".equals(consumptionStatus); }
    public ConsumptionTaxCalculator.Result getConsumption() { return consumption; }
    public void setConsumption(ConsumptionTaxCalculator.Result v) { consumption = v; }
    public BigDecimal getBasePeriodSales() { return basePeriodSales; }
    public void setBasePeriodSales(BigDecimal v) { basePeriodSales = v; }
    public String getBasePeriodSource() { return basePeriodSource; }
    public void setBasePeriodSource(String v) { basePeriodSource = v; }
    public List<String> getConsumptionNotes() { return consumptionNotes; }
    public List<Option> getAssetAccountOptions() { return assetAccountOptions; }
    public List<Option> getDeductionTypeOptions() { return deductionTypeOptions; }
    public String getRuleNote() { return ruleNote; }
    public void setRuleNote(String v) { ruleNote = v; }

    public static class Filer {
        private String name;
        private String nameKana;
        private String birthDate;
        private String address;
        private String phone;
        private String businessName;
        private String industry;
        private String taxOffice;
        private String householdHeadName;
        private String householdHeadRelation;

        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getNameKana() { return nameKana; }
        public void setNameKana(String v) { nameKana = v; }
        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String v) { birthDate = v; }
        public String getAddress() { return address; }
        public void setAddress(String v) { address = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { phone = v; }
        public String getBusinessName() { return businessName; }
        public void setBusinessName(String v) { businessName = v; }
        public String getIndustry() { return industry; }
        public void setIndustry(String v) { industry = v; }
        public String getTaxOffice() { return taxOffice; }
        public void setTaxOffice(String v) { taxOffice = v; }
        public String getHouseholdHeadName() { return householdHeadName; }
        public void setHouseholdHeadName(String v) { householdHeadName = v; }
        public String getHouseholdHeadRelation() { return householdHeadRelation; }
        public void setHouseholdHeadRelation(String v) { householdHeadRelation = v; }
    }

    public static class AssetRow {
        private final Long id;
        private final String name;
        private final String accountName;
        private final String acquired;
        private final String serviceStart;
        private final BigDecimal cost;
        private final String methodLabel;
        private final Integer usefulLife;
        private final BigDecimal rate;
        private final int months;
        private final BigDecimal opening;
        private final BigDecimal depreciation;
        private final BigDecimal businessRatio;
        private final BigDecimal businessPortion;
        private final BigDecimal closing;
        private final String notes;
        private final boolean deletable;

        public AssetRow(Long id, String name, String accountName, String acquired, String serviceStart, BigDecimal cost,
                String methodLabel, Integer usefulLife, BigDecimal rate, int months, BigDecimal opening,
                BigDecimal depreciation, BigDecimal businessRatio, BigDecimal businessPortion, BigDecimal closing,
                String notes, boolean deletable) {
            this.id = id;
            this.name = name;
            this.accountName = accountName;
            this.acquired = acquired;
            this.serviceStart = serviceStart;
            this.cost = cost;
            this.methodLabel = methodLabel;
            this.usefulLife = usefulLife;
            this.rate = rate;
            this.months = months;
            this.opening = opening;
            this.depreciation = depreciation;
            this.businessRatio = businessRatio;
            this.businessPortion = businessPortion;
            this.closing = closing;
            this.notes = notes;
            this.deletable = deletable;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getAccountName() { return accountName; }
        public String getAcquired() { return acquired; }
        public String getServiceStart() { return serviceStart; }
        public BigDecimal getCost() { return cost; }
        public String getMethodLabel() { return methodLabel; }
        public Integer getUsefulLife() { return usefulLife; }
        public BigDecimal getRate() { return rate; }
        public int getMonths() { return months; }
        public BigDecimal getOpening() { return opening; }
        public BigDecimal getDepreciation() { return depreciation; }
        public BigDecimal getBusinessRatio() { return businessRatio; }
        public BigDecimal getBusinessPortion() { return businessPortion; }
        public BigDecimal getClosing() { return closing; }
        public String getNotes() { return notes; }
        public boolean isDeletable() { return deletable; }
    }

    public static class MonthRow {
        private final int month;
        private final BigDecimal sales;
        private final BigDecimal purchases;

        public MonthRow(int month, BigDecimal sales, BigDecimal purchases) {
            this.month = month;
            this.sales = sales;
            this.purchases = purchases;
        }

        public int getMonth() { return month; }
        public BigDecimal getSales() { return sales; }
        public BigDecimal getPurchases() { return purchases; }
    }

    public static class PayeeRow {
        private final String name;
        private final String address;
        private final String description;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal deductible = BigDecimal.ZERO;

        public PayeeRow(String name, String address, String description) {
            this.name = name;
            this.address = address;
            this.description = description;
        }

        public void add(BigDecimal gross, BigDecimal deductiblePart) {
            amount = amount.add(gross);
            deductible = deductible.add(deductiblePart);
        }

        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getDescription() { return description; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getDeductible() { return deductible; }
    }

    public static class DeductionRow {
        private final Long id;
        private final String typeLabel;
        private final String paidDate;
        private final BigDecimal paidAmount;
        private final BigDecimal reimbursedAmount;
        private final String payer;
        private final String notes;
        private final String status;
        private final String statusLabel;
        private final boolean fromTransaction;

        public DeductionRow(Long id, String typeLabel, String paidDate, BigDecimal paidAmount, BigDecimal reimbursedAmount,
                String payer, String notes, String status, String statusLabel, boolean fromTransaction) {
            this.id = id;
            this.typeLabel = typeLabel;
            this.paidDate = paidDate;
            this.paidAmount = paidAmount;
            this.reimbursedAmount = reimbursedAmount;
            this.payer = payer;
            this.notes = notes;
            this.status = status;
            this.statusLabel = statusLabel;
            this.fromTransaction = fromTransaction;
        }

        public Long getId() { return id; }
        public String getTypeLabel() { return typeLabel; }
        public String getPaidDate() { return paidDate; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public BigDecimal getReimbursedAmount() { return reimbursedAmount; }
        public String getPayer() { return payer; }
        public String getNotes() { return notes; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return statusLabel; }
        public boolean isFromTransaction() { return fromTransaction; }
        public boolean isApplied() { return "VERIFIED".equals(status) || "APPLIED".equals(status); }
    }

    public static class IncomeBreakdownRow {
        private final String kind;
        private final String item;
        private final String payerName;
        private final String payerAddress;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal withholding = BigDecimal.ZERO;

        public IncomeBreakdownRow(String kind, String item, String payerName, String payerAddress) {
            this.kind = kind;
            this.item = item;
            this.payerName = payerName;
            this.payerAddress = payerAddress;
        }

        public void add(BigDecimal amount, BigDecimal tax) {
            revenue = revenue.add(amount);
            withholding = withholding.add(tax);
        }

        public String getKind() { return kind; }
        public String getItem() { return item; }
        public String getPayerName() { return payerName; }
        public String getPayerAddress() { return payerAddress; }
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getWithholding() { return withholding; }
    }

    public static class LabelAmount {
        private final String label;
        private final long amount;

        public LabelAmount(String label, long amount) {
            this.label = label;
            this.amount = amount;
        }

        public String getLabel() { return label; }
        public long getAmount() { return amount; }
    }

    public static class Option {
        private final String value;
        private final String label;

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
    }
}
