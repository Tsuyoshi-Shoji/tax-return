package org.example.features.taxreturn.requirements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 申告方式・青色申告特別控除の区分ごとに、必要な帳簿・決算書・提出条件を出力前に判定する。
 * 控除の適用可否は判定せず、アプリのデータで確認できる不足と利用者が確認すべき事項を区別する。
 */
public final class FilingRequirements {
    public static final String METHOD_UNCONFIRMED = "UNCONFIRMED";
    public static final String METHOD_ETAX = "ETAX";
    public static final String METHOD_QUALIFIED_EBOOKS = "QUALIFIED_EBOOKS";

    static final String WARN_REQUIREMENTS_UNCONFIRMED =
            "選択した青色申告特別控除の法定要件を利用者が確認してください。アプリは控除の適用可否を判定しません。";
    static final String WARN_65_ETAX =
            "65万円控除のe-Tax要件（申告書・貸借対照表・損益計算書等の期限内送信）は、本アプリでは送信・完了確認できません。";
    static final String WARN_65_EBOOKS = "優良な電子帳簿の要件・届出は本アプリでは実装・検証していません。";
    static final String WARN_65_UNCONFIRMED =
            "65万円控除には追加要件があります。e-Tax送信または優良な電子帳簿の要件を確認してください。";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");

    public enum Category {
        BOOK("帳簿"), STATEMENT("決算書"), SUBMISSION("提出条件");

        private final String label;
        Category(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum Necessity {
        REQUIRED("必須"), CONDITIONAL("該当時必須"), OPTIONAL("任意"), NOT_APPLICABLE("対象外");

        private final String label;
        Necessity(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum Status {
        OK("充足"), MISSING("不足"), USER_CHECK("利用者確認"), NOT_APPLICABLE("対象外");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /**
     * @param filingDeadline 対象年分の確認済み申告期限。未確定の場合は null。
     */
    public record Input(String filingType, int deduction, String deduction65Method, boolean requirementsConfirmed,
            boolean hasJournal, boolean journalBalanced, long draftCount, boolean hasProfitLoss,
            boolean balanceSheetBalanced, boolean balancesConfirmed, boolean hasFixedAssets,
            LocalDate filingDeadline, LocalDate today) {}

    public static final class Item {
        private final String id;
        private final Category category;
        private final String name;
        private final Necessity necessity;
        private final Status status;
        private final String message;

        Item(String id, Category category, String name, Necessity necessity, Status status, String message) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.necessity = necessity;
            this.status = status;
            this.message = message;
        }

        public String getId() { return id; }
        public Category getCategory() { return category; }
        public String getName() { return name; }
        public Necessity getNecessity() { return necessity; }
        public Status getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static final class Result {
        private final String modeLabel;
        private final List<Item> items;
        private final List<String> errors;
        private final List<String> warnings;

        Result(String modeLabel, List<Item> items, List<String> errors, List<String> warnings) {
            this.modeLabel = modeLabel;
            this.items = List.copyOf(items);
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
        }

        public String getModeLabel() { return modeLabel; }
        public List<Item> getItems() { return items; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public long getMissingCount() { return count(Status.MISSING); }
        public long getUserCheckCount() { return count(Status.USER_CHECK); }
        public boolean isSatisfied() { return getMissingCount() == 0; }

        public Item item(String id) {
            return items.stream().filter(i -> i.id.equals(id)).findFirst().orElse(null);
        }

        private long count(Status status) {
            return items.stream().filter(i -> i.status == status).count();
        }
    }

    private FilingRequirements() {
    }

    public static Result evaluate(Input in) {
        List<Item> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean blue = in.filingType() != null && in.filingType().startsWith("blue");
        if (!blue) {
            items.add(new Item("req-filing-type", Category.SUBMISSION, "青色申告の承認", Necessity.REQUIRED,
                    Status.MISSING, "申告方式が青色申告ではありません"));
            errors.add("申告方式が青色申告ではないため、青色申告決算書・青色申告特別控除を含む帳票は出力できません。"
                    + "白色申告の収支内訳書は本アプリの対象外です。");
            return new Result("白色申告（対象外）", items, errors, warnings);
        }
        boolean doubleEntry = in.deduction() >= 55;
        String mode = "青色申告・" + in.deduction() + "万円控除"
                + (in.deduction() == 65 ? "（" + methodLabel(in.deduction65Method()) + "）" : "");

        items.add(new Item("req-approval", Category.SUBMISSION, "青色申告承認と控除要件の利用者確認",
                Necessity.REQUIRED, in.requirementsConfirmed() ? Status.OK : Status.USER_CHECK,
                in.requirementsConfirmed() ? "利用者が確認済み（アプリは適用可否を判定しません）"
                        : "承認・控除要件を公式資料等で確認し、確認状態を保存してください"));
        if (!in.requirementsConfirmed()) {
            warnings.add(WARN_REQUIREMENTS_UNCONFIRMED);
        }

        items.add(new Item("req-transactions", Category.BOOK, "取引の基本記録（日付・相手方・金額・内容）",
                Necessity.REQUIRED, in.hasJournal() ? Status.OK : Status.USER_CHECK,
                in.hasJournal() ? "対象年分の取引から帳簿を作成済み" : "対象年分の取引がありません。事業取引がないか確認してください"));
        if (!in.hasJournal()) {
            warnings.add("対象年分の取引・仕訳がありません。帳簿を出力する前に、事業取引がないことを確認してください。");
        }

        if (doubleEntry) {
            Status journal;
            String journalMessage;
            if (!in.hasJournal()) {
                journal = Status.MISSING;
                journalMessage = "仕訳がないため、正規の簿記による仕訳帳・総勘定元帳を作成できません";
            } else if (!in.journalBalanced()) {
                journal = Status.MISSING;
                journalMessage = "借方と貸方が一致していない仕訳があります";
            } else if (in.draftCount() > 0) {
                journal = Status.USER_CHECK;
                journalMessage = "未確認の仕訳が" + in.draftCount() + "件あります";
            } else {
                journal = Status.OK;
                journalMessage = "複式簿記の仕訳から作成済み";
            }
            items.add(new Item("req-journal-ledger", Category.BOOK, "仕訳帳・総勘定元帳（正規の簿記）",
                    Necessity.REQUIRED, journal, journalMessage));
            items.add(new Item("req-simple-books", Category.BOOK, "簡易帳簿（現金出納帳・売掛帳・買掛帳・経費帳）",
                    Necessity.OPTIONAL, Status.NOT_APPLICABLE, "正規の簿記の主要簿で記帳。補助簿は帳簿CSVで参考出力できます"));
        } else {
            items.add(new Item("req-simple-books", Category.BOOK, "簡易帳簿（現金出納帳・売掛帳・買掛帳・経費帳）",
                    Necessity.REQUIRED, in.hasJournal() ? Status.OK : Status.USER_CHECK,
                    in.hasJournal() ? "取引から作成済み（帳簿CSVで出力）" : "取引がありません"));
            items.add(new Item("req-journal-ledger", Category.BOOK, "仕訳帳・総勘定元帳（正規の簿記）",
                    Necessity.OPTIONAL, Status.NOT_APPLICABLE, "10万円控除の必須要件ではありません（参考出力可）"));
        }

        items.add(new Item("req-fixed-asset-ledger", Category.BOOK, "固定資産台帳・減価償却の計算",
                in.hasFixedAssets() ? Necessity.CONDITIONAL : Necessity.NOT_APPLICABLE,
                in.hasFixedAssets() ? Status.OK : Status.NOT_APPLICABLE,
                in.hasFixedAssets() ? "登録資産から作成済み（固定資産台帳CSV・決算書3ページ）" : "登録された固定資産がありません"));

        items.add(new Item("req-profit-loss", Category.STATEMENT, "青色申告決算書（損益計算書・内訳）",
                Necessity.REQUIRED, in.hasProfitLoss() ? Status.OK : Status.MISSING,
                in.hasProfitLoss() ? "帳簿から集計済み" : "損益データがありません"));
        if (!in.hasProfitLoss()) {
            warnings.add("青色申告決算書の損益計算書に集計する損益データがありません。");
        }

        if (doubleEntry) {
            Status bs;
            String bsMessage;
            if (!in.balanceSheetBalanced()) {
                bs = Status.MISSING;
                bsMessage = "貸借が一致していません";
            } else if (!in.balancesConfirmed()) {
                bs = Status.USER_CHECK;
                bsMessage = "決算整理（期末残高）が未保存です";
                warnings.add(in.deduction() + "万円控除では貸借対照表の添付が必要です。決算整理（期末残高）を保存し、内容を確認してください。");
            } else {
                bs = Status.OK;
                bsMessage = "帳簿残高と決算整理から作成済み";
            }
            items.add(new Item("req-balance-sheet", Category.STATEMENT, "貸借対照表（決算書4ページ）",
                    Necessity.REQUIRED, bs, bsMessage));
        } else {
            items.add(new Item("req-balance-sheet", Category.STATEMENT, "貸借対照表（決算書4ページ）",
                    Necessity.OPTIONAL, Status.NOT_APPLICABLE, "10万円控除では添付は必須ではありません"));
        }

        if (doubleEntry) {
            Status deadline;
            String deadlineMessage;
            if (in.filingDeadline() == null) {
                deadline = Status.USER_CHECK;
                deadlineMessage = "対象年分の申告期限は未確定です。国税庁の年分別案内で確認してください";
            } else if (in.today() != null && in.today().isAfter(in.filingDeadline())) {
                deadline = Status.MISSING;
                deadlineMessage = "申告期限（" + in.filingDeadline().format(DATE) + "）を過ぎています";
                warnings.add("申告期限（" + in.filingDeadline().format(DATE) + "）を過ぎています。期限後申告では"
                        + "55万円・65万円控除は適用されず、10万円控除が上限となります。控除区分を確認してください。");
            } else {
                deadline = Status.USER_CHECK;
                deadlineMessage = in.filingDeadline().format(DATE) + "までに申告書と決算書を提出してください";
            }
            items.add(new Item("req-deadline", Category.SUBMISSION, "申告書・貸借対照表・損益計算書の期限内提出",
                    Necessity.REQUIRED, deadline, deadlineMessage));
        } else {
            items.add(new Item("req-deadline", Category.SUBMISSION, "申告書・貸借対照表・損益計算書の期限内提出",
                    Necessity.NOT_APPLICABLE, Status.NOT_APPLICABLE, "10万円控除の要件ではありません（所得税の申告期限は別途確認）"));
        }

        if (in.deduction() == 65) {
            String method = in.deduction65Method() == null ? METHOD_UNCONFIRMED : in.deduction65Method();
            if (METHOD_ETAX.equals(method)) {
                items.add(new Item("req-65-additional", Category.SUBMISSION, "65万円控除の追加要件（e-Tax送信）",
                        Necessity.REQUIRED, Status.USER_CHECK,
                        "申告書・貸借対照表・損益計算書等をe-Taxで期限内に送信してください。本アプリの帳票は転記用で送信はしません"));
                warnings.add(WARN_65_ETAX);
            } else if (METHOD_QUALIFIED_EBOOKS.equals(method)) {
                items.add(new Item("req-65-additional", Category.SUBMISSION, "65万円控除の追加要件（優良な電子帳簿）",
                        Necessity.REQUIRED, Status.MISSING,
                        "本アプリは優良な電子帳簿の要件・届出を実装していないため、本アプリの帳簿ではこの要件を満たせません"));
                warnings.add(WARN_65_EBOOKS);
            } else {
                items.add(new Item("req-65-additional", Category.SUBMISSION, "65万円控除の追加要件",
                        Necessity.REQUIRED, Status.MISSING, "e-Tax送信または優良な電子帳簿のいずれかを選択・確認してください"));
                warnings.add(WARN_65_UNCONFIRMED);
            }
        } else {
            items.add(new Item("req-65-additional", Category.SUBMISSION, "65万円控除の追加要件",
                    Necessity.NOT_APPLICABLE, Status.NOT_APPLICABLE, "選択した控除区分では対象外です"));
        }
        return new Result(mode, items, errors, warnings);
    }

    private static String methodLabel(String method) {
        if (METHOD_ETAX.equals(method)) {
            return "e-Tax送信";
        }
        if (METHOD_QUALIFIED_EBOOKS.equals(method)) {
            return "優良な電子帳簿";
        }
        return "追加要件未確認";
    }
}
