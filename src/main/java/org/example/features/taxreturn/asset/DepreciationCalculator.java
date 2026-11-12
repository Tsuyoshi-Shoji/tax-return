package org.example.features.taxreturn.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;

/** 固定資産の取得条件と登録済みライフサイクルイベントから年分別償却を再計算する。 */
public final class DepreciationCalculator {
    public static final String STRAIGHT_LINE = "STRAIGHT_LINE";
    public static final String DECLINING_200 = "DECLINING_200";
    public static final String DECLINING_250 = "DECLINING_250";
    public static final String OLD_STRAIGHT_LINE = "OLD_STRAIGHT_LINE";
    public static final String OLD_DECLINING = "OLD_DECLINING";
    public static final String SMALL_AMOUNT_SPECIAL = "SMALL_AMOUNT_SPECIAL";
    public static final String LUMP_SUM_3Y = "LUMP_SUM_3Y";
    public static final BigDecimal LUMP_SUM_MIN = new BigDecimal("100000");
    public static final BigDecimal LUMP_SUM_LIMIT = new BigDecimal("200000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private DepreciationCalculator() {
    }

    public static BigDecimal straightLineRate(int usefulLife) {
        return DepreciationRates.straight(usefulLife);
    }

    public static boolean isRegular(String method) {
        return method != null && Set.of(STRAIGHT_LINE, DECLINING_200, DECLINING_250,
                OLD_STRAIGHT_LINE, OLD_DECLINING).contains(method);
    }

    public static boolean isOld(String method) {
        return OLD_STRAIGHT_LINE.equals(method) || OLD_DECLINING.equals(method);
    }

    public static boolean isDeclining(String method) {
        return OLD_DECLINING.equals(method) || DECLINING_200.equals(method) || DECLINING_250.equals(method);
    }

    public static BigDecimal privateUseDecrease(FixedAsset asset) {
        if (!asset.isPrivateUseConversion()) {
            return BigDecimal.ZERO;
        }
        LocalDate acquired = asset.getAcquisitionDate();
        LocalDate start = asset.getServiceStartDate();
        int years = Math.toIntExact(ChronoUnit.YEARS.between(acquired, start));
        if (!start.isBefore(acquired.plusYears(years).plusMonths(6))) {
            years++;
        }
        int privateLife = asset.getStatutoryUsefulLife() * 3 / 2;
        BigDecimal cost = asset.getAcquisitionCost();
        BigDecimal decrease = cost.multiply(new BigDecimal("0.9")).multiply(DepreciationRates.oldStraight(privateLife))
                .multiply(BigDecimal.valueOf(years)).setScale(0, RoundingMode.CEILING);
        return decrease.min(cost.multiply(new BigDecimal("0.95")).setScale(0, RoundingMode.DOWN));
    }

    public static BigDecimal initialBookValue(FixedAsset asset) {
        return asset.getAcquisitionCost().subtract(privateUseDecrease(asset));
    }

    public static YearResult forYear(FixedAsset asset, int year) {
        return forYear(asset, year, asset.getBusinessRatio());
    }

    public static YearResult forYear(FixedAsset asset, int year, BigDecimal businessRatio) {
        return calculate(asset, year, businessRatio, null,
                asset.getAcquisitionCost().divide(BigDecimal.valueOf(3), 0, RoundingMode.CEILING));
    }

    public static YearResult forYear(FixedAsset asset, int year, BigDecimal businessRatio,
            List<FixedAssetEvent> events, List<FixedAsset> assets) {
        BigDecimal third = BigDecimal.ZERO;
        if (LUMP_SUM_3Y.equals(asset.getDepreciationMethod())) {
            BigDecimal prefix = BigDecimal.ZERO;
            // 同じ供用年の一括償却合計を先に丸め、ID順で端数を配分する。
            for (FixedAsset member : assets.stream()
                    .filter(a -> LUMP_SUM_3Y.equals(a.getDepreciationMethod()))
                    .filter(a -> a.getServiceStartDate().getYear() == asset.getServiceStartDate().getYear())
                    .sorted(Comparator.comparing(FixedAsset::getId)).toList()) {
                BigDecimal next = prefix.add(member.getAcquisitionCost());
                if (member.getId().equals(asset.getId())) {
                    third = next.divide(BigDecimal.valueOf(3), 0, RoundingMode.CEILING)
                            .subtract(prefix.divide(BigDecimal.valueOf(3), 0, RoundingMode.CEILING));
                    break;
                }
                prefix = next;
            }
        }
        LocalDate disposal = DepreciationRules.disposalDate(asset, events);
        FixedAssetEvent methodChange = events.stream()
                .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.METHOD_CHANGE.equals(e.getEventType()))
                .filter(e -> !e.getEffectiveDate().isAfter(LocalDate.of(year, 12, 31)))
                .max(Comparator.comparing(FixedAssetEvent::getEffectiveDate)).orElse(null);
        FixedAssetEvent transfer = events.stream()
                .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.INHERITANCE.equals(e.getEventType())
                        || FixedAssetEvent.GIFT.equals(e.getEventType()))
                .min(Comparator.comparing(FixedAssetEvent::getEffectiveDate)).orElse(null);
        if (methodChange != null && transfer != null) {
            throw new IllegalArgumentException("A method change and ownership transfer cannot be combined");
        }
        if (methodChange != null && year >= methodChange.getEffectiveDate().getYear()) {
            FixedAsset changed = new FixedAsset();
            changed.setAssetName(asset.getAssetName());
            changed.setAccountCode(asset.getAccountCode());
            changed.setAcquisitionDate(methodChange.getEffectiveDate());
            changed.setServiceStartDate(methodChange.getEffectiveDate());
            changed.setAcquisitionCost(methodChange.getOpeningBookValue());
            changed.setUsefulLife(asset.getUsefulLife());
            changed.setDepreciationMethod(methodChange.getNewDepreciationMethod());
            changed.setBusinessRatio(asset.getBusinessRatio());
            LocalDate changedDisposal = disposal != null && !disposal.isBefore(methodChange.getEffectiveDate())
                    ? disposal : null;
            return calculate(changed, year, businessRatio, changedDisposal, third,
                    methodChange.getEffectiveDate(), methodChange.getOpeningBookValue(), null);
        }
        if (transfer != null) {
            if (year < transfer.getEffectiveDate().getYear()) {
                return notInService(asset, year);
            }
            BigDecimal revisedBase = null;
            if (transfer.getEffectiveDate().getYear() > asset.getServiceStartDate().getYear()) {
                YearResult history = calculate(asset, transfer.getEffectiveDate().getYear() - 1,
                        businessRatio, null, third);
                if ("OLD_FIVE_YEAR".equals(history.phase())) {
                    throw new IllegalArgumentException("Transfer during the old-method five-year tail requires a detailed history");
                }
                revisedBase = history.revisedAcquisitionCost();
            }
            LocalDate transferDisposal = disposal != null && !disposal.isBefore(transfer.getEffectiveDate())
                    ? disposal : null;
            return calculate(asset, year, businessRatio, transferDisposal, third,
                    transfer.getEffectiveDate(), transfer.getOpeningBookValue(), revisedBase);
        }
        return calculate(asset, year, businessRatio, disposal, third);
    }

    private static YearResult calculate(FixedAsset asset, int year, BigDecimal businessRatio,
            LocalDate disposal, BigDecimal third) {
        return calculate(asset, year, businessRatio, disposal, third, null, null, null);
    }

    private static YearResult calculate(FixedAsset asset, int year, BigDecimal businessRatio,
            LocalDate disposal, BigDecimal third, LocalDate startOverride, BigDecimal openingOverride,
            BigDecimal revisedBaseOverride) {
        BigDecimal cost = asset.getAcquisitionCost();
        LocalDate start = startOverride != null ? startOverride
                : asset.getServiceStartDate() != null ? asset.getServiceStartDate() : asset.getAcquisitionDate();
        BigDecimal ratio = businessRatio == null ? HUNDRED : businessRatio;
        BigDecimal opening = openingOverride == null ? initialBookValue(asset) : openingOverride;
        String method = asset.getDepreciationMethod();
        BigDecimal residual = "SOFTWARE_ASSET".equals(asset.getAccountCode()) ? BigDecimal.ZERO : BigDecimal.ONE;
        BigDecimal oldThreshold = "SOFTWARE_ASSET".equals(asset.getAccountCode()) ? BigDecimal.ZERO
                : cost.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.CEILING);
        BigDecimal revisedBase = revisedBaseOverride;
        DepreciationRates.Declining declining = DECLINING_200.equals(method) || DECLINING_250.equals(method)
                ? DepreciationRates.declining(asset.getUsefulLife(), DECLINING_250.equals(method)) : null;
        BigDecimal guarantee = declining == null ? null : cost.multiply(declining.guaranteeRate());
        YearResult result = null;
        for (int y = start.getYear(); y <= year; y++) {
            int firstMonth = y == start.getYear() ? start.getMonthValue() : 1;
            int lastMonth = disposal != null && disposal.getYear() == y
                    && isRegular(method) ? disposal.getMonthValue() : 12;
            int months = lastMonth - firstMonth + 1;
            boolean disposedEarlier = disposal != null && disposal.getYear() < y
                    && !DepreciationRules.retainsBalanceAfterDisposal(asset);
            if (disposedEarlier) {
                opening = BigDecimal.ZERO;
                months = 0;
            }
            BigDecimal depreciation;
            BigDecimal rate = rateOf(asset);
            String phase = "NORMAL";
            if (disposedEarlier) {
                depreciation = BigDecimal.ZERO;
                phase = "DISPOSED";
            } else if (SMALL_AMOUNT_SPECIAL.equals(method)) {
                depreciation = y == start.getYear() ? cost : BigDecimal.ZERO;
                phase = "SMALL_AMOUNT";
            } else if (LUMP_SUM_3Y.equals(method)) {
                int index = y - start.getYear();
                depreciation = index > 2 ? BigDecimal.ZERO : (index == 2 ? opening : third);
                phase = "LUMP_SUM";
            } else if (isRegular(method)) {
                BigDecimal annual;
                BigDecimal minimum = residual;
                if (isOld(method)) {
                    if (oldThreshold.signum() > 0 && opening.compareTo(oldThreshold) <= 0) {
                        // 95％到達の翌年から。2007年以前に到達済みなら2008年から開始する。
                        annual = y >= 2008 ? oldThreshold.subtract(BigDecimal.ONE).divide(BigDecimal.valueOf(5))
                                : BigDecimal.ZERO;
                        rate = null;
                        phase = "OLD_FIVE_YEAR";
                    } else {
                        annual = OLD_DECLINING.equals(method) ? opening.multiply(rate)
                                : cost.multiply("SOFTWARE_ASSET".equals(asset.getAccountCode())
                                        ? BigDecimal.ONE : new BigDecimal("0.9")).multiply(rate);
                        minimum = oldThreshold;
                    }
                } else if (declining != null) {
                    annual = opening.multiply(declining.rate());
                    if (revisedBase == null && annual.setScale(0, RoundingMode.CEILING).compareTo(guarantee) < 0) {
                        revisedBase = opening;
                    }
                    if (revisedBase != null) {
                        annual = revisedBase.multiply(declining.revisedRate());
                        rate = declining.revisedRate();
                        phase = "REVISED";
                    }
                } else {
                    annual = cost.multiply(rate);
                }
                BigDecimal amount = annual.multiply(BigDecimal.valueOf(months)).divide(TWELVE, 0, RoundingMode.CEILING);
                depreciation = amount.min(opening.subtract(minimum).max(BigDecimal.ZERO));
            } else {
                throw new IllegalArgumentException("Unsupported depreciation method: " + method);
            }
            if (y == year) {
                BigDecimal business = depreciation.multiply(ratio).divide(HUNDRED, 0, RoundingMode.DOWN);
                result = new YearResult(opening, depreciation, business, depreciation.subtract(business),
                        opening.subtract(depreciation), months, rate, revisedBase, guarantee, phase);
            }
            opening = opening.subtract(depreciation);
        }
        if (result == null) {
            return notInService(asset, year);
        }
        return result;
    }

    private static YearResult notInService(FixedAsset asset, int year) {
        BigDecimal beforeService = asset.isPrivateUseConversion() ? BigDecimal.ZERO : asset.getAcquisitionCost();
        BigDecimal guarantee = null;
        if (DECLINING_200.equals(asset.getDepreciationMethod())
                || DECLINING_250.equals(asset.getDepreciationMethod())) {
            guarantee = asset.getAcquisitionCost().multiply(DepreciationRates
                    .declining(asset.getUsefulLife(), DECLINING_250.equals(asset.getDepreciationMethod()))
                    .guaranteeRate());
        }
        return new YearResult(beforeService, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, beforeService,
                0, rateOf(asset), null, guarantee, "NOT_IN_SERVICE");
    }

    /** 指定年の1月1日時点の未償却残高（前年末までの償却を控除）。使用開始前は取得価額。 */
    public static BigDecimal bookValueAtStartOf(FixedAsset asset, int year) {
        return forYear(asset, year).opening();
    }

    private static BigDecimal rateOf(FixedAsset asset) {
        return switch (asset.getDepreciationMethod()) {
            case STRAIGHT_LINE -> DepreciationRates.straight(asset.getUsefulLife());
            case OLD_STRAIGHT_LINE -> DepreciationRates.oldStraight(asset.getUsefulLife());
            case OLD_DECLINING -> DepreciationRates.oldDeclining(asset.getUsefulLife());
            case DECLINING_200 -> DepreciationRates.declining(asset.getUsefulLife(), false).rate();
            case DECLINING_250 -> DepreciationRates.declining(asset.getUsefulLife(), true).rate();
            default -> null;
        };
    }

    /**
     * @param opening 期首未償却残高
     * @param depreciation 本年分の償却費合計
     * @param businessPortion 本年分の必要経費算入額
     * @param privatePortion 家事分（事業主貸）
     * @param closing 期末未償却残高
     * @param months 本年中の償却期間（月数）
     * @param rate 適用償却率（改定償却率への切替後は改定率、5年均等等は null）
     */
    public record YearResult(BigDecimal opening, BigDecimal depreciation, BigDecimal businessPortion,
            BigDecimal privatePortion, BigDecimal closing, int months, BigDecimal rate,
            BigDecimal revisedAcquisitionCost, BigDecimal guaranteeAmount, String phase) {
    }
}
