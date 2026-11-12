package org.example.features.taxreturn.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;
import org.example.entity.TaxReturnInput;

/** 所得税の年分別ルール。利用者確認は法的適格性の証明ではない。 */
public final class DepreciationRules {
    public static final String REVISION = "2026-11-12-v3";
    public static final String VERIFIED_ON = "2026-11-12";
    public static final String SOURCE = "https://www.nta.go.jp/taxes/shiraberu/taxanswer/shotoku/2100.htm";
    public static final Set<String> METHOD_BASES = Set.of("STATUTORY", "NOTIFIED", "APPROVED", "MIXED");
    private static final BigDecimal MIN = new BigDecimal("100000");
    public static final String USED_SIMPLIFIED = "USED_SIMPLIFIED";
    public static final Set<String> METHODS = Set.of(DepreciationCalculator.STRAIGHT_LINE,
            DepreciationCalculator.DECLINING_200, DepreciationCalculator.DECLINING_250,
            DepreciationCalculator.OLD_STRAIGHT_LINE, DepreciationCalculator.OLD_DECLINING,
            DepreciationCalculator.SMALL_AMOUNT_SPECIAL, DepreciationCalculator.LUMP_SUM_3Y);

    private DepreciationRules() {}

    public static boolean supported(int year) {
        return year >= 2024 && year <= 2026;
    }

    public static String version(int year) {
        return supported(year) ? year + "-" + REVISION : null;
    }

    public static BigDecimal smallAmountLimit(LocalDate acquired) {
        return acquired.isBefore(LocalDate.of(2026, 4, 1))
                ? new BigDecimal("300000") : new BigDecimal("400000");
    }

    public static BigDecimal annualCap(int businessMonths) {
        if (businessMonths < 1 || businessMonths > 12) {
            throw new IllegalArgumentException("Business months must be between 1 and 12");
        }
        return new BigDecimal("3000000").multiply(BigDecimal.valueOf(businessMonths))
                .divide(BigDecimal.valueOf(12));
    }

    public static List<String> assetErrors(FixedAsset asset) {
        List<String> errors = new ArrayList<>();
        String method = asset.getDepreciationMethod();
        BigDecimal cost = asset.getAcquisitionCost();
        LocalDate acquired = asset.getAcquisitionDate();
        boolean oldDate = acquired.isBefore(LocalDate.of(2007, 4, 1));
        if (DepreciationCalculator.isRegular(method)) {
            if (acquired.isBefore(LocalDate.of(1953, 1, 1))) {
                errors.add("1952年以前取得の評価・償却履歴は未対応です。");
            }
            if (asset.getUsefulLife() == null || asset.getUsefulLife() < 2 || asset.getUsefulLife() > 50) {
                errors.add("償却計算の耐用年数は2〜50年が対象です。");
            }
            if (oldDate != DepreciationCalculator.isOld(method)) {
                errors.add("取得日と旧方式／新方式の区分が一致しません（境界：2007年4月1日）。");
            }
            if (DepreciationCalculator.DECLINING_200.equals(method) && acquired.isBefore(LocalDate.of(2012, 4, 1))) {
                errors.add("200％定率法は2012年4月1日以後取得が対象です。250％から200％への移行計算は未対応です。");
            }
            boolean transition = !acquired.isBefore(LocalDate.of(2012, 4, 1))
                    && !acquired.isAfter(LocalDate.of(2012, 12, 31));
            if (DepreciationCalculator.DECLINING_250.equals(method)
                    && !acquired.isBefore(LocalDate.of(2012, 4, 1))
                    && !(transition && asset.isRateTransitionConfirmed())) {
                errors.add("250％定率法は2012年3月31日以前取得、又は2012年中の経過措置確認済み資産に限ります。");
            }
            if (asset.isRateTransitionConfirmed()
                    && !(DepreciationCalculator.DECLINING_250.equals(method) && transition)) {
                errors.add("250％継続の経過措置確認は2012年4月〜12月取得の250％定率法だけに指定してください。");
            }
            if (DepreciationCalculator.isDeclining(method)
                    && ("SOFTWARE_ASSET".equals(asset.getAccountCode())
                        || ("BUILDING_IMPROVEMENTS".equals(asset.getAccountCode())
                            && !acquired.isBefore(LocalDate.of(2016, 4, 1))))) {
                errors.add("ソフトウェア、2016年4月1日以後取得の建物附属設備は定率法を選択できません。");
            }
        } else if (DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(method)) {
            if (!supported(asset.getServiceStartDate().getYear())
                    || asset.getAcquisitionDate().isBefore(LocalDate.of(2006, 4, 1))) {
                errors.add("少額特例の供用年は2024〜2026年分が対象です。");
            }
            if (cost.compareTo(MIN) < 0 || cost.compareTo(smallAmountLimit(asset.getAcquisitionDate())) >= 0) {
                errors.add("少額特例は10万円以上、取得日に応じて30万円未満／40万円未満です。");
            }
        } else if (DepreciationCalculator.LUMP_SUM_3Y.equals(method)) {
            if (cost.compareTo(MIN) < 0 || cost.compareTo(new BigDecimal("200000")) >= 0) {
                errors.add("一括償却資産は10万円以上20万円未満です。");
            }
        } else {
            errors.add("未対応の償却方法です。");
        }
        if (!Set.of("STATUTORY", USED_SIMPLIFIED).contains(asset.getLifeBasis())) {
            errors.add("耐用年数の根拠が不正です。");
        }
        if (USED_SIMPLIFIED.equals(asset.getLifeBasis())) {
            Integer legalLife = asset.getStatutoryUsefulLife();
            if (!DepreciationCalculator.isRegular(method) || "SOFTWARE_ASSET".equals(asset.getAccountCode())
                    || legalLife == null || legalLife < 2 || legalLife > 50
                    || asset.getUsedElapsedMonths() == null || asset.getUsedElapsedMonths() < 0
                    || asset.getUsedElapsedMonths() > 2400 || !asset.isUsedRequirementsConfirmed()) {
                errors.add("中古簡便法は有形資産・法定耐用年数2〜50年・取得時経過月数0〜2400・要件確認が必要です。");
            } else if (asset.getUsefulLife() != usedUsefulLife(legalLife, asset.getUsedElapsedMonths())) {
                errors.add("中古簡便法の計算耐用年数が入力根拠と一致しません。");
            }
        } else if (asset.getUsedElapsedMonths() != null || asset.isUsedRequirementsConfirmed()) {
            errors.add("経過月数・中古要件確認は中古簡便法の場合だけ指定してください。");
        }
        if (asset.isPrivateUseConversion()) {
            if (!DepreciationCalculator.isRegular(method) || "SOFTWARE_ASSET".equals(asset.getAccountCode())
                    || asset.getStatutoryUsefulLife() == null || asset.getStatutoryUsefulLife() < 2
                    || asset.getStatutoryUsefulLife() > 50 || acquired.isBefore(LocalDate.of(1953, 1, 1))
                    || !asset.getServiceStartDate().isAfter(acquired)
                    || !"OWNER_FUNDS".equals(asset.getAcquisitionFunding())) {
                errors.add("私用転用は1953年以後取得の有形資産・法定耐用年数・取得日より後の供用日・事業主借が必要です。少額／一括償却は対象外です。");
            }
        }
        if (asset.getServiceStartDate().isBefore(asset.getAcquisitionDate())) {
            errors.add("供用開始日は取得日以降である必要があります。");
        }
        return errors;
    }

    public static int usedUsefulLife(int legalLife, int elapsedMonths) {
        if (legalLife < 2 || legalLife > 50 || elapsedMonths < 0 || elapsedMonths > 2400) {
            throw new IllegalArgumentException("Invalid used-asset life inputs");
        }
        BigDecimal months = elapsedMonths >= legalLife * 12
                ? BigDecimal.valueOf(legalLife * 12L).multiply(new BigDecimal("0.2"))
                : BigDecimal.valueOf(legalLife * 12L).subtract(BigDecimal.valueOf(elapsedMonths)
                        .multiply(new BigDecimal("0.8")));
        return Math.max(2, months.divide(BigDecimal.valueOf(12), 0, RoundingMode.DOWN).intValueExact());
    }

    public static LocalDate recognitionDate(FixedAsset asset) {
        return asset.isPrivateUseConversion() ? asset.getServiceStartDate() : asset.getAcquisitionDate();
    }

    public static LocalDate recognitionDate(FixedAsset asset, List<FixedAssetEvent> events) {
        return events.stream()
                .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.INHERITANCE.equals(e.getEventType())
                        || FixedAssetEvent.GIFT.equals(e.getEventType()))
                .map(FixedAssetEvent::getEffectiveDate)
                .min(LocalDate::compareTo)
                .orElseGet(() -> asset.isPrivateUseConversion() ? asset.getServiceStartDate() : asset.getAcquisitionDate());
    }

    public static String calculationDetails(FixedAsset asset, DepreciationCalculator.YearResult result) {
        String details = "耐用年数根拠=" + asset.getLifeBasis() + "、法定年数=" + asset.getStatutoryUsefulLife();
        if (USED_SIMPLIFIED.equals(asset.getLifeBasis())) {
            details += "、取得時経過月数=" + asset.getUsedElapsedMonths() + "、計算年数=" + asset.getUsefulLife();
        }
        if (asset.isPrivateUseConversion()) {
            details += "、非業務用減価=" + DepreciationCalculator.privateUseDecrease(asset).toPlainString()
                    + "、供用開始時簿価=" + DepreciationCalculator.initialBookValue(asset).toPlainString();
        }
        details += "、計算段階=" + result.phase();
        if (result.revisedAcquisitionCost() != null) {
            details += "、改定取得価額=" + result.revisedAcquisitionCost().toPlainString();
        }
        if (result.guaranteeAmount() != null) {
            details += "、償却保証額=" + result.guaranteeAmount().toPlainString();
        }
        return details;
    }

    public static String methodForYear(FixedAsset asset, int year, List<FixedAssetEvent> events) {
        return events.stream()
                .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.METHOD_CHANGE.equals(e.getEventType()))
                .filter(e -> e.getEffectiveDate().getYear() <= year)
                .max(java.util.Comparator.comparing(FixedAssetEvent::getEffectiveDate))
                .map(FixedAssetEvent::getNewDepreciationMethod)
                .orElse(asset.getDepreciationMethod());
    }

    public static BigDecimal depreciationBasis(FixedAsset asset, DepreciationCalculator.YearResult result) {
        if ("OLD_FIVE_YEAR".equals(result.phase())) {
            return asset.getAcquisitionCost().multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.CEILING)
                    .subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
        }
        if (DepreciationCalculator.OLD_STRAIGHT_LINE.equals(asset.getDepreciationMethod())
                && !"SOFTWARE_ASSET".equals(asset.getAccountCode())) {
            return asset.getAcquisitionCost().multiply(new BigDecimal("0.9"));
        }
        if (DepreciationCalculator.isDeclining(asset.getDepreciationMethod())) {
            return result.revisedAcquisitionCost() == null ? result.opening() : result.revisedAcquisitionCost();
        }
        return asset.getAcquisitionCost();
    }

    public static BigDecimal depreciationBasis(FixedAsset asset, DepreciationCalculator.YearResult result,
            int year, List<FixedAssetEvent> events) {
        FixedAssetEvent change = events.stream()
                .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.METHOD_CHANGE.equals(e.getEventType())
                        && e.getEffectiveDate().getYear() <= year)
                .max(java.util.Comparator.comparing(FixedAssetEvent::getEffectiveDate)).orElse(null);
        if (change == null) {
            return depreciationBasis(asset, result);
        }
        String method = change.getNewDepreciationMethod();
        if (DepreciationCalculator.isDeclining(method)) {
            return result.revisedAcquisitionCost() == null ? result.opening() : result.revisedAcquisitionCost();
        }
        return change.getOpeningBookValue();
    }

    public static LocalDate disposalDate(FixedAsset asset, List<FixedAssetEvent> events) {
        return events.stream().filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                .filter(e -> FixedAssetEvent.SALE.equals(e.getEventType()) || FixedAssetEvent.DISPOSAL.equals(e.getEventType())
                        || FixedAssetEvent.BUSINESS_CESSATION.equals(e.getEventType()))
                .map(FixedAssetEvent::getEffectiveDate).min(LocalDate::compareTo).orElse(null);
    }

    public static boolean retainsBalanceAfterDisposal(FixedAsset asset) {
        return DepreciationCalculator.LUMP_SUM_3Y.equals(asset.getDepreciationMethod());
    }

    public static boolean confirmationMatches(int year, List<FixedAsset> assets, TaxReturnInput input) {
        return confirmationMatches(year, assets, input, List.of());
    }

    public static boolean confirmationMatches(int year, List<FixedAsset> assets, TaxReturnInput input,
            List<FixedAssetEvent> events) {
        if (input == null || !supported(year)) {
            return false;
        }
        List<FixedAssetEvent> eventsThroughYear = events.stream()
                .filter(e -> !e.getEffectiveDate().isAfter(LocalDate.of(year, 12, 31))).toList();
        if (version(year).equals(input.getDepreciationRuleVersion())) {
            return true;
        }
        String previousVersion = year + "-2026-04-01-v2";
        if (previousVersion.equals(input.getDepreciationRuleVersion())) {
            return assets.stream().filter(a -> recognitionDate(a, eventsThroughYear).getYear() <= year)
                    .allMatch(a -> !"CAPITAL_IMPROVEMENT".equals(a.getAssetOrigin()))
                    && eventsThroughYear.stream().noneMatch(e -> Set.of(FixedAssetEvent.INHERITANCE, FixedAssetEvent.GIFT,
                            FixedAssetEvent.METHOD_CHANGE, FixedAssetEvent.BUSINESS_CESSATION).contains(e.getEventType()));
        }
        // 変更のない従来3方式は、確定年度を含め旧確認を無効にしない。
        return (year + "-2026-04-01-v1").equals(input.getDepreciationRuleVersion())
                && assets.stream().filter(a -> recognitionDate(a, eventsThroughYear).getYear() <= year).allMatch(a ->
                    Set.of(DepreciationCalculator.STRAIGHT_LINE, DepreciationCalculator.SMALL_AMOUNT_SPECIAL,
                            DepreciationCalculator.LUMP_SUM_3Y).contains(a.getDepreciationMethod())
                    && "STATUTORY".equals(a.getLifeBasis()) && !a.isPrivateUseConversion()
                    && !a.isRateTransitionConfirmed() && a.getUsedElapsedMonths() == null
                    && !a.isUsedRequirementsConfirmed()
                    && !"CAPITAL_IMPROVEMENT".equals(a.getAssetOrigin())
                    && eventsThroughYear.stream().noneMatch(e -> a.getId().equals(e.getFixedAsset().getId())
                            && Set.of(FixedAssetEvent.INHERITANCE, FixedAssetEvent.GIFT,
                                    FixedAssetEvent.METHOD_CHANGE, FixedAssetEvent.BUSINESS_CESSATION)
                                    .contains(e.getEventType())));
    }

    public static List<String> annualErrors(int year, List<FixedAsset> assets, TaxReturnInput input) {
        return annualErrors(year, assets, input, List.of());
    }

    public static List<String> annualErrors(int year, List<FixedAsset> assets, TaxReturnInput input,
            List<FixedAssetEvent> events) {
        List<FixedAssetEvent> eventsThroughYear = events.stream()
                .filter(e -> !e.getEffectiveDate().isAfter(LocalDate.of(year, 12, 31))).toList();
        List<FixedAsset> relevant = assets.stream()
                .filter(a -> recognitionDate(a, eventsThroughYear).getYear() <= year).toList();
        if (relevant.isEmpty()) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        if (!supported(year)) {
            errors.add("減価償却ルールは2024〜2026年分のみ対応しています。");
            return errors;
        }
        if (!confirmationMatches(year, relevant, input, eventsThroughYear)
                || !METHOD_BASES.contains(input.getDepreciationMethodBasis())) {
            errors.add("対象年分の減価償却の対象範囲・届出／承認要件を利用者が確認してください。");
        }
        BigDecimal specialTotal = BigDecimal.ZERO;
        for (FixedAsset asset : relevant) {
            for (String error : assetErrors(asset)) {
                errors.add("固定資産「" + asset.getAssetName() + "」：" + error);
            }
            List<FixedAssetEvent> assetEvents = eventsThroughYear.stream()
                    .filter(e -> asset.getId().equals(e.getFixedAsset().getId()))
                    .toList();
            List<FixedAssetEvent> transfers = assetEvents.stream()
                    .filter(e -> FixedAssetEvent.INHERITANCE.equals(e.getEventType())
                            || FixedAssetEvent.GIFT.equals(e.getEventType())).toList();
            List<FixedAssetEvent> methodChanges = assetEvents.stream()
                    .filter(e -> FixedAssetEvent.METHOD_CHANGE.equals(e.getEventType())).toList();
            List<FixedAssetEvent> cessations = assetEvents.stream()
                    .filter(e -> FixedAssetEvent.BUSINESS_CESSATION.equals(e.getEventType())).toList();
            if (transfers.size() > 1 || methodChanges.size() > 1 || cessations.size() > 1) {
                errors.add("固定資産「" + asset.getAssetName() + "」：移転・方法変更・廃業イベントは各1件までです。");
            }
            if (!transfers.isEmpty() && !methodChanges.isEmpty()) {
                errors.add("固定資産「" + asset.getAssetName() + "」：相続・贈与と途中の償却方法変更の併用履歴は未対応です。");
            }
            if (!transfers.isEmpty() && asset.isPrivateUseConversion()) {
                errors.add("固定資産「" + asset.getAssetName() + "」：相続・贈与と非業務用からの転用の併用履歴は未対応です。");
            }
            for (FixedAssetEvent event : transfers) {
                if (!event.isHistoryConfirmed() || event.getOpeningBookValue() == null
                        || event.getOpeningBookValue().signum() < 0
                        || event.getOpeningBookValue().compareTo(asset.getAcquisitionCost()) > 0
                        || event.getEffectiveDate().isBefore(asset.getServiceStartDate())
                        || !isEvidencePresent(event)) {
                    errors.add("固定資産「" + asset.getAssetName() + "」：相続・贈与では引継ぎ日簿価・承継履歴・確認根拠が必要です。");
                }
                if (!methodChanges.isEmpty()) {
                    errors.add("固定資産「" + asset.getAssetName() + "」：相続・贈与と方法変更の併用は未対応です。");
                }
                if (DepreciationCalculator.isOld(asset.getDepreciationMethod())
                        && event.getEffectiveDate().getYear() > asset.getServiceStartDate().getYear()) {
                    DepreciationCalculator.YearResult beforeTransfer = DepreciationCalculator.forYear(asset,
                            event.getEffectiveDate().getYear() - 1);
                    if ("OLD_FIVE_YEAR".equals(beforeTransfer.phase())) {
                        errors.add("固定資産「" + asset.getAssetName()
                                + "」：旧方式の5年均等償却中の相続・贈与は残り年数の履歴が未対応です。");
                    }
                }
            }
            for (FixedAssetEvent event : methodChanges) {
                if (!event.isHistoryConfirmed() || event.getOpeningBookValue() == null
                        || event.getOpeningBookValue().signum() < 0
                        || event.getOpeningBookValue().compareTo(asset.getAcquisitionCost()) > 0
                        || event.getNewDepreciationMethod() == null
                        || !Set.of(DepreciationCalculator.STRAIGHT_LINE, DepreciationCalculator.DECLINING_200,
                                DepreciationCalculator.DECLINING_250).contains(event.getNewDepreciationMethod())
                        || !DepreciationCalculator.isRegular(asset.getDepreciationMethod())
                        || event.getNewDepreciationMethod().equals(asset.getDepreciationMethod())
                        || !event.getEffectiveDate().equals(LocalDate.of(event.getEffectiveDate().getYear(), 1, 1))
                        || !event.getEffectiveDate().isAfter(asset.getServiceStartDate())
                        || !isEvidencePresent(event)) {
                    errors.add("固定資産「" + asset.getAssetName() + "」：償却方法変更には年初の適用日・変更後方法・確認済み期首簿価・承認根拠が必要です。");
                }
            }
            for (FixedAssetEvent event : cessations) {
                if (DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(asset.getDepreciationMethod())
                        || DepreciationCalculator.LUMP_SUM_3Y.equals(asset.getDepreciationMethod())
                        || event.getEffectiveDate().isBefore(asset.getServiceStartDate())) {
                    errors.add("固定資産「" + asset.getAssetName() + "」：廃業日までの月割りは通常償却資産のみ対応し、残存簿価の処理は別途確認が必要です。");
                }
            }
            if (!cessations.isEmpty() && cessations.stream().anyMatch(e ->
                    assetEvents.stream().anyMatch(other -> other.getEffectiveDate().isAfter(e.getEffectiveDate())
                            && (FixedAssetEvent.SALE.equals(other.getEventType())
                                || FixedAssetEvent.DISPOSAL.equals(other.getEventType()))))) {
                errors.add("固定資産「" + asset.getAssetName() + "」：廃業後の売却・除却イベントは登録できません。");
            }
            if (DepreciationCalculator.isDeclining(asset.getDepreciationMethod())
                    && input != null && "STATUTORY".equals(input.getDepreciationMethodBasis())) {
                errors.add("定率法の資産は種類別の選定届出・みなし選定等の根拠を確認してください。法定償却方法だけでは選択できません。");
            }
            if (DepreciationCalculator.SMALL_AMOUNT_SPECIAL.equals(asset.getDepreciationMethod())
                    && asset.getServiceStartDate().getYear() == year) {
                specialTotal = specialTotal.add(asset.getAcquisitionCost());
            }
        }
        if (specialTotal.signum() > 0) {
            if (input == null || !input.isDepreciationSpecialConfirmed()) {
                errors.add("少額特例の年分別対象者要件・経理方式・貸付制限・明細添付等が未確認です。");
            }
            int months = input == null ? 12 : input.getDepreciationBusinessMonths();
            if (specialTotal.compareTo(annualCap(months)) > 0) {
                errors.add("少額特例の取得価額合計が年分の上限（事業月数按分）を超えています。");
            }
        }
        return errors;
    }

    private static boolean isEvidencePresent(FixedAssetEvent event) {
        return event.getNotes() != null && !event.getNotes().isBlank();
    }
}
