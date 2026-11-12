package org.example.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.asset.DepreciationRules;

/** 国税庁の具体例・適用境界・残額推移を、DBから独立して確認する。 */
public final class DepreciationCalculatorHarness {
    public static void main(String[] args) {
        verifyDeclining();
        verifyOldMethods();
        verifyUsedAndConversion();
        verifyMethodBoundaries();
        verifyLifecycleEvents();
        System.out.println("Depreciation calculator harness passed");
    }

    private static void verifyDeclining() {
        FixedAsset modern = asset("2012-04-01", "2013-01-01", "1000000", 10, "DECLINING_200");
        amount(result(modern, 2013).depreciation(), "200000", "200 percent first year");
        amount(result(modern, 2014).depreciation(), "160000", "200 percent second year");
        var switched = result(modern, 2019);
        require("REVISED".equals(switched.phase()), "200 percent did not switch at guarantee threshold");
        amount(switched.revisedAcquisitionCost(), "262144", "200 percent revised base");
        amount(switched.guaranteeAmount(), "65520", "200 percent guarantee");
        amount(switched.depreciation(), "65536", "200 percent revised expense");
        amount(result(modern, 2022).depreciation(), "65535", "200 percent last yen");
        amount(result(modern, 2023).closing(), "1", "200 percent residual");

        FixedAsset earlier = asset("2011-01-01", "2011-01-01", "1000000", 10, "DECLINING_250");
        var revised250 = result(earlier, 2018);
        amount(revised250.revisedAcquisitionCost(), "133483", "250 percent revised base");
        amount(revised250.depreciation(), "44584", "250 percent revised expense");
        amount(result(earlier, 2020).depreciation(), "44314", "250 percent final expense");
        amount(result(earlier, 2020).closing(), "1", "250 percent residual");

        FixedAsset shortLife = asset("2025-01-01", "2025-01-01", "100001", 2, "DECLINING_200");
        amount(result(shortLife, 2025).depreciation(), "100000", "two-year full rate");
        amount(result(shortLife, 2026).depreciation(), "0", "two-year no revised rate");

        FixedAsset partial = asset("2025-10-01", "2025-10-01", "1000000", 5, "DECLINING_200");
        amount(result(partial, 2025).depreciation(), "100000", "declining first-year months");
        amount(result(partial, 2026).depreciation(), "360000", "declining opening balance");
        FixedAssetEvent disposal = new FixedAssetEvent();
        disposal.setFixedAsset(partial);
        disposal.setEventType("DISPOSAL");
        disposal.setEffectiveDate(LocalDate.of(2026, 6, 1));
        var disposed = DepreciationCalculator.forYear(partial, 2026, new BigDecimal("50"),
                List.of(disposal), List.of(partial));
        amount(disposed.depreciation(), "180000", "declining disposal months");
        amount(disposed.businessPortion(), "90000", "declining business portion");
        amount(DepreciationCalculator.forYear(partial, 2027, BigDecimal.valueOf(100),
                List.of(disposal), List.of(partial)).opening(), "0", "declining after disposal");
    }

    private static void verifyOldMethods() {
        FixedAsset straight = asset("2006-01-01", "2006-01-01", "5000000", 5, "OLD_STRAIGHT_LINE");
        amount(result(straight, 2006).depreciation(), "900000", "old straight first year");
        amount(result(straight, 2011).depreciation(), "250000", "old straight 95 percent cap");
        amount(result(straight, 2012).depreciation(), "50000", "old straight five-year tail");
        amount(result(straight, 2016).depreciation(), "49999", "old straight last year");
        amount(result(straight, 2017).closing(), "1", "old straight residual");

        FixedAsset declining = asset("2006-01-01", "2006-01-01", "5000000", 5, "OLD_DECLINING");
        amount(result(declining, 2006).depreciation(), "1845000", "old declining first year");
        amount(result(declining, 2012).depreciation(), "65606", "old declining 95 percent cap");
        amount(result(declining, 2013).depreciation(), "50000", "old declining five-year tail");
        amount(result(declining, 2017).depreciation(), "49999", "old declining last year");
        amount(result(declining, 2017).closing(), "1", "old declining residual");

        FixedAsset preReform = asset("2000-01-01", "2000-01-01", "5000000", 5, "OLD_STRAIGHT_LINE");
        amount(result(preReform, 2007).depreciation(), "0", "no five-year tail before 2008");
        amount(result(preReform, 2008).depreciation(), "50000", "five-year tail begins in 2008");
        amount(result(preReform, 2012).closing(), "1", "pre-reform final residual");

        FixedAsset oldSoftware = asset("2006-01-01", "2006-01-01", "1000000", 5, "OLD_STRAIGHT_LINE");
        oldSoftware.setAccountCode("SOFTWARE_ASSET");
        amount(result(oldSoftware, 2006).depreciation(), "200000", "old intangible no 90 percent factor");
        amount(result(oldSoftware, 2010).closing(), "0", "old intangible no 5 percent tail");

        FixedAsset sixYear = asset("2006-01-01", "2006-01-01", "1000000", 6, "OLD_STRAIGHT_LINE");
        amount(result(sixYear, 2006).depreciation(), "149400", "old statutory rate not reciprocal");
    }

    private static void verifyUsedAndConversion() {
        require(DepreciationRules.usedUsefulLife(22, 79) == 16, "used life: month precision");
        require(DepreciationRules.usedUsefulLife(30, 120) == 22, "used life: partially elapsed");
        require(DepreciationRules.usedUsefulLife(5, 60) == 2, "used life: minimum two years");
        require(DepreciationRules.usedUsefulLife(50, 900) == 10, "used life: fully elapsed");

        // No.2108の2016年中古取得・2023年転用の具体例。
        FixedAsset used = asset("2016-01-10", "2023-04-01", "10000000", 16, "STRAIGHT_LINE");
        used.setStatutoryUsefulLife(22);
        used.setLifeBasis("USED_SIMPLIFIED");
        used.setUsedElapsedMonths(79);
        used.setUsedRequirementsConfirmed(true);
        used.setPrivateUseConversion(true);
        amount(DepreciationCalculator.privateUseDecrease(used), "1953000", "private loss uses original legal life");
        amount(DepreciationCalculator.initialBookValue(used), "8047000", "used conversion initial value");
        amount(result(used, 2023).depreciation(), "472500", "used conversion depreciation uses original cost");
        amount(result(used, 2023).closing(), "7574500", "used conversion closing");
        amount(result(used, 2022).closing(), "0", "no business value before conversion");

        // 所得税質疑応答04/17の旧定額法による転用例。
        FixedAsset old = asset("2006-10-20", "2025-02-01", "30000000", 22, "OLD_STRAIGHT_LINE");
        old.setAccountCode("BUILDING_IMPROVEMENTS");
        old.setPrivateUseConversion(true);
        amount(DepreciationCalculator.privateUseDecrease(old), "15066000", "old conversion private loss");
        amount(result(old, 2025).depreciation(), "1138500", "old conversion months");
        amount(result(old, 2025).closing(), "13795500", "old conversion closing");

        FixedAsset months = asset("2020-01-10", "2020-07-09", "1000000", 4, "STRAIGHT_LINE");
        months.setPrivateUseConversion(true);
        amount(DepreciationCalculator.privateUseDecrease(months), "0", "private period below six months");
        months.setServiceStartDate(LocalDate.of(2020, 7, 10));
        amount(DepreciationCalculator.privateUseDecrease(months), "149400", "private period at six months");
        months.setServiceStartDate(LocalDate.of(2050, 1, 10));
        amount(DepreciationCalculator.privateUseDecrease(months), "950000", "private loss capped at 95 percent");

        FixedAsset declining = asset("2015-01-01", "2025-07-01", "1000000", 10, "DECLINING_200");
        declining.setPrivateUseConversion(true);
        amount(result(declining, 2025).opening(), "346600", "private declining initial value");
        amount(result(declining, 2025).depreciation(), "34660", "private declining uses reduced balance");
        amount(result(declining, 2026).revisedAcquisitionCost(), "311940", "private declining revised base");
        amount(result(declining, 2026).depreciation(), "77985", "private declining guarantee uses original cost");

        FixedAsset exhausted = asset("2000-01-01", "2025-07-01", "1000000", 4, "OLD_STRAIGHT_LINE");
        exhausted.setPrivateUseConversion(true);
        var tail = result(exhausted, 2025);
        require("OLD_FIVE_YEAR".equals(tail.phase()), "old exhausted conversion did not use five-year tail");
        amount(tail.depreciation(), "5000", "old conversion tail prorated");
    }

    private static void verifyMethodBoundaries() {
        FixedAsset boundary = asset("2007-03-31", "2025-01-01", "1000000", 5, "STRAIGHT_LINE");
        require(!DepreciationRules.assetErrors(boundary).isEmpty(), "new method allowed before reform");
        boundary.setDepreciationMethod("OLD_DECLINING");
        require(DepreciationRules.assetErrors(boundary).isEmpty(), "old method rejected on boundary");
        boundary.setAcquisitionDate(LocalDate.of(2007, 4, 1));
        require(!DepreciationRules.assetErrors(boundary).isEmpty(), "old method allowed after reform");
        boundary.setDepreciationMethod("DECLINING_250");
        require(DepreciationRules.assetErrors(boundary).isEmpty(), "250 percent rejected after reform");
        boundary.setAcquisitionDate(LocalDate.of(2012, 4, 1));
        require(!DepreciationRules.assetErrors(boundary).isEmpty(), "250 percent transitional confirmation missing");
        boundary.setRateTransitionConfirmed(true);
        require(DepreciationRules.assetErrors(boundary).isEmpty(), "250 percent continuation rejected");
        boundary.setAcquisitionDate(LocalDate.of(2013, 1, 1));
        require(!DepreciationRules.assetErrors(boundary).isEmpty(), "250 percent allowed outside transition");
        boundary.setRateTransitionConfirmed(false);
        boundary.setDepreciationMethod("DECLINING_200");
        boundary.setAccountCode("BUILDING_IMPROVEMENTS");
        boundary.setAcquisitionDate(LocalDate.of(2016, 3, 31));
        require(DepreciationRules.assetErrors(boundary).isEmpty(), "equipment declining rejected before cutoff");
        boundary.setAcquisitionDate(LocalDate.of(2016, 4, 1));
        require(!DepreciationRules.assetErrors(boundary).isEmpty(), "equipment declining allowed after cutoff");
        boundary.setAccountCode("SOFTWARE_ASSET");
        require(!DepreciationRules.assetErrors(boundary).isEmpty(), "intangible declining allowed");

        FixedAsset legacy = asset("2025-01-01", "2025-01-01", "1000000", 5, "STRAIGHT_LINE");
        var confirmation = new org.example.entity.TaxReturnInput();
        confirmation.setDepreciationRuleVersion("2025-2026-04-01-v1");
        require(DepreciationRules.confirmationMatches(2025, List.of(legacy), confirmation),
                "unchanged legacy confirmation was invalidated");
        legacy.setDepreciationMethod("DECLINING_200");
        require(!DepreciationRules.confirmationMatches(2025, List.of(legacy), confirmation),
                "legacy confirmation accepted for an extended method");
    }

    private static void verifyLifecycleEvents() {
        FixedAsset legacyDeclining = asset("2011-01-01", "2011-01-01", "1000000", 10, "DECLINING_250");
        FixedAssetEvent methodChange = new FixedAssetEvent();
        methodChange.setFixedAsset(legacyDeclining);
        methodChange.setEventType(FixedAssetEvent.METHOD_CHANGE);
        methodChange.setEffectiveDate(LocalDate.of(2024, 1, 1));
        methodChange.setOpeningBookValue(new BigDecimal("500000"));
        methodChange.setNewDepreciationMethod(DepreciationCalculator.DECLINING_200);
        methodChange.setHistoryConfirmed(true);
        methodChange.setNotes("Approved method change; opening balance verified");
        var switched = DepreciationCalculator.forYear(legacyDeclining, 2024, BigDecimal.valueOf(100),
                List.of(methodChange), List.of(legacyDeclining));
        amount(switched.opening(), "500000", "method-change opening book value");
        amount(switched.depreciation(), "100000", "250 to 200 percent transition");

        FixedAsset gifted = asset("2020-01-01", "2020-01-01", "1000000", 10, "STRAIGHT_LINE");
        FixedAssetEvent transfer = new FixedAssetEvent();
        transfer.setFixedAsset(gifted);
        transfer.setEventType(FixedAssetEvent.GIFT);
        transfer.setEffectiveDate(LocalDate.of(2025, 4, 15));
        transfer.setOpeningBookValue(new BigDecimal("600000"));
        transfer.setHistoryConfirmed(true);
        transfer.setNotes("Gift record and prior depreciation verified");
        var transferred = DepreciationCalculator.forYear(gifted, 2025, BigDecimal.valueOf(100),
                List.of(transfer), List.of(gifted));
        amount(transferred.opening(), "600000", "gift carryover book value");
        amount(transferred.depreciation(), "75000", "gift transfer month proration");
        amount(DepreciationCalculator.forYear(gifted, 2024, BigDecimal.valueOf(100),
                List.of(transfer), List.of(gifted)).depreciation(), "0", "no depreciation before gift");

        FixedAsset closing = asset("2020-01-01", "2020-01-01", "1000000", 10, "STRAIGHT_LINE");
        FixedAssetEvent cessation = new FixedAssetEvent();
        cessation.setFixedAsset(closing);
        cessation.setEventType(FixedAssetEvent.BUSINESS_CESSATION);
        cessation.setEffectiveDate(LocalDate.of(2025, 6, 15));
        var ended = DepreciationCalculator.forYear(closing, 2025, BigDecimal.valueOf(100),
                List.of(cessation), List.of(closing));
        amount(ended.depreciation(), "50000", "business cessation through event month");
        amount(ended.closing(), "450000", "business cessation preserves residual book value");
        amount(DepreciationCalculator.forYear(closing, 2026, BigDecimal.valueOf(100),
                List.of(cessation), List.of(closing)).depreciation(), "0", "no business depreciation after cessation");
    }

    private static FixedAsset asset(String acquired, String service, String cost, int life, String method) {
        FixedAsset asset = new FixedAsset();
        try {
            var id = FixedAsset.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(asset, 1L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create asset fixture", e);
        }
        asset.setAssetName("Synthetic asset");
        asset.setAccountCode("TOOLS_EQUIPMENT");
        asset.setAcquisitionDate(LocalDate.parse(acquired));
        asset.setServiceStartDate(LocalDate.parse(service));
        asset.setAcquisitionCost(new BigDecimal(cost));
        asset.setUsefulLife(life);
        asset.setStatutoryUsefulLife(life);
        asset.setDepreciationMethod(method);
        asset.setBusinessRatio(BigDecimal.valueOf(100));
        asset.setAcquisitionFunding("OWNER_FUNDS");
        return asset;
    }

    private static DepreciationCalculator.YearResult result(FixedAsset asset, int year) {
        return DepreciationCalculator.forYear(asset, year);
    }

    private static void amount(BigDecimal actual, String expected, String message) {
        require(actual != null && actual.compareTo(new BigDecimal(expected)) == 0,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalStateException(message);
        }
    }
}
