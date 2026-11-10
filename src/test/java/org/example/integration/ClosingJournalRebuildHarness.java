package org.example.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.example.config.AccountMasterInitializer;
import org.example.entity.JournalEntry;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.features.taxreturn.service.impl.ClosingJournalServiceImpl;
import org.example.repository.JournalEntryRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 決算整理仕訳の再生成（{@link ClosingJournalService#rebuildOpenYears}）が冪等であることを検証する。
 * 本番では毎回のTomcat起動時（{@link AccountMasterInitializer}）および収益・支出登録のたび（
 * {@code EntryServiceImpl#save}）に全未確定年度へ対して呼び出されるため、2回目以降の呼び出しで
 * 既存の仕訳明細を同内容の新しい明細へ置き換える際に、UNIQUE制約（journal_entry_id, line_no）
 * 違反（本番incident: "Duplicate entry '6-1' for key 'uk_journal_line_entry_line_no'"）が
 * 発生しないことを確認する。
 */
public final class ClosingJournalRebuildHarness {
    private static final Pattern ALTER = Pattern.compile("(?is)^ALTER TABLE\\s+(\\w+)\\s+(.*)$");

    public static void main(String[] args) throws Exception {
        Path dir = Path.of("target", "closing-journal-rebuild-harness");
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(dir);
        String url = "jdbc:h2:file:" + dir.toAbsolutePath().resolve("db") + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty("bootstrap.url", url);

        applyMigrations(url, 1, 6);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
            s.execute("INSERT INTO app_user (login_id, email, password_hash, display_name, status, mfa_enabled, created_at, updated_at) "
                    + "VALUES ('u@example.com','u@example.com','x','User','active',FALSE,NOW(),NOW())");
            s.execute("INSERT INTO business_profile (app_user_id, business_name, filing_type, created_at, updated_at) VALUES (1,'B','blue',NOW(),NOW())");
            s.execute("INSERT INTO fiscal_year (business_profile_id, tax_year, period_start, period_end, status, created_at, updated_at) "
                    + "VALUES (1, 2025, '2025-01-01', '2025-12-31', 'open', NOW(), NOW())");
            // unpaid_expenses(未払金の期末残高)を帳簿残高（0）と異なる値にしておくことで、
            // rebuildSettlementが毎回非空の調整仕訳（未払金／普通預金）を生成し続ける状況を再現する。
            s.execute("INSERT INTO settlement_adjustment (fiscal_year_id, opening_inventory, closing_inventory, depreciation, accounts_receivable, "
                    + "accounts_payable, unpaid_expenses, unreceived_income, opening_balance, closing_cash, blue_deduction, balances_confirmed, "
                    + "settlement_status, version, created_at, updated_at) "
                    + "VALUES (1, 0, 0, 0, 0, 0, 5000, 0, 100000, 0, 65, TRUE, 'SETTLED', 0, NOW(), NOW())");
        }

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
            // コンテキスト起動時にAccountMasterInitializerがensureMasterData()とrebuildOpenYears(1)を
            // 一度自動実行済み（本番の初回Tomcat起動に相当）。ここで決算整理仕訳が新規作成されているはず。
            JournalEntryRepository journalEntryRepository = context.getBean(JournalEntryRepository.class);
            require(settlementLineCount(journalEntryRepository) == 2, "初回rebuildで決算整理仕訳が作成されていません");

            ClosingJournalService closingJournalService = context.getBean(ClosingJournalService.class);
            // 2回目の呼び出し（2回目のTomcat起動、または同一年度内の収益・支出の追加登録に相当）。
            // 修正前はHibernateが新しい明細のINSERTを既存明細のDELETEより先に実行するため、
            // UNIQUE制約(journal_entry_id, line_no)違反で失敗していた。
            closingJournalService.rebuildOpenYears(1L);
            require(settlementLineCount(journalEntryRepository) == 2, "2回目のrebuildで決算整理仕訳の明細数が変化しました");

            // 3回目も安定して冪等であることを確認する。
            closingJournalService.rebuildOpenYears(1L);
            require(settlementLineCount(journalEntryRepository) == 2, "3回目のrebuildで決算整理仕訳の明細数が変化しました");
        }
        System.out.println("Closing journal rebuild harness passed");
    }

    private static int settlementLineCount(JournalEntryRepository journalEntryRepository) {
        List<JournalEntry> entries = journalEntryRepository.findAllWithLinesByFiscalYearId(1L);
        return entries.stream()
                .filter(e -> ClosingJournalServiceImpl.SOURCE_SETTLEMENT.equals(e.getSourceType()))
                .findFirst()
                .map(e -> e.getLines().size())
                .orElse(0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void applyMigrations(String url, int from, int to) throws Exception {
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
            for (int v = from; v <= to; v++) {
                String prefix = "V" + v + "__";
                Path file;
                try (var files = Files.list(Path.of("src", "main", "resources", "db", "migration"))) {
                    file = files.filter(p -> p.getFileName().toString().startsWith(prefix)).findFirst().orElseThrow();
                }
                for (String raw : Files.readString(file).split(";")) {
                    String sql = raw.strip();
                    if (sql.isEmpty()) {
                        continue;
                    }
                    sql = sql.replaceAll("(?s)\\)\\s*ENGINE=.*$", ")");
                    Matcher m = ALTER.matcher(sql);
                    if (m.matches()) {
                        for (String clause : splitTopLevel(m.group(2))) {
                            s.execute("ALTER TABLE " + m.group(1) + " " + clause.strip());
                        }
                    } else {
                        s.execute(sql);
                    }
                }
            }
        }
    }

    private static List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
            } else if (ch == ',' && depth == 0) {
                parts.add(body.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(body.substring(start));
        return parts;
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "org.example.repository")
    @ComponentScan(basePackages = {"org.example.features", "org.example.controller"})
    @Import(AccountMasterInitializer.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource(System.getProperty("bootstrap.url"), "sa", "");
            ds.setDriverClassName("org.h2.Driver");
            return ds;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            emf.setPackagesToScan("org.example.entity");
            emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            Properties p = new Properties();
            p.put("hibernate.hbm2ddl.auto", "validate");
            p.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            emf.setJpaProperties(p);
            return emf;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
