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
 * 本番起動の再現（H2 MySQLモード）。本番用マイグレーション（V1〜V6）を適用し、Hibernate の validate と
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
        applyMigrations(url, 5, 6);
        System.out.println("Migrations applied up to V6");

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
