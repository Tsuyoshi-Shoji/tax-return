package org.example.features.taxreturn.incometax;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 年分別の国税庁資料参照状況。アプリ帳票は公式様式そのものではない。
 */
public final class BlueReturnEdition {
    private static final DateTimeFormatter JAPANESE_DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");
    /** 国税庁資料と照合した日。年分の様式公表・訂正を再確認したら更新する。 */
    static final String VERIFIED_ON = "2026年11月12日";
    private static final String TAX_ANSWER_URL = "https://www.nta.go.jp/taxes/shiraberu/taxanswer/shotoku/2020.htm";

    private final String code;
    private final String status;
    private final String sourceLabel;
    private final String sourceUrl;
    private final LocalDate filingDeadline;

    private BlueReturnEdition(String code, String status, String sourceLabel, String sourceUrl, LocalDate filingDeadline) {
        this.code = code;
        this.status = status;
        this.sourceLabel = sourceLabel;
        this.sourceUrl = sourceUrl;
        this.filingDeadline = filingDeadline;
    }

    public static BlueReturnEdition forTaxYear(int taxYear) {
        return switch (taxYear) {
            case 2024 -> new BlueReturnEdition("R06-NTA0KOA21-R5-CHK20261112",
                    "国税庁の令和6年分様式一覧に掲載の「所得税青色申告決算書（一般用）【令和5年分以降用】」"
                            + "（様式ID NTA0KOA21、全4ページ）と全欄照合済み（" + VERIFIED_ON + "）",
                    "国税庁・令和6年分 所得税等の様式一覧",
                    "https://www.nta.go.jp/taxes/shiraberu/shinkoku/syotoku/r06.htm",
                    LocalDate.of(2025, 3, 17));
            case 2025 -> new BlueReturnEdition("R07-NTA0KOA21-R5-CHK20261112",
                    "国税庁の令和7年分様式一覧に掲載の「所得税青色申告決算書（一般用）【令和5年分以降用】」"
                            + "（令和6年分と同一様式）と全欄照合済み（" + VERIFIED_ON + "）。"
                            + "令和7年分の訂正票（正誤表）は申告書第四表付表の書き方が対象で、青色申告決算書への影響なし",
                    "国税庁・令和7年分 所得税等の様式一覧",
                    "https://www.nta.go.jp/taxes/shiraberu/shinkoku/syotoku/r07.htm",
                    LocalDate.of(2026, 3, 16));
            case 2026 -> new BlueReturnEdition("R08-UNCONFIRMED",
                    "令和8年分の青色申告決算書様式・期限は国税庁未公表のため未確認（" + VERIFIED_ON
                            + "確認）。公表後に照合して反映します",
                    "国税庁・所得税の確定申告案内",
                    "https://www.nta.go.jp/taxes/shiraberu/shinkoku/kakutei.htm",
                    null);
            default -> new BlueReturnEdition("UNSUPPORTED",
                    "対象年分の様式・期限は未確認",
                    "国税庁・確定申告の案内",
                    "https://www.nta.go.jp/taxes/shiraberu/shinkoku/kakutei.htm",
                    null);
        };
    }

    /** 出力記録に残す様式版の識別子（参照資料と照合状態を表す。公式様式の版番号ではない）。 */
    public String getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    /** 確認済みの申告期限。未確定の場合は null。 */
    public LocalDate getFilingDeadline() {
        return filingDeadline;
    }

    public String getFilingDeadlineText() {
        return filingDeadline == null
                ? "未確定。国税庁の年分別案内で確認してください。"
                : filingDeadline.format(JAPANESE_DATE);
    }

    public String getDeadlineSourceUrl() {
        return TAX_ANSWER_URL;
    }
}
