package org.example.features.taxreturn.consumption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.example.entity.IncomeEntry;

/**
 * 消費税（簡易課税）の納付税額の計算。税込経理で記帳された収益（申告対象・課税区分）から計算する。
 * 課税標準額は税率ごとに1,000円未満切捨て、消費税額は国税7.8%（軽減6.24%）、
 * 控除対象仕入税額＝消費税額×みなし仕入率、差引税額・地方消費税（差引税額×22/78）は100円未満切捨て。
 * 売上に係る対価の返還等・貸倒れ・中間納付は対象外。
 */
public final class ConsumptionTaxCalculator {
    public static final String TAXABLE_10 = "TAXABLE_10";
    public static final String TAXABLE_8 = "TAXABLE_8";
    public static final BigDecimal SIMPLIFIED_UPPER_LIMIT = new BigDecimal("50000000");
    public static final BigDecimal EXEMPTION_THRESHOLD = new BigDecimal("10000000");
    private static final Map<Integer, Integer> DEEMED_RATES = Map.of(1, 90, 2, 80, 3, 70, 4, 60, 5, 50, 6, 40);

    private ConsumptionTaxCalculator() {
    }

    public static Map<Integer, Integer> deemedRates() {
        return DEEMED_RATES;
    }

    public static BigDecimal taxableSalesExcludingTax(List<IncomeEntry> entries) {
        BigDecimal total = BigDecimal.ZERO;
        for (IncomeEntry e : entries) {
            String category = category(e);
            if (TAXABLE_10.equals(category)) {
                total = total.add(e.getAmount().multiply(new BigDecimal("100")).divide(new BigDecimal("110"), 0, RoundingMode.DOWN));
            } else if (TAXABLE_8.equals(category)) {
                total = total.add(e.getAmount().multiply(new BigDecimal("100")).divide(new BigDecimal("108"), 0, RoundingMode.DOWN));
            } else if ("EXPORT_EXEMPT".equals(category)) {
                total = total.add(e.getAmount());
            }
        }
        return total;
    }

    public static Result calculate(List<IncomeEntry> entries, int businessType) {
        Result r = new Result();
        r.businessType = businessType;
        r.deemedRate = DEEMED_RATES.getOrDefault(businessType, 0);
        for (IncomeEntry e : entries) {
            String category = category(e);
            if (TAXABLE_10.equals(category)) {
                r.sales10 = r.sales10.add(e.getAmount());
            } else if (TAXABLE_8.equals(category)) {
                r.sales8 = r.sales8.add(e.getAmount());
            } else if ("EXPORT_EXEMPT".equals(category)) {
                r.exportSales = r.exportSales.add(e.getAmount());
            } else if ("NON_TAXABLE".equals(category)) {
                r.nonTaxableSales = r.nonTaxableSales.add(e.getAmount());
            }
        }
        r.base10 = truncate(r.sales10.multiply(new BigDecimal("100")).divide(new BigDecimal("110"), 0, RoundingMode.DOWN), 1000);
        r.base8 = truncate(r.sales8.multiply(new BigDecimal("100")).divide(new BigDecimal("108"), 0, RoundingMode.DOWN), 1000);
        r.taxableBase = r.base10.add(r.base8);
        r.tax10 = r.base10.multiply(new BigDecimal("0.078")).setScale(0, RoundingMode.DOWN);
        r.tax8 = r.base8.multiply(new BigDecimal("0.0624")).setScale(0, RoundingMode.DOWN);
        r.consumptionTax = r.tax10.add(r.tax8);
        r.deemedPurchaseTax = r.consumptionTax.multiply(BigDecimal.valueOf(r.deemedRate))
                .divide(new BigDecimal("100"), 0, RoundingMode.DOWN);
        r.netTax = truncate(r.consumptionTax.subtract(r.deemedPurchaseTax).max(BigDecimal.ZERO), 100);
        r.localTax = truncate(r.netTax.multiply(new BigDecimal("22")).divide(new BigDecimal("78"), 0, RoundingMode.DOWN), 100);
        r.totalTax = r.netTax.add(r.localTax);
        return r;
    }

    private static String category(IncomeEntry e) {
        return e.getTaxCategory() == null ? TAXABLE_10 : e.getTaxCategory();
    }

    private static BigDecimal truncate(BigDecimal value, int unit) {
        BigDecimal u = BigDecimal.valueOf(unit);
        return value.divide(u, 0, RoundingMode.DOWN).multiply(u);
    }

    public static final class Result {
        int businessType;
        int deemedRate;
        BigDecimal sales10 = BigDecimal.ZERO;
        BigDecimal sales8 = BigDecimal.ZERO;
        BigDecimal exportSales = BigDecimal.ZERO;
        BigDecimal nonTaxableSales = BigDecimal.ZERO;
        BigDecimal base10 = BigDecimal.ZERO;
        BigDecimal base8 = BigDecimal.ZERO;
        BigDecimal taxableBase = BigDecimal.ZERO;
        BigDecimal tax10 = BigDecimal.ZERO;
        BigDecimal tax8 = BigDecimal.ZERO;
        BigDecimal consumptionTax = BigDecimal.ZERO;
        BigDecimal deemedPurchaseTax = BigDecimal.ZERO;
        BigDecimal netTax = BigDecimal.ZERO;
        BigDecimal localTax = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        public int getBusinessType() { return businessType; }
        public int getDeemedRate() { return deemedRate; }
        public BigDecimal getSales10() { return sales10; }
        public BigDecimal getSales8() { return sales8; }
        public BigDecimal getExportSales() { return exportSales; }
        public BigDecimal getNonTaxableSales() { return nonTaxableSales; }
        public BigDecimal getBase10() { return base10; }
        public BigDecimal getBase8() { return base8; }
        public BigDecimal getTaxableBase() { return taxableBase; }
        public BigDecimal getTax10() { return tax10; }
        public BigDecimal getTax8() { return tax8; }
        public BigDecimal getConsumptionTax() { return consumptionTax; }
        public BigDecimal getDeemedPurchaseTax() { return deemedPurchaseTax; }
        public BigDecimal getNetTax() { return netTax; }
        public BigDecimal getLocalTax() { return localTax; }
        public BigDecimal getTotalTax() { return totalTax; }
    }
}
