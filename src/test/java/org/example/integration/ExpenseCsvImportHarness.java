package org.example.integration;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.entity.UserSetting;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.csv.ExpenseCsvImportPreparation;
import org.example.features.entry.csv.ExpenseCsvParser;
import org.example.features.entry.form.EntryForm;
import org.example.features.entry.form.MonthlyExpenseRowForm;
import org.example.features.entry.service.EntryPrepareResult;
import org.example.features.entry.service.EntryService;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.UserSettingRepository;
import javax.sql.DataSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** 支出CSV一括登録（パーサーとサービス）の検証。 */
public final class ExpenseCsvImportHarness {
    private static final Long USER_ID = 1L;
    private static final String HEADER = "区分,項目,支出発生日,金額,支払方法,申告対象外,家事按分,支出詳細\n";

    public static void main(String[] args) throws Exception {
        new ExpenseCsvImportHarness().run();
    }

    private void run() throws Exception {
        verifyParser();
        verifyServiceImport();
        System.out.println("Expense CSV import harness passed");
    }

    private void verifyParser() {
        ExpenseCsvParser.Result template = ExpenseCsvParser.parse(ExpenseCsvParser.templateBytes());
        require(template.hasErrors() && template.errors().get(0).contains("データ行がありません"),
                "テンプレートは見出し一致・データ行0件として扱うこと: " + template.errors());

        ExpenseCsvParser.Result badHeader = ExpenseCsvParser.parse("日付,金額\n2025-01-01,100\n".getBytes(StandardCharsets.UTF_8));
        require(badHeader.hasErrors(), "見出し不一致はエラーになること");

        ExpenseCsvParser.Result quoted = ExpenseCsvParser.parse((HEADER
                + "\n# コメント行\n"
                + "経費,消耗品費,2025/3/5,\"1,200\",現金,0,0,\"文具, \"\"ペン\"\"\"\n").getBytes(StandardCharsets.UTF_8));
        require(!quoted.hasErrors() && quoted.rows().size() == 1, "引用符・空行・コメント行を処理できること: " + quoted.errors());
        require(quoted.rows().get(0).amount().equals("1,200"), "引用符内のカンマを保持すること");
        require(quoted.rows().get(0).details().equals("文具, \"ペン\""), "二重引用符をエスケープ解除すること");
        require(quoted.rows().get(0).lineNumber() == 4, "行番号はファイル上の行であること");

        ExpenseCsvParser.Result sjis = ExpenseCsvParser.parse((HEADER + "経費,消耗品費,2025-03-05,100,現金,0,0,文具\n")
                .getBytes(Charset.forName("Windows-31J")));
        require(!sjis.hasErrors() && sjis.rows().size() == 1 && sjis.rows().get(0).details().equals("文具"),
                "Shift_JIS を読めること: " + sjis.errors());

        ExpenseCsvParser.Result columns = ExpenseCsvParser.parse((HEADER + "経費,消耗品費,2025-03-05\n").getBytes(StandardCharsets.UTF_8));
        require(columns.hasErrors() && columns.errors().get(0).startsWith("2行目"), "列数不足は行番号付きエラー: " + columns.errors());

        ExpenseCsvParser.Result unclosed = ExpenseCsvParser.parse((HEADER + "経費,\"消耗品費\n").getBytes(StandardCharsets.UTF_8));
        require(unclosed.hasErrors(), "閉じていない引用符はエラーになること");
    }

    private void verifyServiceImport() throws Exception {
        Path dir = Path.of("target", "expense-csv-harness");
        deleteRecursively(dir);
        Files.createDirectories(dir);
        String url = "jdbc:h2:file:" + dir.resolve("db").toAbsolutePath()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000";
        ProductionBootstrapHarness.applyMigrations(url, 1, 17);
        System.setProperty("entry.harness.datasource.url", url);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
            TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            seed(context, tx);
            EntryService service = context.getBean(EntryService.class);
            ExpenseEntryRepository expenses = context.getBean(ExpenseEntryRepository.class);

            EntryPageData page = service.loadPage(EntryKind.EXPENSE, USER_ID, null);
            String categoryId = page.getCategories().get(0).getId();
            String categoryLabel = page.getCategories().get(0).getLabel();
            String itemLabel = page.getItems().stream()
                    .filter(item -> categoryId.equals(item.getGroup()))
                    .filter(item -> "消耗品費".equals(item.getLabel()))
                    .map(item -> item.getLabel())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("消耗品費 not found"));
            String methodLabel = page.getMethods().get(0).getLabel();
            String prefix = categoryLabel + "," + itemLabel + ",";

            ExpenseCsvImportPreparation empty = service.prepareExpenseCsvImport(USER_ID, List.of());
            require(!empty.isValid(), "データ行0件は登録不可");

            long before = expenses.count();
            String mixed = HEADER
                    + prefix + "2025-03-01,1000," + methodLabel + ",0,0,ok row\n"
                    + prefix + "2025-03-02,abc," + methodLabel + ",0,0,bad amount\n"
                    + categoryLabel + ",存在しない項目,2025-03-03,500," + methodLabel + ",0,0,bad item\n"
                    + prefix + "2025-03-04,500," + methodLabel + ",2,0,bad flag\n";
            ExpenseCsvImportPreparation invalid = service.prepareExpenseCsvImport(USER_ID,
                    ExpenseCsvParser.parse(mixed.getBytes(StandardCharsets.UTF_8)).rows());
            require(!invalid.isValid() && invalid.command() == null, "エラー行があれば登録コマンドを返さないこと");
            require(invalid.errors().stream().anyMatch(e -> e.startsWith("3行目（金額）")), "金額エラーの行番号: " + invalid.errors());
            require(invalid.errors().stream().anyMatch(e -> e.startsWith("4行目（項目）")), "項目エラーの行番号: " + invalid.errors());
            require(invalid.errors().stream().anyMatch(e -> e.startsWith("5行目（申告対象外）")), "フラグエラー: " + invalid.errors());
            require(invalid.errors().stream().noneMatch(e -> e.startsWith("2行目")), "正常行はエラーにしないこと: " + invalid.errors());
            require(expenses.count() == before, "エラー時は1件も登録しないこと");

            String future = LocalDate.now().plusDays(3).toString();
            ExpenseCsvImportPreparation rejected = service.prepareExpenseCsvImport(USER_ID, ExpenseCsvParser.parse((HEADER
                    + prefix + "2025-03-01,100000000," + methodLabel + ",0,0,huge\n"
                    + prefix + future + ",100," + methodLabel + ",0,0,future\n").getBytes(StandardCharsets.UTF_8)).rows());
            require(!rejected.isValid(), "1億円以上・未来日は拒否");
            require(rejected.errors().stream().anyMatch(e -> e.startsWith("2行目（金額）") && e.contains("1億円以上")), "高額拒否: " + rejected.errors());
            require(rejected.errors().stream().anyMatch(e -> e.startsWith("3行目（支出発生日）")), "未来日拒否: " + rejected.errors());

            String valid = HEADER
                    + prefix + "2025/3/1,\"1,000\"," + methodLabel + ",0,0,文具\n"
                    + prefix + "2025-04-15,２５００円," + methodLabel + ",0,1,プリンタ用紙\n";
            ExpenseCsvImportPreparation prepared = service.prepareExpenseCsvImport(USER_ID,
                    ExpenseCsvParser.parse(valid.getBytes(StandardCharsets.UTF_8)).rows());
            require(prepared.isValid(), "正常CSVは検証を通ること: " + prepared.errors());
            require(prepared.command().getMonthlyEntries().size() == 2, "2件の登録コマンド");
            require(prepared.command().getMonthlyEntries().get(1).getAmount().compareTo(new BigDecimal("2500")) == 0,
                    "全角数字・円表記を正規化すること");
            require(prepared.command().getMonthlyEntries().get(1).getDeductibleAmount().compareTo(new BigDecimal("1875")) == 0,
                    "家事按分75%で経費額1875円");
            String saved = service.save(prepared.command()).getSuccessMessage();
            require(expenses.count() == before + 2, "2件登録されること");
            require(saved.contains("2件"), "成功メッセージ: " + saved);

            verifyMonthlyModeUnchanged(service, page, categoryId);
        }
    }

    /** 行検証を共通化したため、月間一括登録モードの挙動（対象月外の日付拒否・行キー）が変わっていないことを確認する。 */
    private void verifyMonthlyModeUnchanged(EntryService service, EntryPageData page, String categoryId) {
        String itemId = page.getItems().stream()
                .filter(item -> categoryId.equals(item.getGroup()))
                .filter(item -> "消耗品費".equals(item.getLabel()))
                .map(item -> item.getId())
                .findFirst()
                .orElseThrow();
        String methodId = page.getMethods().get(0).getId();
        EntryForm form = new EntryForm();
        form.setEntryMode("monthly");
        form.setTargetMonth("2025-03");
        form.setMonthlyRows(List.of(
                monthlyRow(categoryId, itemId, methodId, "2025-03-10"),
                monthlyRow(categoryId, itemId, methodId, "2025-04-10")));
        EntryPrepareResult outOfMonth = service.prepareSubmission(EntryKind.EXPENSE, USER_ID, form);
        require(outOfMonth.getValidatedEntry() == null
                        && outOfMonth.getPageData().getFieldErrors().containsKey("monthlyRows[1].date")
                        && !outOfMonth.getPageData().getFieldErrors().containsKey("monthlyRows[0].date"),
                "月間モードの対象月外エラー: " + outOfMonth.getPageData().getFieldErrors());

        form.setMonthlyRows(List.of(monthlyRow(categoryId, itemId, methodId, "2025-03-10")));
        EntryPrepareResult ok = service.prepareSubmission(EntryKind.EXPENSE, USER_ID, form);
        require(ok.getValidatedEntry() != null && ok.getValidatedEntry().getMonthlyEntries().size() == 1,
                "月間モード正常系: " + ok.getPageData().getFieldErrors());
    }

    private MonthlyExpenseRowForm monthlyRow(String type, String itemId, String methodId, String date) {
        MonthlyExpenseRowForm row = new MonthlyExpenseRowForm();
        row.setExpenseType(type);
        row.setCategory(itemId);
        row.setPaymentMethodId(methodId);
        row.setDate(date);
        row.setAmount("1200");
        row.setDetails("月間テスト");
        return row;
    }

    private void seed(AnnotationConfigApplicationContext context, TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            AppUser user = new AppUser();
            user.setLoginId("csv@example.com");
            user.setEmail("csv@example.com");
            user.setPasswordHash("hash");
            user.setDisplayName("Csv");
            user.setStatus("active");
            user = context.getBean(AppUserRepository.class).save(user);

            UserSetting setting = new UserSetting();
            setting.setAppUser(user);
            setting.setHomeApportionmentRate(new BigDecimal("75.00"));
            context.getBean(UserSettingRepository.class).save(setting);

            BusinessProfile profile = new BusinessProfile();
            profile.setAppUser(user);
            profile.setBusinessName("Csv Business");
            profile.setFilingType("blue");
            profile = context.getBean(BusinessProfileRepository.class).save(profile);

            FiscalYear fiscalYear = new FiscalYear();
            fiscalYear.setBusinessProfile(profile);
            fiscalYear.setTaxYear(2025);
            fiscalYear.setPeriodStart(LocalDate.of(2025, 1, 1));
            fiscalYear.setPeriodEnd(LocalDate.of(2025, 12, 31));
            fiscalYear.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(fiscalYear);
        });
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /** 未追跡の db/h2/schema.sql に依存せず、本番用マイグレーション適用済みの H2 を使う。 */
    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "org.example.repository")
    static class Config extends EntryBackendReviewHarness.HarnessConfig {
        @Override
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    System.getProperty("entry.harness.datasource.url"), "sa", "");
            dataSource.setDriverClassName("org.h2.Driver");
            return dataSource;
        }
    }
}
