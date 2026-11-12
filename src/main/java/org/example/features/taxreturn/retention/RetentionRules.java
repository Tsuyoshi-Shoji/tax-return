package org.example.features.taxreturn.retention;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 法令・年分・書類種別に応じた保存期限の決定的な計算規則。
 * <p>
 * 根拠（確認日 {@value #CHECKED_ON}、e-Gov 法令検索・国税庁タックスアンサー No.2070／No.2080）：
 * 所得税法施行規則63条（青色申告者）、102条（白色申告者等）、消費税法施行令50条1項（仕入税額控除）。
 * 起算日は帳簿＝対象年分の翌年3月16日、書類＝作成・受領日の属する年の翌年3月16日、
 * 消費税＝課税期間（暦年）の末日の翌日から2月を経過した日（翌年3月1日）とし、期限は起算日から N 年後の前日とする。
 * 期限短縮となる特例（前々年分所得300万円以下の5年特例、消費税の5年特例等）は自動適用しない。
 * 本計算は法令適合を保証せず、利用者が対象年分の公式資料で確認することを前提とする。
 */
public final class RetentionRules {
    public static final String CHECKED_ON = "2026-11-12";
    public static final String RULE_VERSION = "R2026-11-12";
    public static final String MANUAL = "MANUAL";
    public static final int MIN_TAX_YEAR = 2024;
    public static final int MAX_TAX_YEAR = 2026;
    public static final int DUE_SOON_DAYS = 90;
    private static final LocalDate MIN_DOCUMENT_DATE = LocalDate.of(2000, 1, 1);
    private static final String SOURCES = "国税庁タックスアンサーNo.2070・No.2080";

    public enum Category {
        BOOK_MAIN(true, "主要帳簿（仕訳帳・総勘定元帳）", 7, 7,
                "所得税法施行規則63条1項・4項", "所得税法施行規則102条（法定帳簿）"),
        BOOK_OTHER(true, "補助簿その他の帳簿（現金出納帳・売掛帳・固定資産台帳等）", 7, 5,
                "所得税法施行規則63条1項・4項", "所得税法施行規則102条（任意帳簿）"),
        SETTLEMENT_DOC(false, "決算関係書類（棚卸表・貸借対照表・損益計算書等）", 7, 5,
                "所得税法施行規則63条1項・4項", "所得税法施行規則102条"),
        CASH_DOC(false, "現金預金取引等関係書類（領収書・預金通帳等、現金・預金の授受に関するもの）", 7, 5,
                "所得税法施行規則63条1項・3項・4項", "所得税法施行規則102条"),
        OTHER_DOC(false, "その他の書類（請求書・納品書・見積書・注文書・契約書等）", 5, 5,
                "所得税法施行規則63条1項・4項", "所得税法施行規則102条");

        private final boolean book;
        private final String label;
        private final int blueYears;
        private final int whiteYears;
        private final String blueBasis;
        private final String whiteBasis;

        Category(boolean book, String label, int blueYears, int whiteYears, String blueBasis, String whiteBasis) {
            this.book = book;
            this.label = label;
            this.blueYears = blueYears;
            this.whiteYears = whiteYears;
            this.blueBasis = blueBasis;
            this.whiteBasis = whiteBasis;
        }

        public String getCode() { return name(); }

        public String getLabel() { return label; }

        public boolean isBook() { return book; }
    }

    public record Result(LocalDate startDate, LocalDate deadline, String legalBasis, int years) {
    }

    private RetentionRules() {
    }

    public static List<Category> categories() {
        return Arrays.asList(Category.values());
    }

    public static Optional<Category> category(String code) {
        return Arrays.stream(Category.values()).filter(c -> c.name().equals(code)).findFirst();
    }

    public static String label(String code) {
        if (code == null || MANUAL.equals(code)) {
            return "手入力（ルール未適用）";
        }
        return category(code).map(Category::getLabel).orElse("不明なルール");
    }

    public static boolean supportsYear(int taxYear) {
        return taxYear >= MIN_TAX_YEAR && taxYear <= MAX_TAX_YEAR;
    }

    public static boolean isBlue(String filingType) {
        return filingType != null && filingType.startsWith("blue");
    }

    /** 書類の作成・受領日として受け付ける範囲か。 */
    public static boolean validDocumentDate(LocalDate documentDate, LocalDate today) {
        return documentDate != null && !documentDate.isBefore(MIN_DOCUMENT_DATE) && !documentDate.isAfter(today);
    }

    /**
     * 所得税・消費税の該当ルールのうち最も遅い期限を返す。対象年分の最低保存期限も下回らない。
     */
    public static Result calculate(Category category, boolean blue, int taxYear, LocalDate documentDate,
            boolean consumptionTax) {
        if (!supportsYear(taxYear)) {
            throw new IllegalArgumentException("unsupported tax year");
        }
        if (!category.book && documentDate == null) {
            throw new IllegalArgumentException("document date required");
        }
        int baseYear = category.book ? taxYear : documentDate.getYear();
        int years = blue ? category.blueYears : category.whiteYears;
        LocalDate start = LocalDate.of(baseYear + 1, 3, 16);
        LocalDate deadline = deadline(start, years);
        List<String> basis = new ArrayList<>();
        basis.add((blue ? category.blueBasis : category.whiteBasis) + "（" + years + "年）");
        if (consumptionTax) {
            LocalDate taxStart = LocalDate.of(baseYear + 1, 3, 1);
            LocalDate taxDeadline = deadline(taxStart, 7);
            basis.add("消費税法施行令50条1項（7年）");
            if (taxDeadline.isAfter(deadline)) {
                deadline = taxDeadline;
                years = 7;
            }
        }
        LocalDate floor = minimumDeadline(taxYear);
        if (floor.isAfter(deadline)) {
            deadline = floor;
            basis.add("対象年分の最低保存期限");
        }
        String text = String.join("・", basis) + "／" + SOURCES + "／確認日" + CHECKED_ON + "／" + RULE_VERSION;
        return new Result(start, deadline, text, years);
    }

    /** 起算日から N 年を経過する日の前日（＝保存期限の最終日）。 */
    public static LocalDate deadline(LocalDate start, int years) {
        return start.plusYears(years).minusDays(1);
    }

    /** 対象年分の帳簿・書類に共通する最低保存期限（翌年3月16日から5年）。これより前の削除は認めない。 */
    public static LocalDate minimumDeadline(int taxYear) {
        return deadline(LocalDate.of(taxYear + 1, 3, 16), 5);
    }

    /** 削除可否の判定に使う実効期限（登録期限と最低保存期限の遅い方）。 */
    public static LocalDate effectiveDeadline(LocalDate storedDeadline, int taxYear) {
        LocalDate floor = minimumDeadline(taxYear);
        return storedDeadline.isBefore(floor) ? floor : storedDeadline;
    }
}
