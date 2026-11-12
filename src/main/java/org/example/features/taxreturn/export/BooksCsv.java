package org.example.features.taxreturn.export;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.example.entity.FixedAssetEvent;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.features.accounting.AccountMapping;
import org.example.features.accounting.AccountMapping.AccountRef;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.taxreturn.dto.TaxReturnExtras.AssetRow;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.dto.TaxReturnPageData.BsRow;

/** 年分の共通仕訳と決算資料から作る、提出用ではない帳簿一覧。 */
public final class BooksCsv {
    private BooksCsv() {}

    public static String build(TaxReturnPageData page, List<JournalEntry> entries, AccountMapping mapping,
            List<FixedAssetEvent> events) {
        int year = page.getSelectedYear();
        return build(page, entries, mapping, events,
                new BookExportFilter(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), null, true));
    }

    public static String build(TaxReturnPageData page, List<JournalEntry> entries, AccountMapping mapping,
            List<FixedAssetEvent> events, BookExportFilter filter) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        row(csv, "分類", "帳簿名", "年分", "仕訳ID・資産ID", "行番号", "日付", "摘要・資産名",
                "科目コード", "科目", "相手科目", "状態", "借方", "貸方", "期首", "期末", "項目", "値");
        row(csv, "注意", "非公式・確認用", page.getSelectedYear(), "", "", "", "", "", "", "", "",
                "", "", "", "", "注意", "提出データではありません。保存要件・税務適合は保証しません。");
        row(csv, "抽出条件", "期間", page.getSelectedYear(), "", "", filter.fromDate() + "～" + filter.toDate(),
                "", "", "", "", "", "", "", "", "", "科目コード", filter.accountCode() == null ? "すべて" : filter.accountCode());
        if (!filter.fullPeriod()) {
            row(csv, "注意", "期間抽出", page.getSelectedYear(), "", "", "", "", "", "", "", "",
                    "", "", "", "", "注意", "一部期間のため年度の期首・期末残高は出力しません。固定資産の償却額は年分全体です。");
        }
        for (JournalEntry entry : new LinkedHashSet<>(entries)) {
            if (entry.getEntryDate().isBefore(filter.fromDate()) || entry.getEntryDate().isAfter(filter.toDate())) continue;
            List<JournalLine> lines = entry.getLines().stream()
                    .sorted(Comparator.comparing(JournalLine::getLineNo)).toList();
            for (JournalLine line : lines) {
                if (!matchesAccount(filter.accountCode(), line.getAccountCode())) continue;
                String counter = lines.stream().filter(other -> !Objects.equals(line.getDebitCredit(), other.getDebitCredit()))
                        .map(JournalLine::getAccountName).distinct().reduce((a, b) -> a + "／" + b).orElse("");
                writeLine(csv, "主要簿", "仕訳帳", page, entry, line, counter);
                writeLine(csv, "主要簿", "総勘定元帳", page, entry, line, counter);
                for (String book : books(line.getAccountCode(), line.getAccountName(), mapping)) {
                    if (book.equals("現金出納帳") || book.equals("売掛帳") || book.equals("買掛帳") || book.equals("経費帳")) {
                        writeLine(csv, "簡易記帳", book, page, entry, line, counter);
                    }
                    writeLine(csv, "補助簿", book, page, entry, line, counter);
                }
            }
        }
        List<BsRow> balances = new ArrayList<>(page.getAssets());
        balances.addAll(page.getLiabilities());
        for (BsRow balance : filter.fullPeriod() ? balances : List.<BsRow>of()) {
            if (balance.getOpening().signum() == 0 && balance.getClosing().signum() == 0) continue;
            AccountRef ref = mapping.byName(balance.getLabel()).orElse(null);
            if (ref == null) continue;
            if (!matchesAccount(filter.accountCode(), ref.code())) continue;
            balance(csv, "主要簿", "総勘定元帳", page, ref, balance);
            for (String book : books(ref.code(), ref.name(), mapping)) {
                if (book.equals("現金出納帳") || book.equals("売掛帳") || book.equals("買掛帳")) {
                    balance(csv, "簡易記帳", book, page, ref, balance);
                }
                balance(csv, "補助簿", book, page, ref, balance);
            }
        }
        for (AssetRow asset : page.getExtras().getAssets()) {
            AccountRef ref = mapping.byName(asset.getAccountName()).orElse(null);
            if (ref == null || !matchesAccount(filter.accountCode(), ref.code())
                    || LocalDate.parse(asset.getAcquired()).isAfter(filter.toDate())) continue;
            boolean disposedBeforeRange = events.stream()
                    .filter(event -> event.getFixedAsset().getId().equals(asset.getId()))
                    .anyMatch(event -> (FixedAssetEvent.SALE.equals(event.getEventType())
                            || FixedAssetEvent.DISPOSAL.equals(event.getEventType()))
                            && event.getEffectiveDate().isBefore(filter.fromDate()));
            if (!filter.fullPeriod() && disposedBeforeRange) continue;
            for (String group : List.of("簡易記帳", "補助簿")) {
                asset(csv, group, page, asset, "取得日", asset.getAcquired());
                asset(csv, group, page, asset, "供用開始日", asset.getServiceStart());
                asset(csv, group, page, asset, "取得価額", asset.getCost());
                asset(csv, group, page, asset, "償却方法", asset.getMethodLabel());
                asset(csv, group, page, asset, "耐用年数", asset.getUsefulLife());
                asset(csv, group, page, asset, "償却率", asset.getRate());
                asset(csv, group, page, asset, "償却基礎金額", asset.getDepreciationBasis());
                asset(csv, group, page, asset, "償却月数", asset.getMonths());
                asset(csv, group, page, asset, "償却費", asset.getDepreciation());
                asset(csv, group, page, asset, "事業割合", asset.getBusinessRatio());
                asset(csv, group, page, asset, "必要経費算入額", asset.getBusinessPortion());
                asset(csv, group, page, asset, "備考", asset.getNotes());
                asset(csv, group, page, asset, "計算根拠", asset.getCalculationDetails());
            }
        }
        for (FixedAssetEvent event : events) {
            if (event.getEffectiveDate().isBefore(filter.fromDate()) || event.getEffectiveDate().isAfter(filter.toDate())
                    || !matchesAccount(filter.accountCode(), event.getFixedAsset().getAccountCode())) continue;
            for (String group : List.of("簡易記帳", "補助簿")) {
                row(csv, group, "固定資産台帳", page.getSelectedYear(), event.getFixedAsset().getId(), "",
                        event.getEffectiveDate(), event.getFixedAsset().getAssetName(), "", "", "", "",
                        "", "", "", "", event.getEventType(), event.getProceeds());
                if (event.getBusinessRatio() != null) {
                    row(csv, group, "固定資産台帳", page.getSelectedYear(), event.getFixedAsset().getId(), "",
                            event.getEffectiveDate(), event.getFixedAsset().getAssetName(), "", "", "", "",
                            "", "", "", "", "事業割合変更", event.getBusinessRatio());
                }
                if (event.getNotes() != null && !event.getNotes().isBlank()) {
                    row(csv, group, "固定資産台帳", page.getSelectedYear(), event.getFixedAsset().getId(), "",
                            event.getEffectiveDate(), event.getFixedAsset().getAssetName(), "", "", "", "",
                            "", "", "", "", "イベント備考", event.getNotes());
                }
            }
        }
        return csv.toString();
    }

    private static boolean matchesAccount(String accountCode, String lineAccountCode) {
        return accountCode == null || accountCode.equals(lineAccountCode);
    }

    private static List<String> books(String code, String name, AccountMapping mapping) {
        AccountRef ref = mapping.byCode(code).orElseGet(() -> mapping.byName(name).orElse(null));
        if (ref == null) return List.of();
        List<String> result = new ArrayList<>();
        switch (ref.code()) {
            case AccountMasterSeed.ACC_CASH -> result.add("現金出納帳");
            case AccountMasterSeed.ACC_BANK_DEPOSIT -> result.add("預金出納帳");
            case AccountMasterSeed.ACC_ACCOUNTS_RECEIVABLE -> result.add("売掛帳");
            case AccountMasterSeed.ACC_ACCOUNTS_PAYABLE -> result.add("買掛帳");
            case AccountMasterSeed.ACC_SALES -> result.add("売上帳");
            case AccountMasterSeed.ACC_PURCHASES -> result.add("仕入帳");
            default -> { }
        }
        if (AccountMasterSeed.CLASS_EXPENSE.equals(ref.accountClass())
                && !AccountMasterSeed.ACC_PURCHASES.equals(ref.code())) result.add("経費帳");
        return result;
    }

    private static void writeLine(StringBuilder csv, String group, String book, TaxReturnPageData page,
            JournalEntry entry, JournalLine line, String counter) {
        row(csv, group, book, page.getSelectedYear(), entry.getId(), line.getLineNo(), entry.getEntryDate(),
                entry.getSummary(), line.getAccountCode(), line.getAccountName(), counter, entry.getStatus(),
                "D".equalsIgnoreCase(line.getDebitCredit()) ? line.getAmount() : null,
                "C".equalsIgnoreCase(line.getDebitCredit()) ? line.getAmount() : null,
                "", "", "明細メモ", line.getMemo());
    }

    private static void balance(StringBuilder csv, String group, String book, TaxReturnPageData page,
            AccountRef ref, BsRow balance) {
        row(csv, group, book, page.getSelectedYear(), "", "", "", "残高", ref.code(), ref.name(), "", "",
                "", "", balance.getOpening(), balance.getClosing(), "", "");
    }

    private static void asset(StringBuilder csv, String group, TaxReturnPageData page, AssetRow asset,
            String field, Object value) {
        row(csv, group, "固定資産台帳", page.getSelectedYear(), asset.getId(), "", "", asset.getName(),
                "", asset.getAccountName(), "", "", "", "", asset.getOpening(), asset.getClosing(), field, value);
    }

    static void row(StringBuilder csv, Object... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) csv.append(',');
            Object field = fields[i];
            String value = field == null ? "" : field.toString();
            if (!(field instanceof Number)) {
                int start = 0;
                while (start < value.length() && (Character.isWhitespace(value.charAt(start))
                        || Character.isSpaceChar(value.charAt(start)) || Character.isISOControl(value.charAt(start))
                        || value.charAt(start) == '\uFEFF')) start++;
                if ((start < value.length() && "=+-@".indexOf(value.charAt(start)) >= 0)
                        || (start > 0 && (value.charAt(0) == '\t' || value.charAt(0) == '\r' || value.charAt(0) == '\n'))) {
                    value = "'" + value;
                }
            }
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }
}
