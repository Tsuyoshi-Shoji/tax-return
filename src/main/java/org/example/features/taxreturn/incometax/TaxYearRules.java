package org.example.features.taxreturn.incometax;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * 年分別の所得税ルール（控除額・所得要件・特例の有無）。
 * 金額は国税庁タックスアンサー（令和8年4月1日現在法令等）で確認した値。
 * No.1199 基礎控除、No.1410 給与所得控除、No.1180 扶養控除、No.1191 配偶者控除、No.1195 配偶者特別控除、
 * No.1177 特定親族特別控除、No.1175 勤労学生控除、No.1140 生命保険料控除、No.2260 所得税の税率。
 * 新しい年分に対応する場合は、公式資料で確認のうえ年分を追加し、確認日を更新する。
 */
public final class TaxYearRules {
    public static final LocalDate VERIFIED_ON = LocalDate.of(2026, 11, 1);
    public static final String SOURCE = "国税庁タックスアンサー（令和8年4月1日現在法令等）";

    private static final Map<Integer, TaxYearRules> RULES = Map.of(
            2024, new TaxYearRules(2024, 480_000, 750_000, false, false, true),
            2025, new TaxYearRules(2025, 580_000, 850_000, true, false, false),
            2026, new TaxYearRules(2026, 620_000, 890_000, true, true, false));

    private final int year;
    private final long dependentIncomeLimit;
    private final long workingStudentIncomeLimit;
    private final boolean specificRelativeDeduction;
    private final boolean lifeInsuranceU23Extension;
    private final boolean flatRateReduction;

    private TaxYearRules(int year, long dependentIncomeLimit, long workingStudentIncomeLimit,
            boolean specificRelativeDeduction, boolean lifeInsuranceU23Extension, boolean flatRateReduction) {
        this.year = year;
        this.dependentIncomeLimit = dependentIncomeLimit;
        this.workingStudentIncomeLimit = workingStudentIncomeLimit;
        this.specificRelativeDeduction = specificRelativeDeduction;
        this.lifeInsuranceU23Extension = lifeInsuranceU23Extension;
        this.flatRateReduction = flatRateReduction;
    }

    public static Optional<TaxYearRules> of(int year) {
        return Optional.ofNullable(RULES.get(year));
    }

    public int getYear() {
        return year;
    }

    /** 扶養親族・同一生計配偶者の合計所得金額の上限（配偶者特別控除の下限）。 */
    public long getDependentIncomeLimit() {
        return dependentIncomeLimit;
    }

    public long getWorkingStudentIncomeLimit() {
        return workingStudentIncomeLimit;
    }

    public boolean isSpecificRelativeDeduction() {
        return specificRelativeDeduction;
    }

    public boolean isLifeInsuranceU23Extension() {
        return lifeInsuranceU23Extension;
    }

    /** 令和6年分の定額減税（特別税額控除）。 */
    public boolean isFlatRateReduction() {
        return flatRateReduction;
    }

    public long basicDeduction(long totalIncome) {
        if (totalIncome > 25_000_000L) {
            return 0;
        }
        if (totalIncome > 24_500_000L) {
            return 160_000;
        }
        if (totalIncome > 24_000_000L) {
            return 320_000;
        }
        if (year <= 2024) {
            return 480_000;
        }
        if (totalIncome > 23_500_000L) {
            return 480_000;
        }
        if (year == 2025) {
            if (totalIncome <= 1_320_000L) {
                return 950_000;
            }
            if (totalIncome <= 3_360_000L) {
                return 880_000;
            }
            if (totalIncome <= 4_890_000L) {
                return 680_000;
            }
            if (totalIncome <= 6_550_000L) {
                return 630_000;
            }
            return 580_000;
        }
        if (totalIncome <= 4_890_000L) {
            return 1_040_000;
        }
        if (totalIncome <= 6_550_000L) {
            return 670_000;
        }
        return 620_000;
    }

    /**
     * 給与所得の金額。660万円未満は所得税法別表第五の刻み（4千円単位）に合わせて計算する。
     */
    public long salaryIncome(long revenue) {
        if (revenue <= 0) {
            return 0;
        }
        if (revenue >= 8_500_000L) {
            return revenue - 1_950_000L;
        }
        if (revenue >= 6_600_000L) {
            return revenue * 9 / 10 - 1_100_000L;
        }
        if (year >= 2026) {
            if (revenue < 741_000L) {
                return 0;
            }
            if (revenue < 2_191_000L) {
                return revenue - 740_000L;
            }
            if (revenue < 2_193_000L) {
                return 1_451_000L;
            }
            if (revenue < 2_196_000L) {
                return 1_453_000L;
            }
            if (revenue < 2_200_000L) {
                return 1_456_000L;
            }
            return table5(revenue);
        }
        if (year == 2025) {
            if (revenue <= 1_900_000L) {
                return Math.max(0, revenue - 650_000L);
            }
            return table5(revenue);
        }
        if (revenue < 1_619_000L) {
            return Math.max(0, revenue - 550_000L);
        }
        if (revenue < 1_620_000L) {
            return 1_069_000L;
        }
        if (revenue < 1_622_000L) {
            return 1_070_000L;
        }
        if (revenue < 1_624_000L) {
            return 1_072_000L;
        }
        if (revenue < 1_628_000L) {
            return 1_074_000L;
        }
        long a = revenue / 4_000L * 4_000L;
        if (revenue <= 1_800_000L) {
            return a * 6 / 10 + 100_000L;
        }
        return table5Upper(a);
    }

    private static long table5(long revenue) {
        return table5Upper(revenue / 4_000L * 4_000L);
    }

    private static long table5Upper(long a) {
        if (a <= 3_600_000L) {
            return a * 7 / 10 - 80_000L;
        }
        return a * 8 / 10 - 440_000L;
    }

    /** 課税される所得金額（1,000円未満切捨て後）に対する所得税額（速算表）。 */
    public static long incomeTax(long taxable) {
        if (taxable <= 0) {
            return 0;
        }
        long[][] table = {
                {1_949_000L, 5, 0},
                {3_299_000L, 10, 97_500L},
                {6_949_000L, 20, 427_500L},
                {8_999_000L, 23, 636_000L},
                {17_999_000L, 33, 1_536_000L},
                {39_999_000L, 40, 2_796_000L},
                {Long.MAX_VALUE, 45, 4_796_000L}};
        for (long[] row : table) {
            if (taxable <= row[0]) {
                return taxable * row[1] / 100 - row[2];
            }
        }
        return 0;
    }
}
