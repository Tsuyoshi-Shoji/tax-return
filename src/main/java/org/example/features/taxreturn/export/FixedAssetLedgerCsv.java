package org.example.features.taxreturn.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.entity.FixedAsset;
import org.example.entity.FixedAssetEvent;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMapping.AccountRef;
import org.example.features.taxreturn.asset.DepreciationCalculator;
import org.example.features.taxreturn.asset.DepreciationRules;

/** 選択年末時点の登録資産一覧と、その時点までのイベント履歴。 */
public final class FixedAssetLedgerCsv {
    private FixedAssetLedgerCsv() {}

    public static String build(int year, List<FixedAsset> assets, List<FixedAssetEvent> events,
            AccountMapping mapping) {
        return build(year, assets, events, mapping,
                new BookExportFilter(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), null, true));
    }

    public static String build(int year, List<FixedAsset> assets, List<FixedAssetEvent> events,
            AccountMapping mapping, BookExportFilter filter) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        Map<Long, List<FixedAssetEvent>> byAsset = events.stream()
                .filter(e -> !e.getEffectiveDate().isAfter(end))
                .sorted(Comparator.comparing(FixedAssetEvent::getEffectiveDate).thenComparing(FixedAssetEvent::getId))
                .collect(Collectors.groupingBy(e -> e.getFixedAsset().getId()));
        StringBuilder csv = new StringBuilder("\uFEFF");
        BooksCsv.row(csv, "年分", "行区分", "資産ID", "資産名", "科目コード", "科目", "取得日", "供用開始日",
                "取得価額", "取得資金区分", "償却方法コード", "耐用年数", "償却率", "当年償却月数",
                "期首未償却残高", "当年償却費", "当年事業専用割合（%）", "当年必要経費算入額",
                "事業供用後償却累計額", "売却・除却時未償却残高", "期末未償却残高", "状態", "資産備考",
                "イベントID", "イベント区分", "適用日", "変更後事業専用割合（%）", "売却価額", "イベント備考",
                "耐用年数根拠", "法定耐用年数", "中古取得時経過月数", "中古要件利用者確認",
                "非業務用転用", "非業務用期間減価", "供用開始時簿価", "計算段階",
                "改定取得価額", "償却保証額", "250％経過措置利用者確認", "計算ルール版", "償却基礎金額");
        if (!filter.fullPeriod() || filter.accountCode() != null) {
            Object[] filterRow = new Object[42];
            filterRow[0] = year;
            filterRow[1] = "抽出条件";
            filterRow[3] = filter.accountCode() == null ? "全勘定科目" : "科目コード=" + filter.accountCode();
            filterRow[6] = filter.fromDate();
            filterRow[7] = filter.toDate();
            if (!filter.fullPeriod()) filterRow[22] = "償却額・残高は選択年分全体";
            BooksCsv.row(csv, filterRow);
        }
        for (FixedAsset asset : assets) {
            if (DepreciationRules.recognitionDate(asset, events).isAfter(end)) {
                continue;
            }
            if (filter.accountCode() != null && !filter.accountCode().equals(asset.getAccountCode())) continue;
            List<FixedAssetEvent> history = byAsset.getOrDefault(asset.getId(), List.of());
            FixedAssetEvent cessation = history.stream()
                    .filter(e -> FixedAssetEvent.BUSINESS_CESSATION.equals(e.getEventType()))
                    .findFirst().orElse(null);
            FixedAssetEvent disposal = history.stream()
                    .filter(e -> FixedAssetEvent.SALE.equals(e.getEventType())
                            || FixedAssetEvent.DISPOSAL.equals(e.getEventType())
                            || FixedAssetEvent.BUSINESS_CESSATION.equals(e.getEventType()))
                    .findFirst().orElse(null);
            boolean disposedEarlier = disposal != null && disposal.getEffectiveDate().isBefore(start);
            boolean retain = DepreciationRules.retainsBalanceAfterDisposal(asset) || cessation != null;
            boolean ceasedEarlier = cessation != null && cessation.getEffectiveDate().isBefore(start);
            if (DepreciationRules.recognitionDate(asset, events).isAfter(filter.toDate())
                    || (!filter.fullPeriod() && disposal != null
                            && disposal.getEffectiveDate().isBefore(filter.fromDate()) && !retain)) continue;
            BigDecimal ratio = history.stream()
                    .filter(e -> FixedAssetEvent.BUSINESS_RATIO_CHANGE.equals(e.getEventType()))
                    .filter(e -> !e.getEffectiveDate().isAfter(start))
                    .reduce((first, second) -> second)
                    .map(FixedAssetEvent::getBusinessRatio).orElse(asset.getBusinessRatio());
            int calculationYear = disposedEarlier && !retain ? disposal.getEffectiveDate().getYear()
                    : ceasedEarlier ? cessation.getEffectiveDate().getYear() : year;
            DepreciationCalculator.YearResult result = DepreciationCalculator.forYear(asset, calculationYear, ratio, history, assets);
            BigDecimal disposalValue = disposal == null || FixedAssetEvent.BUSINESS_CESSATION.equals(disposal.getEventType())
                    ? null : DepreciationCalculator.forYear(asset,
                    disposal.getEffectiveDate().getYear(), ratio, history, assets).closing();
            BigDecimal opening = (disposedEarlier && !retain) || !DepreciationRules.recognitionDate(asset, events).isBefore(start)
                    ? BigDecimal.ZERO : ceasedEarlier ? result.closing() : result.opening();
            BigDecimal depreciation = disposedEarlier && !retain || ceasedEarlier
                    ? BigDecimal.ZERO : result.depreciation();
            BigDecimal businessPortion = disposedEarlier && !retain || ceasedEarlier
                    ? BigDecimal.ZERO : result.businessPortion();
            String state = disposal != null
                    ? (FixedAssetEvent.BUSINESS_CESSATION.equals(disposal.getEventType())
                            ? (disposedEarlier ? "廃業後（残存簿価の処理確認）" : "廃業")
                            : FixedAssetEvent.SALE.equals(disposal.getEventType()) ? "売却済み" : "除却済み")
                    : (asset.getServiceStartDate().isAfter(end) ? "未供用" : "保有");
            String assetNotes = "CAPITAL_IMPROVEMENT".equals(asset.getAssetOrigin()) ? "資本的支出（別資産）" : "";
            if (asset.getParentAsset() != null) {
                assetNotes += (assetNotes.isEmpty() ? "" : " ") + "対象資産ID=" + asset.getParentAsset().getId();
            }
            if (asset.getNotes() != null && !asset.getNotes().isBlank()) {
                assetNotes += (assetNotes.isEmpty() ? "" : " ") + asset.getNotes();
            }
            BooksCsv.row(csv, year, "資産", asset.getId(), asset.getAssetName(), asset.getAccountCode(),
                    mapping.byCode(asset.getAccountCode()).map(AccountRef::name).orElse(asset.getAccountCode()),
                    asset.getAcquisitionDate(), asset.getServiceStartDate(), asset.getAcquisitionCost(),
                    asset.getAcquisitionFunding(), asset.getDepreciationMethod(), asset.getUsefulLife(),
                    result.rate(), disposedEarlier && !retain || ceasedEarlier ? 0 : result.months(),
                    opening, depreciation, ratio, businessPortion,
                    DepreciationCalculator.initialBookValue(asset).subtract(result.closing()),
                    disposalValue, disposal == null || retain || cessation != null ? result.closing() : BigDecimal.ZERO,
                    state, assetNotes, "", "", "", "", "", "",
                    asset.getLifeBasis(), asset.getStatutoryUsefulLife(), asset.getUsedElapsedMonths(),
                    asset.isUsedRequirementsConfirmed(), asset.isPrivateUseConversion(),
                    DepreciationCalculator.privateUseDecrease(asset), DepreciationCalculator.initialBookValue(asset),
                    result.phase(), result.revisedAcquisitionCost(), result.guaranteeAmount(),
                    asset.isRateTransitionConfirmed(), DepreciationRules.version(year), DepreciationRules.depreciationBasis(asset, result));
            for (FixedAssetEvent event : history) {
                if (!filter.fullPeriod() && event.getEffectiveDate().isBefore(filter.fromDate())) continue;
                if (event.getEffectiveDate().isAfter(filter.toDate())) continue;
                BooksCsv.row(csv, year, "イベント", asset.getId(), asset.getAssetName(), "", "", "", "", "", "",
                        "", "", "", "", "", "", "", "", "", "", "", "", "",
                        event.getId(), event.getEventType(), event.getEffectiveDate(),
                        event.getBusinessRatio(), event.getProceeds(), event.getNotes(),
                        "", "", "", "", "", "", "", "", "", "", "", "", "");
            }
        }
        return csv.toString();
    }
}
