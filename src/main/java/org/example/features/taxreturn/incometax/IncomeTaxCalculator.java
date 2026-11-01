package org.example.features.taxreturn.incometax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

import org.example.entity.TaxDeductionRecord;
import org.example.entity.TaxDependent;
import org.example.entity.TaxReturnInput;

/**
 * 所得控除・税額の計算（決定的ロジック。AIの回答に依存しない）。
 * 対応範囲：事業所得（営業等）、給与所得、雑所得（業務・その他）、主な所得控除、定額減税（令和6年分）、
 * 復興特別所得税、源泉徴収税額、予定納税額。純損失の繰越控除、分離課税、税額控除（住宅ローン控除等）は対象外。
 */
public final class IncomeTaxCalculator {
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_APPLIED = "APPLIED";

    private IncomeTaxCalculator() {
    }

    public static Result calculate(Input in) {
        Result r = new Result();
        TaxYearRules rules = in.rules();
        int year = rules.getYear();
        TaxReturnInput ti = in.returnInput();
        long limit = rules.getDependentIncomeLimit();

        r.businessRevenue = in.businessRevenue();
        r.businessIncome = in.businessIncome();
        r.salaryRevenue = whole(ti == null ? null : ti.getSalaryRevenue());
        r.salaryIncome = rules.salaryIncome(r.salaryRevenue);
        r.miscRevenue = whole(ti == null ? null : ti.getMiscRevenue());
        r.miscIncome = Math.max(0, r.miscRevenue - whole(ti == null ? null : ti.getMiscExpense()));
        r.totalIncome = r.businessIncome + r.salaryIncome + r.miscIncome;
        long base = Math.max(0, r.totalIncome);

        for (TaxDeductionRecord d : in.deductions()) {
            if (!STATUS_VERIFIED.equals(d.getStatus()) && !STATUS_APPLIED.equals(d.getStatus())) {
                continue;
            }
            long paid = whole(d.getPaidAmount());
            String type = d.getDeductionType() == null ? "" : d.getDeductionType();
            if (type.startsWith("SOCIAL_INSURANCE")) {
                r.socialInsurancePaid += paid;
            } else if ("SMALL_BUSINESS_MUTUAL".equals(type)) {
                r.smallBusinessMutual += paid;
            } else if ("LIFE_INSURANCE_GENERAL".equals(type)) {
                r.lifeGeneralPaid += paid;
            } else if ("LIFE_INSURANCE_MEDICAL".equals(type)) {
                r.lifeMedicalPaid += paid;
            } else if ("LIFE_INSURANCE_PENSION".equals(type)) {
                r.lifePensionPaid += paid;
            } else if ("EARTHQUAKE_INSURANCE".equals(type)) {
                r.earthquakePaid += paid;
            } else if ("MEDICAL".equals(type)) {
                r.medicalPaid += paid;
                r.medicalReimbursed += Math.min(paid, whole(d.getReimbursedAmount()));
            } else if ("DONATION".equals(type)) {
                r.donationPaid += paid;
            }
        }
        r.socialInsurance = r.socialInsurancePaid;

        boolean hasYoungDependent = in.dependents().stream().anyMatch(dep -> ageAtYearEnd(dep.getBirthDate(), year) < 23);
        boolean extended = rules.isLifeInsuranceU23Extension() && hasYoungDependent;
        long general = extended ? lifeExtended(r.lifeGeneralPaid) : lifeNew(r.lifeGeneralPaid);
        r.lifeInsurance = Math.min(120_000, general + lifeNew(r.lifeMedicalPaid) + lifeNew(r.lifePensionPaid));
        r.earthquake = Math.min(50_000, r.earthquakePaid);

        if (ti != null && r.totalIncome <= 5_000_000L) {
            if ("WIDOW".equals(ti.getWidowStatus())) {
                r.widow = 270_000;
            } else if ("SINGLE_PARENT".equals(ti.getWidowStatus())) {
                r.singleParent = 350_000;
            }
        }
        if (ti != null && ti.isWorkingStudent() && r.totalIncome <= rules.getWorkingStudentIncomeLimit()) {
            r.workingStudent = 270_000;
        }
        if (ti != null) {
            r.disability += selfDisability(ti.getSelfDisability());
        }

        boolean spouseEligible = ti != null && ti.isHasSpouse();
        long spouseIncome = ti == null ? 0 : whole(ti.getSpouseIncome());
        boolean sameLivelihoodSpouse = spouseEligible && spouseIncome <= limit;
        if (spouseEligible && r.totalIncome <= 10_000_000L) {
            boolean elderly = ti.getSpouseBirthDate() != null && ageAtYearEnd(ti.getSpouseBirthDate(), year) >= 70;
            if (spouseIncome <= limit) {
                r.spouse = spouseDeduction(r.totalIncome, elderly);
            } else if (spouseIncome <= 1_330_000L) {
                r.spouseSpecial = spouseSpecialDeduction(r.totalIncome, spouseIncome);
            }
        }
        if (sameLivelihoodSpouse) {
            r.disability += relativeDisability(ti.getSpouseDisability(), ti.isSpouseCohabiting());
        }

        int flatRateCount = 1 + (sameLivelihoodSpouse ? 1 : 0);
        for (TaxDependent dep : in.dependents()) {
            int age = ageAtYearEnd(dep.getBirthDate(), year);
            long income = whole(dep.getIncome());
            DependentRow row = new DependentRow(dep, age);
            if (income <= limit) {
                flatRateCount++;
                r.disability += relativeDisability(dep.getDisability(), dep.isCohabiting());
                if (age >= 16) {
                    if (age >= 19 && age <= 22) {
                        row.deduction = 630_000;
                        row.category = "特定扶養親族";
                    } else if (age >= 70) {
                        boolean parent = dep.isCohabiting() && isLinealAscendant(dep.getRelationship());
                        row.deduction = parent ? 580_000 : 480_000;
                        row.category = parent ? "老人扶養親族（同居老親等）" : "老人扶養親族";
                    } else {
                        row.deduction = 380_000;
                        row.category = "一般の控除対象扶養親族";
                    }
                    r.dependents += row.deduction;
                } else {
                    row.category = "年少扶養親族（扶養控除なし）";
                }
            } else if (rules.isSpecificRelativeDeduction() && age >= 19 && age <= 22 && income <= 1_230_000L) {
                row.specificRelative = specificRelativeDeduction(income);
                row.category = "特定親族";
                r.specificRelative += row.specificRelative;
            } else {
                row.category = "所得要件を超えるため控除対象外";
            }
            r.dependentRows.add(row);
        }

        r.basic = rules.basicDeduction(r.totalIncome);

        long medicalThreshold = Math.min(100_000, base * 5 / 100);
        r.medical = Math.max(0, Math.min(2_000_000, r.medicalPaid - r.medicalReimbursed - medicalThreshold));
        if (r.donationPaid > 0) {
            r.donation = Math.max(0, Math.min(r.donationPaid, base * 40 / 100) - 2_000);
        }

        r.deductionTotal = r.socialInsurance + r.smallBusinessMutual + r.lifeInsurance + r.earthquake + r.widow
                + r.singleParent + r.workingStudent + r.disability + r.spouse + r.spouseSpecial + r.dependents
                + r.specificRelative + r.basic + r.medical + r.donation;

        r.taxableIncome = Math.max(0, base - r.deductionTotal) / 1_000 * 1_000;
        r.incomeTax = TaxYearRules.incomeTax(r.taxableIncome);
        if (rules.isFlatRateReduction() && r.totalIncome <= 18_050_000L) {
            r.flatRateReductionAvailable = 30_000L * flatRateCount;
            r.flatRateReduction = Math.min(r.flatRateReductionAvailable, r.incomeTax);
        }
        r.baseIncomeTax = r.incomeTax - r.flatRateReduction;
        r.reconstructionTax = r.baseIncomeTax * 21 / 1_000;
        r.totalTax = r.baseIncomeTax + r.reconstructionTax;
        r.withholding = in.businessWithholding() + whole(ti == null ? null : ti.getSalaryWithholding())
                + whole(ti == null ? null : ti.getMiscWithholding());
        r.declaredTax = r.totalTax - r.withholding;
        r.prepaidTax = whole(ti == null ? null : ti.getPrepaidTax());
        long third = r.declaredTax - r.prepaidTax;
        if (third >= 0) {
            r.taxDue = third / 100 * 100;
        } else {
            r.refund = -third;
        }
        return r;
    }

    /** 12月31日現在の年齢（1月1日生まれは前日の12月31日に加齢するため1歳加算）。 */
    public static int ageAtYearEnd(LocalDate birthDate, int year) {
        if (birthDate == null) {
            return 0;
        }
        int age = year - birthDate.getYear();
        if (MonthDay.from(birthDate).equals(MonthDay.of(1, 1))) {
            age++;
        }
        return age;
    }

    static long lifeNew(long paid) {
        if (paid <= 20_000) {
            return paid;
        }
        if (paid <= 40_000) {
            return paid / 2 + 10_000;
        }
        if (paid <= 80_000) {
            return paid / 4 + 20_000;
        }
        return 40_000;
    }

    static long lifeExtended(long paid) {
        if (paid <= 30_000) {
            return paid;
        }
        if (paid <= 60_000) {
            return paid / 2 + 15_000;
        }
        if (paid <= 120_000) {
            return paid / 4 + 30_000;
        }
        return 60_000;
    }

    static long spouseDeduction(long totalIncome, boolean elderly) {
        if (totalIncome <= 9_000_000L) {
            return elderly ? 480_000 : 380_000;
        }
        if (totalIncome <= 9_500_000L) {
            return elderly ? 320_000 : 260_000;
        }
        if (totalIncome <= 10_000_000L) {
            return elderly ? 160_000 : 130_000;
        }
        return 0;
    }

    static long spouseSpecialDeduction(long totalIncome, long spouseIncome) {
        long[] uppers = {950_000, 1_000_000, 1_050_000, 1_100_000, 1_150_000, 1_200_000, 1_250_000, 1_300_000, 1_330_000};
        long[][] amounts = {
                {380_000, 360_000, 310_000, 260_000, 210_000, 160_000, 110_000, 60_000, 30_000},
                {260_000, 240_000, 210_000, 180_000, 140_000, 110_000, 80_000, 40_000, 20_000},
                {130_000, 120_000, 110_000, 90_000, 70_000, 60_000, 40_000, 20_000, 10_000}};
        int column = totalIncome <= 9_000_000L ? 0 : totalIncome <= 9_500_000L ? 1 : totalIncome <= 10_000_000L ? 2 : -1;
        if (column < 0) {
            return 0;
        }
        for (int i = 0; i < uppers.length; i++) {
            if (spouseIncome <= uppers[i]) {
                return amounts[column][i];
            }
        }
        return 0;
    }

    static long specificRelativeDeduction(long income) {
        long[] uppers = {850_000, 900_000, 950_000, 1_000_000, 1_050_000, 1_100_000, 1_150_000, 1_200_000, 1_230_000};
        long[] amounts = {630_000, 610_000, 510_000, 410_000, 310_000, 210_000, 110_000, 60_000, 30_000};
        for (int i = 0; i < uppers.length; i++) {
            if (income <= uppers[i]) {
                return amounts[i];
            }
        }
        return 0;
    }

    private static long selfDisability(String type) {
        if ("SPECIAL".equals(type)) {
            return 400_000;
        }
        return "GENERAL".equals(type) ? 270_000 : 0;
    }

    private static long relativeDisability(String type, boolean cohabiting) {
        if ("SPECIAL".equals(type)) {
            return cohabiting ? 750_000 : 400_000;
        }
        return "GENERAL".equals(type) ? 270_000 : 0;
    }

    private static boolean isLinealAscendant(String relationship) {
        if (relationship == null) {
            return false;
        }
        return relationship.contains("父") || relationship.contains("母");
    }

    private static long whole(BigDecimal value) {
        return value == null ? 0 : value.longValue();
    }

    public record Input(TaxYearRules rules, long businessRevenue, long businessIncome, long businessWithholding,
            TaxReturnInput returnInput, List<TaxDependent> dependents, List<TaxDeductionRecord> deductions) {
    }

    public static final class DependentRow {
        private final TaxDependent dependent;
        private final int age;
        private long deduction;
        private long specificRelative;
        private String category;

        DependentRow(TaxDependent dependent, int age) {
            this.dependent = dependent;
            this.age = age;
        }

        public TaxDependent getDependent() { return dependent; }
        public int getAge() { return age; }
        public long getDeduction() { return deduction; }
        public long getSpecificRelative() { return specificRelative; }
        public String getCategory() { return category; }
    }

    public static final class Result {
        long businessRevenue;
        long businessIncome;
        long salaryRevenue;
        long salaryIncome;
        long miscRevenue;
        long miscIncome;
        long totalIncome;
        long socialInsurancePaid;
        long socialInsurance;
        long smallBusinessMutual;
        long lifeGeneralPaid;
        long lifeMedicalPaid;
        long lifePensionPaid;
        long lifeInsurance;
        long earthquakePaid;
        long earthquake;
        long widow;
        long singleParent;
        long workingStudent;
        long disability;
        long spouse;
        long spouseSpecial;
        long dependents;
        long specificRelative;
        long basic;
        long medicalPaid;
        long medicalReimbursed;
        long medical;
        long donationPaid;
        long donation;
        long deductionTotal;
        long taxableIncome;
        long incomeTax;
        long flatRateReductionAvailable;
        long flatRateReduction;
        long baseIncomeTax;
        long reconstructionTax;
        long totalTax;
        long withholding;
        long declaredTax;
        long prepaidTax;
        long taxDue;
        long refund;
        final List<DependentRow> dependentRows = new ArrayList<>();

        public long getBusinessRevenue() { return businessRevenue; }
        public long getBusinessIncome() { return businessIncome; }
        public long getSalaryRevenue() { return salaryRevenue; }
        public long getSalaryIncome() { return salaryIncome; }
        public long getMiscRevenue() { return miscRevenue; }
        public long getMiscIncome() { return miscIncome; }
        public long getTotalIncome() { return totalIncome; }
        public long getSocialInsurance() { return socialInsurance; }
        public long getSmallBusinessMutual() { return smallBusinessMutual; }
        public long getLifeGeneralPaid() { return lifeGeneralPaid; }
        public long getLifeMedicalPaid() { return lifeMedicalPaid; }
        public long getLifePensionPaid() { return lifePensionPaid; }
        public long getLifeInsurance() { return lifeInsurance; }
        public long getEarthquakePaid() { return earthquakePaid; }
        public long getEarthquake() { return earthquake; }
        public long getWidow() { return widow; }
        public long getSingleParent() { return singleParent; }
        public long getWidowOrSingleParent() { return widow + singleParent; }
        public long getWorkingStudent() { return workingStudent; }
        public long getDisability() { return disability; }
        public long getWorkingStudentOrDisability() { return workingStudent + disability; }
        public long getSpouse() { return spouse; }
        public long getSpouseSpecial() { return spouseSpecial; }
        public long getDependents() { return dependents; }
        public long getSpecificRelative() { return specificRelative; }
        public long getBasic() { return basic; }
        public long getMedicalPaid() { return medicalPaid; }
        public long getMedicalReimbursed() { return medicalReimbursed; }
        public long getMedical() { return medical; }
        public long getDonationPaid() { return donationPaid; }
        public long getDonation() { return donation; }
        public long getDeductionTotal() { return deductionTotal; }
        public long getTaxableIncome() { return taxableIncome; }
        public long getIncomeTax() { return incomeTax; }
        public long getFlatRateReductionAvailable() { return flatRateReductionAvailable; }
        public long getFlatRateReduction() { return flatRateReduction; }
        public long getBaseIncomeTax() { return baseIncomeTax; }
        public long getReconstructionTax() { return reconstructionTax; }
        public long getTotalTax() { return totalTax; }
        public long getWithholding() { return withholding; }
        public long getDeclaredTax() { return declaredTax; }
        public long getPrepaidTax() { return prepaidTax; }
        public long getTaxDue() { return taxDue; }
        public long getRefund() { return refund; }
        public List<DependentRow> getDependentRows() { return dependentRows; }
    }
}
