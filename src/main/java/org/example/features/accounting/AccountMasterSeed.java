package org.example.features.accounting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 勘定科目マスタ・決算書様式欄・画面品目対応の初期値。
 * 起動時に未登録分のみ投入し、登録済みの行（利用者・運用者による変更）は上書きしない。
 */
public final class AccountMasterSeed {
    public static final String FORM_BLUE_GENERAL_PL = "BLUE_GENERAL_PL";
    /** 令和6年分 青色申告決算書（一般用）1ページ目の欄構成。様式改正時は新しい effective_from_year で行を追加する。 */
    public static final int FORM_BLUE_GENERAL_PL_FROM_YEAR = 2020;
    public static final LocalDate ITEM_MAPPING_EFFECTIVE_FROM = LocalDate.of(2000, 1, 1);

    public static final String CLASS_ASSET = "ASSET";
    public static final String CLASS_LIABILITY = "LIABILITY";
    public static final String CLASS_EQUITY = "EQUITY";
    public static final String CLASS_REVENUE = "REVENUE";
    public static final String CLASS_EXPENSE = "EXPENSE";

    public static final String KIND_INCOME = "INCOME";
    public static final String KIND_EXPENSE = "EXPENSE";

    public static final String LINE_TYPE_INPUT = "INPUT";
    public static final String LINE_TYPE_FREE = "FREE";
    public static final String LINE_TYPE_CALC = "CALC";

    public static final String ACC_CASH = "CASH";
    public static final String ACC_TOOLS_EQUIPMENT = "TOOLS_EQUIPMENT";
    public static final String ACC_LUMP_SUM_ASSETS = "LUMP_SUM_ASSETS";
    public static final String ACC_ACCOUNTS_RECEIVABLE = "ACCOUNTS_RECEIVABLE";
    public static final String ACC_ACCOUNTS_PAYABLE = "ACCOUNTS_PAYABLE";
    public static final String ACC_CAPITAL = "CAPITAL";
    public static final String ACC_RENT = "RENT";
    public static final String ACC_INTEREST_DISCOUNT = "INTEREST_DISCOUNT";
    public static final String ACC_COMMISSION = "COMMISSION";
    public static final String ACC_SALARIES = "SALARIES";
    public static final String ACC_DEDICATED_SALARY = "DEDICATED_SALARY";
    /** 固定資産台帳で選択できる資産科目。 */
    public static final List<String> FIXED_ASSET_ACCOUNTS = List.of(
            ACC_TOOLS_EQUIPMENT, "BUILDING_IMPROVEMENTS", "VEHICLES", "SOFTWARE_ASSET", ACC_LUMP_SUM_ASSETS);
    public static final String ACC_BANK_DEPOSIT = "BANK_DEPOSIT";
    public static final String ACC_OTHER_RECEIVABLE = "OTHER_RECEIVABLE";
    public static final String ACC_INVENTORY = "INVENTORY";
    public static final String ACC_ACCUMULATED_DEPRECIATION = "ACCUMULATED_DEPRECIATION";
    public static final String ACC_ACCRUED = "ACCRUED";
    public static final String ACC_OWNER_DRAW = "OWNER_DRAW";
    public static final String ACC_OWNER_CONTRIBUTION = "OWNER_CONTRIBUTION";
    public static final String ACC_SALES = "SALES";
    public static final String ACC_MISC_INCOME = "MISC_INCOME";
    public static final String ACC_PURCHASES = "PURCHASES";
    public static final String ACC_DEPRECIATION = "DEPRECIATION";
    public static final String ACC_MISC_EXPENSE = "MISC_EXPENSE";

    public static final String LINE_SALES = "SALES";
    public static final String LINE_OPENING_INVENTORY = "OPENING_INVENTORY";
    public static final String LINE_PURCHASES = "PURCHASES";
    public static final String LINE_COGS_SUBTOTAL = "COGS_SUBTOTAL";
    public static final String LINE_CLOSING_INVENTORY = "CLOSING_INVENTORY";
    public static final String LINE_COGS = "COGS";
    public static final String LINE_GROSS_PROFIT = "GROSS_PROFIT";
    public static final String LINE_DEPRECIATION = "DEPRECIATION";
    public static final String LINE_FREE_PREFIX = "FREE_";
    public static final String LINE_FREE = "FREE";
    public static final String LINE_MISC_EXPENSE = "MISC_EXPENSE";
    public static final String LINE_EXPENSE_TOTAL = "EXPENSE_TOTAL";
    public static final String LINE_NET_BEFORE_RESERVES = "NET_BEFORE_RESERVES";
    public static final String LINE_REVERSAL_BAD_DEBT = "REVERSAL_BAD_DEBT";
    public static final String LINE_REVERSAL_OTHER = "REVERSAL_OTHER";
    public static final String LINE_REVERSAL_TOTAL = "REVERSAL_TOTAL";
    public static final String LINE_DEDICATED_SALARY = "DEDICATED_SALARY";
    public static final String LINE_PROVISION_BAD_DEBT = "PROVISION_BAD_DEBT";
    public static final String LINE_PROVISION_OTHER = "PROVISION_OTHER";
    public static final String LINE_PROVISION_TOTAL = "PROVISION_TOTAL";
    public static final String LINE_INCOME_BEFORE_BLUE = "INCOME_BEFORE_BLUE";
    public static final String LINE_BLUE_DEDUCTION = "BLUE_DEDUCTION";
    public static final String LINE_INCOME = "INCOME";

    public record AccountSeed(String code, String name, String accountClass, String normalBalance, int order) {
    }

    public record LineSeed(String lineCode, int number, String label, String type) {
    }

    public record StatementMappingSeed(String accountCode, String lineCode) {
    }

    public record ItemMappingSeed(String kind, String categoryCode, String itemCode, String accountCode,
            boolean requiresReview, String reviewNote) {
    }

    public static final List<AccountSeed> ACCOUNTS = List.of(
            new AccountSeed(ACC_CASH, "現金", CLASS_ASSET, "D", 100),
            new AccountSeed(ACC_BANK_DEPOSIT, "普通預金", CLASS_ASSET, "D", 110),
            new AccountSeed("ACCOUNTS_RECEIVABLE", "売掛金", CLASS_ASSET, "D", 120),
            new AccountSeed(ACC_OTHER_RECEIVABLE, "未収入金", CLASS_ASSET, "D", 130),
            new AccountSeed("ADVANCES_PAID", "立替金", CLASS_ASSET, "D", 140),
            new AccountSeed("PREPAID_EXPENSES", "前払費用", CLASS_ASSET, "D", 150),
            new AccountSeed(ACC_INVENTORY, "棚卸資産", CLASS_ASSET, "D", 160),
            new AccountSeed("TOOLS_EQUIPMENT", "工具器具備品", CLASS_ASSET, "D", 170),
            new AccountSeed("BUILDING_IMPROVEMENTS", "建物附属設備", CLASS_ASSET, "D", 171),
            new AccountSeed("VEHICLES", "車両運搬具", CLASS_ASSET, "D", 172),
            new AccountSeed("SOFTWARE_ASSET", "ソフトウェア", CLASS_ASSET, "D", 173),
            new AccountSeed("LUMP_SUM_ASSETS", "一括償却資産", CLASS_ASSET, "D", 174),
            new AccountSeed(ACC_ACCUMULATED_DEPRECIATION, "減価償却累計額", CLASS_ASSET, "C", 175),
            new AccountSeed("DEPOSITS_PAID", "敷金・保証金", CLASS_ASSET, "D", 180),
            new AccountSeed("SUSPENSE_PAYMENT", "仮払金", CLASS_ASSET, "D", 190),
            new AccountSeed(ACC_OWNER_DRAW, "事業主貸", CLASS_EQUITY, "D", 195),
            new AccountSeed("ACCOUNTS_PAYABLE", "買掛金", CLASS_LIABILITY, "C", 200),
            new AccountSeed(ACC_ACCRUED, "未払金", CLASS_LIABILITY, "C", 210),
            new AccountSeed("LOANS", "借入金", CLASS_LIABILITY, "C", 220),
            new AccountSeed("DEPOSITS_RECEIVED", "預り金", CLASS_LIABILITY, "C", 230),
            new AccountSeed("SUSPENSE_RECEIPT", "仮受金", CLASS_LIABILITY, "C", 240),
            new AccountSeed(ACC_OWNER_CONTRIBUTION, "事業主借", CLASS_EQUITY, "C", 300),
            new AccountSeed("CAPITAL", "元入金", CLASS_EQUITY, "C", 310),
            new AccountSeed(ACC_SALES, "売上高", CLASS_REVENUE, "C", 400),
            new AccountSeed(ACC_MISC_INCOME, "雑収入", CLASS_REVENUE, "C", 410),
            new AccountSeed(ACC_PURCHASES, "仕入高", CLASS_EXPENSE, "D", 500),
            new AccountSeed("TAXES_DUES", "租税公課", CLASS_EXPENSE, "D", 508),
            new AccountSeed("PACKING_FREIGHT", "荷造運賃", CLASS_EXPENSE, "D", 509),
            new AccountSeed("UTILITIES", "水道光熱費", CLASS_EXPENSE, "D", 510),
            new AccountSeed("TRAVEL", "旅費交通費", CLASS_EXPENSE, "D", 511),
            new AccountSeed("COMMUNICATION", "通信費", CLASS_EXPENSE, "D", 512),
            new AccountSeed("ADVERTISING", "広告宣伝費", CLASS_EXPENSE, "D", 513),
            new AccountSeed("ENTERTAINMENT", "接待交際費", CLASS_EXPENSE, "D", 514),
            new AccountSeed("INSURANCE", "損害保険料", CLASS_EXPENSE, "D", 515),
            new AccountSeed("REPAIRS", "修繕費", CLASS_EXPENSE, "D", 516),
            new AccountSeed("SUPPLIES", "消耗品費", CLASS_EXPENSE, "D", 517),
            new AccountSeed(ACC_DEPRECIATION, "減価償却費", CLASS_EXPENSE, "D", 518),
            new AccountSeed("WELFARE", "福利厚生費", CLASS_EXPENSE, "D", 519),
            new AccountSeed("SALARIES", "給料賃金", CLASS_EXPENSE, "D", 520),
            new AccountSeed("OUTSOURCING", "外注工賃", CLASS_EXPENSE, "D", 521),
            new AccountSeed("INTEREST_DISCOUNT", "利子割引料", CLASS_EXPENSE, "D", 522),
            new AccountSeed("RENT", "地代家賃", CLASS_EXPENSE, "D", 523),
            new AccountSeed("BAD_DEBTS", "貸倒金", CLASS_EXPENSE, "D", 524),
            new AccountSeed("COMMISSION", "支払手数料", CLASS_EXPENSE, "D", 525),
            new AccountSeed("VEHICLE", "車両費", CLASS_EXPENSE, "D", 526),
            new AccountSeed("MEETING", "会議費", CLASS_EXPENSE, "D", 527),
            new AccountSeed("TRAINING", "研修費", CLASS_EXPENSE, "D", 528),
            new AccountSeed("BOOKS", "新聞図書費", CLASS_EXPENSE, "D", 529),
            new AccountSeed("MEMBERSHIP", "諸会費", CLASS_EXPENSE, "D", 530),
            new AccountSeed("LEGAL_WELFARE", "法定福利費", CLASS_EXPENSE, "D", 531),
            new AccountSeed(ACC_MISC_EXPENSE, "雑費", CLASS_EXPENSE, "D", 539),
            new AccountSeed("DEDICATED_SALARY", "専従者給与", CLASS_EXPENSE, "D", 540));

    public static final List<LineSeed> BLUE_GENERAL_PL_LINES = List.of(
            new LineSeed(LINE_SALES, 1, "売上（収入）金額（雑収入を含む）", LINE_TYPE_INPUT),
            new LineSeed(LINE_OPENING_INVENTORY, 2, "期首商品（製品）棚卸高", LINE_TYPE_INPUT),
            new LineSeed(LINE_PURCHASES, 3, "仕入金額（製品製造原価）", LINE_TYPE_INPUT),
            new LineSeed(LINE_COGS_SUBTOTAL, 4, "小計（②＋③）", LINE_TYPE_CALC),
            new LineSeed(LINE_CLOSING_INVENTORY, 5, "期末商品（製品）棚卸高", LINE_TYPE_INPUT),
            new LineSeed(LINE_COGS, 6, "差引原価（④－⑤）", LINE_TYPE_CALC),
            new LineSeed(LINE_GROSS_PROFIT, 7, "差引金額（①－⑥）", LINE_TYPE_CALC),
            new LineSeed("TAXES_DUES", 8, "租税公課", LINE_TYPE_INPUT),
            new LineSeed("PACKING_FREIGHT", 9, "荷造運賃", LINE_TYPE_INPUT),
            new LineSeed("UTILITIES", 10, "水道光熱費", LINE_TYPE_INPUT),
            new LineSeed("TRAVEL", 11, "旅費交通費", LINE_TYPE_INPUT),
            new LineSeed("COMMUNICATION", 12, "通信費", LINE_TYPE_INPUT),
            new LineSeed("ADVERTISING", 13, "広告宣伝費", LINE_TYPE_INPUT),
            new LineSeed("ENTERTAINMENT", 14, "接待交際費", LINE_TYPE_INPUT),
            new LineSeed("INSURANCE", 15, "損害保険料", LINE_TYPE_INPUT),
            new LineSeed("REPAIRS", 16, "修繕費", LINE_TYPE_INPUT),
            new LineSeed("SUPPLIES", 17, "消耗品費", LINE_TYPE_INPUT),
            new LineSeed(LINE_DEPRECIATION, 18, "減価償却費", LINE_TYPE_INPUT),
            new LineSeed("WELFARE", 19, "福利厚生費", LINE_TYPE_INPUT),
            new LineSeed("SALARIES", 20, "給料賃金", LINE_TYPE_INPUT),
            new LineSeed("OUTSOURCING", 21, "外注工賃", LINE_TYPE_INPUT),
            new LineSeed("INTEREST_DISCOUNT", 22, "利子割引料", LINE_TYPE_INPUT),
            new LineSeed("RENT", 23, "地代家賃", LINE_TYPE_INPUT),
            new LineSeed("BAD_DEBTS", 24, "貸倒金", LINE_TYPE_INPUT),
            new LineSeed(LINE_FREE_PREFIX + "1", 25, "（任意記載欄）", LINE_TYPE_FREE),
            new LineSeed(LINE_FREE_PREFIX + "2", 26, "（任意記載欄）", LINE_TYPE_FREE),
            new LineSeed(LINE_FREE_PREFIX + "3", 27, "（任意記載欄）", LINE_TYPE_FREE),
            new LineSeed(LINE_FREE_PREFIX + "4", 28, "（任意記載欄）", LINE_TYPE_FREE),
            new LineSeed(LINE_FREE_PREFIX + "5", 29, "（任意記載欄）", LINE_TYPE_FREE),
            new LineSeed(LINE_FREE_PREFIX + "6", 30, "（任意記載欄）", LINE_TYPE_FREE),
            new LineSeed(LINE_MISC_EXPENSE, 31, "雑費", LINE_TYPE_INPUT),
            new LineSeed(LINE_EXPENSE_TOTAL, 32, "経費 計", LINE_TYPE_CALC),
            new LineSeed(LINE_NET_BEFORE_RESERVES, 33, "差引金額（⑦－㉜）", LINE_TYPE_CALC),
            new LineSeed(LINE_REVERSAL_BAD_DEBT, 34, "繰戻額等 貸倒引当金", LINE_TYPE_INPUT),
            new LineSeed(LINE_REVERSAL_OTHER, 35, "繰戻額等 その他", LINE_TYPE_INPUT),
            new LineSeed(LINE_REVERSAL_TOTAL, 36, "繰戻額等 計", LINE_TYPE_CALC),
            new LineSeed(LINE_DEDICATED_SALARY, 37, "繰入額等 専従者給与", LINE_TYPE_INPUT),
            new LineSeed(LINE_PROVISION_BAD_DEBT, 38, "繰入額等 貸倒引当金", LINE_TYPE_INPUT),
            new LineSeed(LINE_PROVISION_OTHER, 39, "繰入額等 その他", LINE_TYPE_INPUT),
            new LineSeed(LINE_PROVISION_TOTAL, 40, "繰入額等 計", LINE_TYPE_CALC),
            new LineSeed(LINE_INCOME_BEFORE_BLUE, 41, "青色申告特別控除前の所得金額（㉝＋㊱－㊵）", LINE_TYPE_CALC),
            new LineSeed(LINE_BLUE_DEDUCTION, 42, "青色申告特別控除額", LINE_TYPE_CALC),
            new LineSeed(LINE_INCOME, 43, "所得金額（㊶－㊷）", LINE_TYPE_CALC));

    /** 任意記載欄（㉕〜㉚）へ割り当てる科目は lineCode に {@link #LINE_FREE} を指定する。 */
    public static final List<StatementMappingSeed> BLUE_GENERAL_PL_MAPPINGS = List.of(
            new StatementMappingSeed(ACC_SALES, LINE_SALES),
            new StatementMappingSeed(ACC_MISC_INCOME, LINE_SALES),
            new StatementMappingSeed(ACC_PURCHASES, LINE_PURCHASES),
            new StatementMappingSeed("TAXES_DUES", "TAXES_DUES"),
            new StatementMappingSeed("PACKING_FREIGHT", "PACKING_FREIGHT"),
            new StatementMappingSeed("UTILITIES", "UTILITIES"),
            new StatementMappingSeed("TRAVEL", "TRAVEL"),
            new StatementMappingSeed("COMMUNICATION", "COMMUNICATION"),
            new StatementMappingSeed("ADVERTISING", "ADVERTISING"),
            new StatementMappingSeed("ENTERTAINMENT", "ENTERTAINMENT"),
            new StatementMappingSeed("INSURANCE", "INSURANCE"),
            new StatementMappingSeed("REPAIRS", "REPAIRS"),
            new StatementMappingSeed("SUPPLIES", "SUPPLIES"),
            new StatementMappingSeed(ACC_DEPRECIATION, LINE_DEPRECIATION),
            new StatementMappingSeed("WELFARE", "WELFARE"),
            new StatementMappingSeed("SALARIES", "SALARIES"),
            new StatementMappingSeed("OUTSOURCING", "OUTSOURCING"),
            new StatementMappingSeed("INTEREST_DISCOUNT", "INTEREST_DISCOUNT"),
            new StatementMappingSeed("RENT", "RENT"),
            new StatementMappingSeed("BAD_DEBTS", "BAD_DEBTS"),
            new StatementMappingSeed("COMMISSION", LINE_FREE),
            new StatementMappingSeed("VEHICLE", LINE_FREE),
            new StatementMappingSeed("MEETING", LINE_FREE),
            new StatementMappingSeed("TRAINING", LINE_FREE),
            new StatementMappingSeed("BOOKS", LINE_FREE),
            new StatementMappingSeed("MEMBERSHIP", LINE_FREE),
            new StatementMappingSeed("LEGAL_WELFARE", LINE_FREE),
            new StatementMappingSeed(ACC_MISC_EXPENSE, LINE_MISC_EXPENSE),
            new StatementMappingSeed("DEDICATED_SALARY", LINE_DEDICATED_SALARY));

    private static final String ASSET_REVIEW = "取得価額・使用可能期間により固定資産・一括償却資産等の判定が必要です。";

    public static final List<ItemMappingSeed> ITEM_MAPPINGS = buildItemMappings();

    private AccountMasterSeed() {
    }

    private static List<ItemMappingSeed> buildItemMappings() {
        List<ItemMappingSeed> list = new ArrayList<>();
        for (String item : List.of("product", "service", "ses-contract", "contract-system-development", "it-training",
                "technical-content", "saas-subscription", "software-license-sales", "advertising-affiliate")) {
            list.add(income("sales", item, ACC_SALES));
        }
        list.add(review(KIND_INCOME, "sales", "other-sales", ACC_SALES, "内容に応じて科目の妥当性を確認してください。"));
        for (String item : List.of("interest", "commission", "client-gratuity", "business-misc-income")) {
            list.add(income("misc", item, ACC_MISC_INCOME));
        }
        list.add(review(KIND_INCOME, "misc", "business-subsidy", ACC_MISC_INCOME, "交付条件・非課税特例等を個別に確認してください。"));
        list.add(review(KIND_INCOME, "misc", "business-insurance", ACC_MISC_INCOME, "補償の性質により収益計上の要否を確認してください。"));
        list.add(review(KIND_INCOME, "misc", "business-cashback", ACC_MISC_INCOME, "値引き・経費返還の性質なら原取引の減額を優先してください。"));
        list.add(review(KIND_INCOME, "misc", "other-misc-income", ACC_MISC_INCOME, "事業外所得・非課税取引でないか確認してください。"));
        list.add(income("other", "owner-loan", ACC_OWNER_CONTRIBUTION));
        list.add(income("other", "loan-proceeds", "LOANS"));
        list.add(income("other", "deposit-received", "DEPOSITS_RECEIVED"));
        list.add(income("other", "reimbursement-collected", "ADVANCES_PAID"));
        list.add(income("other", "erroneous-deposit-adjustment", "SUSPENSE_RECEIPT"));

        String[][] expenseItems = {
                {"supplies", "SUPPLIES"}, {"communication", "COMMUNICATION"}, {"travel", "TRAVEL"},
                {"utilities", "UTILITIES"}, {"cloud-services", "COMMUNICATION"}, {"server-hosting", "COMMUNICATION"},
                {"domain", "COMMUNICATION"}, {"ssl-security", "COMMUNICATION"}, {"ide-development-tools", "COMMUNICATION"},
                {"business-tools", "COMMUNICATION"}, {"api-usage", "COMMUNICATION"}, {"database-storage", "COMMUNICATION"},
                {"office-supplies", "SUPPLIES"}, {"equipment-maintenance", "REPAIRS"},
                {"business-communication", "COMMUNICATION"}, {"electricity", "UTILITIES"}, {"gas-water", "UTILITIES"},
                {"office-rent", "RENT"}, {"home-office-rent", "RENT"}, {"workspace-rent", "RENT"},
                {"postage-delivery", "COMMUNICATION"}, {"transportation", "TRAVEL"}, {"business-trip-lodging", "TRAVEL"},
                {"parking-tolls", "TRAVEL"}, {"fuel", "VEHICLE"}, {"entertainment", "ENTERTAINMENT"},
                {"gifts", "ENTERTAINMENT"}, {"sales-meetings", "MEETING"}, {"events", "TRAINING"},
                {"advertising", "ADVERTISING"}, {"learning-materials", "BOOKS"}, {"online-learning", "TRAINING"},
                {"training-seminars", "TRAINING"}, {"tax-accountant", "COMMISSION"}, {"legal-professional", "COMMISSION"},
                {"outsourcing", "OUTSOURCING"}, {"payment-fees", "COMMISSION"}, {"business-insurance", "INSURANCE"},
                {"business-taxes", "TAXES_DUES"}, {"loan-interest", "INTEREST_DISCOUNT"}, {"depreciation", ACC_DEPRECIATION},
                {"employee-benefits-statutory", "LEGAL_WELFARE"}, {"employee-welfare", "WELFARE"},
                {"blue-dedicated-employee-salary", "DEDICATED_SALARY"}, {"recruitment-advertising", "ADVERTISING"},
                {"recruitment-agency", "COMMISSION"}, {"external-engineer", "OUTSOURCING"},
                {"design-production-outsourcing", "OUTSOURCING"}};
        for (String[] pair : expenseItems) {
            list.add(new ItemMappingSeed(KIND_EXPENSE, "expense", pair[0], pair[1], false, null));
        }
        for (String item : List.of("software-license-purchase", "devices", "peripherals", "office-furniture")) {
            list.add(review(KIND_EXPENSE, "expense", item, "SUPPLIES", ASSET_REVIEW));
        }
        list.add(review(KIND_EXPENSE, "expense", "certification-exam", "TRAINING", "事業に直接必要な資格か確認してください。"));
        list.add(review(KIND_EXPENSE, "expense", "certification-renewal", "TRAINING", "更新内容により諸会費等も検討してください。"));
        list.add(review(KIND_EXPENSE, "expense", "community-membership", "MEMBERSHIP", "私的な交流は事業主貸として扱ってください。"));

        for (String item : List.of("goods", "materials", "software", "hardware", "digital-content",
                "raw-material-components", "other-purchase")) {
            list.add(new ItemMappingSeed(KIND_EXPENSE, "purchase", item, ACC_PURCHASES, false, null));
        }

        for (String item : List.of("living-expenses", "entertainment-expenses", "income-resident-tax", "national-health-pension")) {
            list.add(new ItemMappingSeed(KIND_EXPENSE, "other", item, ACC_OWNER_DRAW, false, null));
        }
        list.add(new ItemMappingSeed(KIND_EXPENSE, "other", "loan-principal-repayment", "LOANS", false, null));
        list.add(review(KIND_EXPENSE, "other", "fixed-asset-purchase", "TOOLS_EQUIPMENT", "資産種別・取得日・耐用年数を固定資産台帳で管理してください。"));
        list.add(new ItemMappingSeed(KIND_EXPENSE, "other", "prepaid-expense", "PREPAID_EXPENSES", false, null));
        list.add(new ItemMappingSeed(KIND_EXPENSE, "other", "security-deposit", "DEPOSITS_PAID", false, null));
        list.add(new ItemMappingSeed(KIND_EXPENSE, "other", "reimbursement-paid", "ADVANCES_PAID", false, null));
        list.add(new ItemMappingSeed(KIND_EXPENSE, "other", "deposit-paid", "DEPOSITS_RECEIVED", false, null));
        list.add(new ItemMappingSeed(KIND_EXPENSE, "other", "erroneous-payment-refund", "SUSPENSE_PAYMENT", false, null));
        return List.copyOf(list);
    }

    private static ItemMappingSeed income(String category, String item, String account) {
        return new ItemMappingSeed(KIND_INCOME, category, item, account, false, null);
    }

    private static ItemMappingSeed review(String kind, String category, String item, String account, String note) {
        return new ItemMappingSeed(kind, category, item, account, true, note);
    }
}
