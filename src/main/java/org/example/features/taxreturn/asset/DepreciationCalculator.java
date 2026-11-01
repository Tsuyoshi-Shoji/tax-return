package org.example.features.taxreturn.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.example.entity.FixedAsset;

/**
 * 固定資産台帳1件の年分ごとの償却額を計算する（決定的ロジック）。
 * <ul>
 * <li>STRAIGHT_LINE：平成19年4月1日以後取得の定額法。償却率＝耐用年数の逆数を小数第3位に切上げ（法定の定額法償却率表と一致）。
 * 初年度は使用開始月から12月までの月数で月割り、1円未満切上げ。未償却残高が備忘価額1円に達するまで償却する。</li>
 * <li>SMALL_AMOUNT_SPECIAL：青色申告者の少額減価償却資産の特例（取得価額30万円未満）。使用開始年に全額を償却。</li>
 * <li>LUMP_SUM_3Y：一括償却資産（取得価額10万円以上20万円未満）。使用開始年から3年間、月数に関係なく3分の1ずつ償却（最終年に端数調整）。</li>
 * </ul>
 * 事業専用割合は償却費に乗じて1円未満切捨てとし、残額を家事分（事業主貸）とする。
 */
public final class DepreciationCalculator {
    public static final String STRAIGHT_LINE = "STRAIGHT_LINE";
    public static final String SMALL_AMOUNT_SPECIAL = "SMALL_AMOUNT_SPECIAL";
    public static final String LUMP_SUM_3Y = "LUMP_SUM_3Y";
    public static final BigDecimal SMALL_AMOUNT_LIMIT = new BigDecimal("300000");
    public static final BigDecimal SMALL_AMOUNT_ANNUAL_CAP = new BigDecimal("3000000");
    public static final BigDecimal LUMP_SUM_MIN = new BigDecimal("100000");
    public static final BigDecimal LUMP_SUM_LIMIT = new BigDecimal("200000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private DepreciationCalculator() {
    }

    public static BigDecimal straightLineRate(int usefulLife) {
        return BigDecimal.ONE.divide(BigDecimal.valueOf(usefulLife), 3, RoundingMode.CEILING);
    }

    public static YearResult forYear(FixedAsset asset, int year) {
        BigDecimal cost = asset.getAcquisitionCost();
        LocalDate start = asset.getServiceStartDate() != null ? asset.getServiceStartDate() : asset.getAcquisitionDate();
        BigDecimal ratio = asset.getBusinessRatio() == null ? HUNDRED : asset.getBusinessRatio();
        BigDecimal opening = cost;
        YearResult result = null;
        for (int y = start.getYear(); y <= year; y++) {
            BigDecimal depreciation = depreciationOf(asset, cost, start, y, opening);
            int months = y == start.getYear() ? 12 - start.getMonthValue() + 1 : 12;
            if (y == year) {
                BigDecimal business = depreciation.multiply(ratio).divide(HUNDRED, 0, RoundingMode.DOWN);
                result = new YearResult(opening, depreciation, business, depreciation.subtract(business),
                        opening.subtract(depreciation), months, rateOf(asset));
            }
            opening = opening.subtract(depreciation);
        }
        if (result == null) {
            return new YearResult(cost, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, cost, 0, rateOf(asset));
        }
        return result;
    }

    /** 指定年の1月1日時点の未償却残高（前年末までの償却を控除）。使用開始前は取得価額。 */
    public static BigDecimal bookValueAtStartOf(FixedAsset asset, int year) {
        return forYear(asset, year).opening();
    }

    private static BigDecimal depreciationOf(FixedAsset asset, BigDecimal cost, LocalDate start, int year, BigDecimal opening) {
        String method = asset.getDepreciationMethod();
        if (SMALL_AMOUNT_SPECIAL.equals(method)) {
            return year == start.getYear() ? cost : BigDecimal.ZERO;
        }
        if (LUMP_SUM_3Y.equals(method)) {
            int index = year - start.getYear();
            if (index < 0 || index > 2) {
                return BigDecimal.ZERO;
            }
            BigDecimal third = cost.divide(BigDecimal.valueOf(3), 0, RoundingMode.DOWN);
            return index == 2 ? opening : third;
        }
        if (opening.compareTo(BigDecimal.ONE) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal annual = cost.multiply(straightLineRate(asset.getUsefulLife()));
        int months = year == start.getYear() ? 12 - start.getMonthValue() + 1 : 12;
        BigDecimal amount = annual.multiply(BigDecimal.valueOf(months)).divide(TWELVE, 0, RoundingMode.CEILING);
        BigDecimal max = opening.subtract(BigDecimal.ONE);
        return amount.min(max).max(BigDecimal.ZERO);
    }

    private static BigDecimal rateOf(FixedAsset asset) {
        if (STRAIGHT_LINE.equals(asset.getDepreciationMethod()) && asset.getUsefulLife() != null && asset.getUsefulLife() > 0) {
            return straightLineRate(asset.getUsefulLife());
        }
        return null;
    }

    /**
     * @param opening 期首未償却残高
     * @param depreciation 本年分の償却費合計
     * @param businessPortion 本年分の必要経費算入額
     * @param privatePortion 家事分（事業主貸）
     * @param closing 期末未償却残高
     * @param months 本年中の償却期間（月数）
     * @param rate 定額法の償却率（その他の方法は null）
     */
    public record YearResult(BigDecimal opening, BigDecimal depreciation, BigDecimal businessPortion,
            BigDecimal privatePortion, BigDecimal closing, int months, BigDecimal rate) {
    }
}
