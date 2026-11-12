package org.example.features.setting;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 確定申告画面の画面上のセクション。required のセクションは申告書作成に必須のため非表示にできない。
 * 印刷専用の帳票（決算書様式の印刷・総勘定元帳など）は対象外で、常に出力される。
 */
public enum TaxReturnSection {
    DEDUCTION_REQUIREMENTS("deduction-requirements", "青色申告特別控除の区分・要件確認", "控除区分の選択と要件確認", true),
    RETENTION("retention", "年分別の保存管理台帳", "帳簿・証憑の保存期限の管理", false),
    BASIC_INFO("basic-info", "基本情報", "申告者の氏名・住所等", true),
    ANNUAL_SUMMARY("annual-summary", "年間収支サマリー", "売上・経費・所得の概要", true),
    MONTHLY("monthly", "月別収支", "決算書2ページの月別売上等", true),
    PROFIT_LOSS("profit-loss", "損益計算書", "勘定科目別の損益", true),
    SETTLEMENT("settlement", "決算整理", "棚卸・売掛金・期末残高等", true),
    BLUE_STATEMENT("blue-statement", "青色申告決算書（損益計算書）", "決算書1ページの各欄", true),
    FIXED_ASSETS("fixed-assets", "固定資産台帳・減価償却", "固定資産・減価償却費がある年度に使用", false),
    HOUSEHOLD_CONSUMPTION("household-consumption", "家事消費等", "棚卸資産の家事消費・贈与がある場合に使用", false),
    SALARY_DETAILS("salary-details", "給料賃金・専従者給与の個人別内訳", "従業員・青色事業専従者がいる場合に使用", false),
    BALANCE_SHEET("balance-sheet", "貸借対照表", "55万円・65万円控除で添付", true),
    JOURNAL("journal", "仕訳帳", "複式簿記の帳簿", true),
    DEDUCTIONS("deductions", "申告情報・所得控除", "確定申告書 第一表・第二表の控除", true),
    TAX("tax", "税額計算", "確定申告書 第一表・第二表", true),
    CONSUMPTION("consumption", "消費税（簡易課税）", "課税事業者（簡易課税）の場合に使用", false),
    CHECKLIST("checklist", "青色申告決算書チェック", "出力前の確認項目", true),
    OUTPUT("output", "出力", "PDF・CSVの出力", true),
    CLOSING("closing", "年度締め・訂正履歴・出力記録", "年度の確定と履歴", true);

    private final String code;
    private final String label;
    private final String description;
    private final boolean required;

    TaxReturnSection(String code, String label, String description, boolean required) {
        this.code = code;
        this.label = label;
        this.description = description;
        this.required = required;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }

    public static Optional<TaxReturnSection> fromCode(String code) {
        return Arrays.stream(values()).filter(section -> section.code.equals(code)).findFirst();
    }

    /** 保存値（コード区切り）から非表示セクションを復元する。未知・必須のコードは無視する。 */
    public static Set<TaxReturnSection> parseHidden(String stored) {
        Set<TaxReturnSection> hidden = EnumSet.noneOf(TaxReturnSection.class);
        if (stored == null || stored.isBlank()) {
            return hidden;
        }
        for (String code : stored.split(",")) {
            fromCode(code.strip()).filter(section -> !section.required).ifPresent(hidden::add);
        }
        return hidden;
    }

    public static String formatHidden(Set<TaxReturnSection> hidden) {
        String value = hidden.stream()
                .filter(section -> !section.required)
                .sorted()
                .map(TaxReturnSection::getCode)
                .collect(Collectors.joining(","));
        return value.isEmpty() ? null : value;
    }
}
