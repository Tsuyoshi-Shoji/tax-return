package org.example.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.example.entity.AppUser;
import org.example.features.setting.TaxReturnSection;
import org.example.features.setting.TaxReturnSectionVisibility;
import org.example.features.setting.dto.SettingOperationResult;
import org.example.features.setting.dto.TaxReturnSectionOption;
import org.example.features.setting.form.TaxReturnSectionForm;
import org.example.features.setting.service.SettingService;
import org.example.features.setting.service.impl.SettingServiceImpl;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** 確定申告画面のセクション表示設定（保存・必須セクションの保護・データありの強制表示）の検証。 */
public final class TaxReturnSectionSettingHarness {
    public static void main(String[] args) throws Exception {
        verifyVisibilityRules();
        verifyService();
        System.out.println("Tax return section setting harness passed");
    }

    private static void verifyVisibilityRules() {
        require(TaxReturnSection.parseHidden("fixed-assets, basic-info,unknown,consumption")
                        .equals(EnumSet.of(TaxReturnSection.FIXED_ASSETS, TaxReturnSection.CONSUMPTION)),
                "必須・未知コードは非表示として復元しないこと");
        require(TaxReturnSection.formatHidden(EnumSet.noneOf(TaxReturnSection.class)) == null, "全表示は NULL で保存");

        Set<TaxReturnSection> hidden = EnumSet.of(TaxReturnSection.FIXED_ASSETS, TaxReturnSection.HOUSEHOLD_CONSUMPTION);
        TaxReturnSectionVisibility none = TaxReturnSectionVisibility.resolve(hidden, EnumSet.noneOf(TaxReturnSection.class));
        require(!none.getShow().get("fixed-assets") && !none.getShow().get("household-consumption"), "非表示設定を反映すること");
        require(none.getShow().get("salary-details") && none.getShow().get("basic-info"), "他セクションは表示すること");

        TaxReturnSectionVisibility inUse = TaxReturnSectionVisibility.resolve(hidden, EnumSet.of(TaxReturnSection.FIXED_ASSETS));
        require(inUse.getShow().get("fixed-assets") && inUse.getForced().get("fixed-assets"),
                "登録データがある任意セクションは非表示設定でも表示すること");
        require(!inUse.getForced().get("salary-details"), "非表示にしていないセクションは強制表示扱いにしないこと");

        TaxReturnSectionVisibility required = TaxReturnSectionVisibility.resolve(
                EnumSet.allOf(TaxReturnSection.class), EnumSet.noneOf(TaxReturnSection.class));
        for (TaxReturnSection section : TaxReturnSection.values()) {
            if (section.isRequired()) {
                require(required.isShown(section), "必須セクションは常に表示: " + section);
            }
        }
    }

    private static void verifyService() throws Exception {
        Path dir = Path.of("target", "tax-return-section-harness");
        deleteRecursively(dir);
        Files.createDirectories(dir);
        String url = "jdbc:h2:file:" + dir.resolve("db").toAbsolutePath()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000";
        ProductionBootstrapHarness.applyMigrations(url, 1, 17);
        System.setProperty("entry.harness.datasource.url", url);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
            TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            Long userId = tx.execute(status -> {
                AppUser user = new AppUser();
                user.setLoginId("section@example.com");
                user.setEmail("section@example.com");
                user.setPasswordHash("hash");
                user.setDisplayName("Section");
                user.setStatus("active");
                return context.getBean(AppUserRepository.class).save(user).getId();
            });
            SettingService service = context.getBean(SettingService.class);

            require(service.loadHiddenTaxReturnSections(userId).isEmpty(), "初期状態は全表示");
            List<TaxReturnSectionOption> options = service.loadTaxReturnSectionOptions(userId);
            require(options.size() == TaxReturnSection.values().length && options.stream().allMatch(TaxReturnSectionOption::isVisible),
                    "設定画面の初期表示は全チェック");

            // UserSetting 未作成の利用者でも保存でき、未チェックの任意セクションだけが非表示になる。
            SettingOperationResult saved = service.updateTaxReturnSections(userId,
                    form("retention", "salary-details", "basic-info"));
            require(saved.isSuccess(), "保存できること: " + saved.getErrors());
            require(service.loadHiddenTaxReturnSections(userId).equals(EnumSet.of(TaxReturnSection.FIXED_ASSETS,
                    TaxReturnSection.HOUSEHOLD_CONSUMPTION, TaxReturnSection.CONSUMPTION)), "未チェックの任意セクションを非表示");
            require(stored(url, userId).equals("fixed-assets,household-consumption,consumption"), "保存値: " + stored(url, userId));
            require(rate(url, userId) == 0, "新規作成時の家事按分率は0");

            // 必須セクションは送信されなくても（画面では非活性）非表示にならない。
            require(service.updateTaxReturnSections(userId, form()).isSuccess(), "全未チェックでも保存できること");
            require(service.loadTaxReturnSectionOptions(userId).stream()
                            .allMatch(option -> option.isRequired() ? option.isVisible() : !option.isVisible()),
                    "必須は表示のまま、任意はすべて非表示");

            SettingOperationResult unknown = service.updateTaxReturnSections(userId, form("retention", "drop-table"));
            require(!unknown.isSuccess(), "未知のセクションコードは拒否すること");
            require(service.loadHiddenTaxReturnSections(userId).size() == 5, "拒否時は保存内容を変更しないこと");

            List<String> all = new ArrayList<>();
            for (TaxReturnSection section : TaxReturnSection.values()) {
                all.add(section.getCode());
            }
            require(service.updateTaxReturnSections(userId, form(all.toArray(String[]::new))).isSuccess(), "全表示に戻せること");
            require(stored(url, userId) == null, "全表示は NULL");
        }
    }

    private static TaxReturnSectionForm form(String... codes) {
        TaxReturnSectionForm form = new TaxReturnSectionForm();
        form.setVisibleSections(new ArrayList<>(List.of(codes)));
        return form;
    }

    private static String stored(String url, Long userId) throws Exception {
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT hidden_tax_return_sections FROM user_setting WHERE app_user_id = " + userId)) {
            require(rs.next(), "user_setting が作成されていること");
            return rs.getString(1);
        }
    }

    private static int rate(String url, Long userId) throws Exception {
        try (Connection c = DriverManager.getConnection(url, "sa", ""); Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT home_apportionment_rate FROM user_setting WHERE app_user_id = " + userId)) {
            rs.next();
            return rs.getBigDecimal(1).signum();
        }
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

        @Bean
        @SuppressWarnings("deprecation")
        SettingService settingService(AppUserRepository users, BusinessProfileRepository profiles,
                UserSettingRepository settings) {
            return new SettingServiceImpl(users, profiles, settings, NoOpPasswordEncoder.getInstance());
        }
    }
}
