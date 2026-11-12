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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 本番起動の再現（H2 MySQLモード）。本番用マイグレーション（V1〜V16）を適用し、Hibernate の validate と
 * 起動時初期化処理（AccountMasterInitializer）を、旧形式の決算整理仕訳がある状態で実行する。
 * H2 は MySQL の「ALTER TABLE ... ADD ..., ADD ...」を解釈できないため、句ごとに分割して適用する。
 */
public final class ProductionBootstrapHarness {
    private static final Pattern ALTER = Pattern.compile("(?is)^ALTER TABLE\\s+(\\w+)\\s+(.*)$");

    public static void main(String[] args) throws Exception {
        Path dir = Path.of("target", "bootstrap-harness");
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(dir);
        String url = "jdbc:h2:file:" + dir.toAbsolutePath().resolve("db") + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        System.setProperty("bootstrap.url", url);

        applyMigrations(url, 1, 4);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
            s.execute("INSERT INTO app_user (login_id, email, password_hash, display_name, status, mfa_enabled, created_at, updated_at) "
                    + "VALUES ('u@example.com','u@example.com','x','User','active',FALSE,NOW(),NOW())");
            s.execute("INSERT INTO business_profile (app_user_id, business_name, filing_type, created_at, updated_at) VALUES (1,'B','blue',NOW(),NOW())");
            s.execute("INSERT INTO fiscal_year (business_profile_id, tax_year, period_start, period_end, status, created_at, updated_at) "
                    + "VALUES (1, 2025, '2025-01-01', '2025-12-31', 'open', NOW(), NOW())");
            s.execute("INSERT INTO settlement_adjustment (fiscal_year_id, opening_inventory, closing_inventory, depreciation, accounts_receivable, "
                    + "accounts_payable, unpaid_expenses, unreceived_income, opening_balance, closing_cash, blue_deduction, settlement_status, version, created_at, updated_at) "
                    + "VALUES (1, 0, 0, 12000, 0, 0, 0, 0, 100000, 0, 65, 'SETTLED', 0, NOW(), NOW())");
            s.execute("INSERT INTO journal_entry (fiscal_year_id, source_type, source_id, entry_date, summary, status, created_at, updated_at) "
                    + "VALUES (1, 'SETTLEMENT_ADJUSTMENT', 1, '2025-12-31', '決算整理仕訳', 'draft', NOW(), NOW())");
            s.execute("INSERT INTO journal_line (journal_entry_id, line_no, debit_credit, account_name, amount, memo, created_at, updated_at) "
                    + "VALUES (1, 1, 'D', '減価償却費', 12000, '決算整理', NOW(), NOW()), (1, 2, 'C', '減価償却累計額', 12000, '決算整理', NOW(), NOW())");
        }
        applyMigrations(url, 5, 10);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
            s.execute("INSERT INTO tax_return_input (fiscal_year_id, salary_revenue, created_at, updated_at) VALUES (1, 123456, NOW(), NOW())");
        }
        applyMigrations(url, 11, 11);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement();
                var rows = s.executeQuery("SELECT salary_revenue, depreciation_rule_version, depreciation_method_basis, "
                        + "depreciation_special_confirmed, depreciation_business_months FROM tax_return_input")) {
            if (!rows.next() || rows.getInt(1) != 123456 || rows.getString(2) != null
                    || !"UNCONFIRMED".equals(rows.getString(3)) || rows.getBoolean(4) || rows.getInt(5) != 12) {
                throw new IllegalStateException("V11 changed legacy data or automatically confirmed depreciation requirements");
            }
        }
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
            s.execute("INSERT INTO fixed_asset (business_profile_id, asset_name, account_code, acquisition_date, service_start_date, "
                    + "acquisition_cost, useful_life, depreciation_method, business_ratio, acquisition_funding, created_at, updated_at) "
                    + "VALUES (1, 'Migration fixture', 'TOOLS_EQUIPMENT', '2025-01-01', '2025-01-01', 100000, 4, 'STRAIGHT_LINE', 100, 'OWNER_FUNDS', NOW(), NOW())");
        }
        applyMigrations(url, 12, 13);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement();
                var rows = s.executeQuery("SELECT acquisition_cost, useful_life, life_basis, statutory_useful_life, "
                        + "used_elapsed_months, used_requirements_confirmed, private_use_conversion, rate_transition_confirmed FROM fixed_asset")) {
            if (!rows.next() || rows.getInt(1) != 100000 || rows.getInt(2) != 4 || !"STATUTORY".equals(rows.getString(3))
                    || rows.getInt(4) != 4 || rows.getObject(5) != null || rows.getBoolean(6)
                    || rows.getBoolean(7) || rows.getBoolean(8)) {
                throw new IllegalStateException("V12 changed legacy calculation inputs or automatically confirmed an exception");
            }
        }
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement();
                var rows = s.executeQuery("SELECT asset_origin, parent_asset_id FROM fixed_asset")) {
            if (!rows.next() || !"ACQUISITION".equals(rows.getString(1)) || rows.getObject(2) != null) {
                throw new IllegalStateException("V13 changed legacy fixed-asset origin data");
            }
        }
        applyMigrations(url, 14, 14);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
            s.execute("INSERT INTO retention_record (fiscal_year_id, record_kind, document_type, title, retention_start_date, "
                    + "retention_deadline, legal_basis, created_at, updated_at) VALUES (1, 'BOOK', 'B', 'T', '2026-01-01', "
                    + "'2033-01-01', 'basis', NOW(), NOW())");
            s.execute("INSERT INTO evidence_file (retention_record_id, storage_provider, external_file_id, original_file_name, "
                    + "mime_type, file_size, sha256, uploaded_at, verification_status, created_at, updated_at) VALUES "
                    + "(1, 'GOOGLE_DRIVE', 'f1', 'a.pdf', 'application/pdf', 10, '" + "0".repeat(64) + "', NOW(), 'UPLOADED', NOW(), NOW())");
            boolean rejected = false;
            try {
                s.execute("INSERT INTO evidence_file (retention_record_id, storage_provider, external_file_id, original_file_name, "
                        + "mime_type, file_size, sha256, uploaded_at, verification_status, created_at, updated_at) VALUES "
                        + "(1, 'GOOGLE_DRIVE', 'f2', 'b.svg', 'image/svg+xml', 10, '" + "0".repeat(64) + "', NOW(), 'UPLOADED', NOW(), NOW())");
            } catch (java.sql.SQLException expected) {
                rejected = true;
            }
            if (!rejected) {
                throw new IllegalStateException("V14 accepted a second file or a disallowed MIME type");
            }
            s.execute("DELETE FROM evidence_file");
        }
        applyMigrations(url, 15, 15);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement();
                var rows = s.executeQuery("SELECT retention_deadline, retention_rule_code, rule_version, document_date, "
                        + "consumption_tax_applies, legal_hold, legal_hold_reason, legal_hold_at FROM retention_record")) {
            if (!rows.next() || !"2033-01-01".equals(rows.getString(1)) || rows.getString(2) != null || rows.getString(3) != null
                    || rows.getObject(4) != null || rows.getBoolean(5) || rows.getBoolean(6) || rows.getString(7) != null
                    || rows.getObject(8) != null) {
                throw new IllegalStateException("V15 changed legacy retention data or placed an automatic legal hold");
            }
            s.execute("DELETE FROM retention_record");
        }
        applyMigrations(url, 16, 16);
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement()) {
            String event = "INSERT INTO fiscal_year_closing_event (fiscal_year_id, app_user_id, event_type, close_version, "
                    + "sales, expenses, income, income_after_deduction, blue_deduction, assets_closing, liabilities_closing, "
                    + "journal_line_count, debit_total, credit_total, data_fingerprint, calc_rule_version, form_edition, "
                    + "occurred_at, created_at) VALUES (1, 1, '%s', 1, 0, 0, 0, 0, 65, 0, 0, 0, 0, 0, '" + "0".repeat(64)
                    + "', '2026-11-12', 'R07', NOW(), NOW())";
            s.execute(String.format(event, "CLOSE"));
            for (String invalid : List.of("CLOSE", "DELETE")) {
                boolean rejected = false;
                try {
                    s.execute(String.format(event, invalid));
                } catch (java.sql.SQLException expected) {
                    rejected = true;
                }
                if (!rejected) {
                    throw new IllegalStateException("V16 accepted a duplicate close version or an unknown event type");
                }
            }
            boolean rejected = false;
            try {
                s.execute("INSERT INTO report_output_record (fiscal_year_id, app_user_id, output_type, file_name, file_sha256, "
                        + "file_size, data_fingerprint, calc_rule_version, form_edition, year_status, record_status, "
                        + "generated_at, created_at, updated_at) VALUES (1, 1, 'pdf:return', 'a.pdf', '" + "0".repeat(64)
                        + "', 1, '" + "0".repeat(64) + "', '2026-11-12', 'R07', 'closed', 'DELETED', NOW(), NOW(), NOW())");
            } catch (java.sql.SQLException expected) {
                rejected = true;
            }
            if (!rejected) {
                throw new IllegalStateException("V16 accepted an unknown output record status");
            }
            s.execute("DELETE FROM fiscal_year_closing_event");
        }
        System.out.println("Migrations applied up to V16");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
            System.out.println("Context started: " + context.getBeanDefinitionCount() + " beans");
        }
        System.out.println("Production bootstrap harness passed");
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
