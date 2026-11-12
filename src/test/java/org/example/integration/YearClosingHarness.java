package org.example.integration;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.entity.JournalEntry;
import org.example.entity.ReportOutputRecord;
import org.example.entity.UserSetting;
import org.example.features.accounting.service.AccountMappingService;
import org.example.features.accounting.service.impl.AccountMappingServiceImpl;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.entry.EntryOption;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.form.EntryForm;
import org.example.features.entry.service.EntryPrepareResult;
import org.example.features.entry.service.EntryService;
import org.example.features.entry.service.impl.EntryServiceImpl;
import org.example.features.taxreturn.balance.BalanceSheetService;
import org.example.features.accounting.AccountMasterSeed;
import org.example.features.taxreturn.closing.ClosingPageData;
import org.example.features.taxreturn.closing.YearSnapshot;
import org.example.features.taxreturn.closing.YearClosingService;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.form.SettlementForm;
import org.example.features.taxreturn.input.TaxReturnInputService;
import org.example.features.taxreturn.service.TaxReturnService;
import org.example.features.taxreturn.service.impl.ClosingJournalServiceImpl;
import org.example.features.taxreturn.service.impl.TaxReturnServiceImpl;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.ReportOutputRecordRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 年度締め・締めの取消・再締め時の訂正履歴、確定済み年度のデータ変化検知、出力帳票の生成記録・取消の結合確認。
 * 実行: mvn test-compile 後に main を実行（H2ファイルDBを target/year-closing-harness に作成）。
 */
public final class YearClosingHarness {
    private static final int YEAR = 2025;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 10, 0);

    public static void main(String[] args) throws Exception {
        Path dir = Path.of("target", "year-closing-harness");
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(dir);
        System.setProperty("year.closing.harness.url",
                "jdbc:h2:file:" + dir.toAbsolutePath().resolve("db") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(HarnessConfig.class)) {
            new YearClosingHarness().run(context);
        }
        System.out.println("Year closing harness passed");
    }

    private void run(AnnotationConfigApplicationContext context) {
        context.getBean(AccountMappingService.class).ensureMasterData();
        EntryService entryService = context.getBean(EntryService.class);
        TaxReturnService taxReturnService = context.getBean(TaxReturnService.class);
        YearClosingService closing = context.getBean(YearClosingService.class);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        Long userId = seedUser(context, tx, "closing@example.com", YEAR - 1, YEAR);
        EntryPageData incomePage = entryService.loadPage(EntryKind.INCOME, userId, null);
        EntryPageData expensePage = entryService.loadPage(EntryKind.EXPENSE, userId, null);
        String incomeBank = optionId(incomePage.getMethods(), "銀行振込");
        String bank = optionId(expensePage.getMethods(), "銀行振込");
        saveEntry(entryService, EntryKind.INCOME, userId, incomePage, "sales", "SES・準委任契約報酬", "2025-03-31",
                "1000000", incomeBank, "3月分SES");
        saveEntry(entryService, EntryKind.EXPENSE, userId, expensePage, "expense",
                "クラウド利用料（AWS・Azure・GCP等）", "2025-04-10", "5000", bank, "AWS");

        TaxReturnPageData open = taxReturnService.load(userId, YEAR, null);
        require(open.getErrors().isEmpty(), "締め前のページにエラーがあります: " + open.getErrors());

        List<String> priorOpen = closing.close(userId, YEAR, true, null, NOW);
        require(priorOpen.size() == 1 && priorOpen.get(0).contains("2024年分"), "前年未締めで締めを拒否していません: " + priorOpen);
        require(closing.close(userId, YEAR - 1, true, null, NOW).isEmpty(), "前年の締めに失敗しました");
        require(closing.close(userId, YEAR - 1, true, null, NOW).get(0).contains("既に締めています"), "二重締めを拒否していません");

        if (!open.getWarnings().isEmpty()) {
            List<String> unconfirmed = closing.close(userId, YEAR, false, null, NOW);
            require(unconfirmed.size() == 1 && unconfirmed.get(0).contains("警告"), "警告未確認の締めを拒否していません: " + unconfirmed);
        }
        String tooLong = "あ".repeat(YearClosingService.MAX_REASON_LENGTH + 1);
        require(closing.close(userId, YEAR, true, tooLong, NOW).get(0).contains("200文字"), "長すぎる理由を拒否していません");
        require(closing.close(userId, YEAR, true, null, NOW).isEmpty(), "締めに失敗しました");
        require("closed".equals(status(context, tx, userId, YEAR)) && closedAt(context, tx, userId, YEAR) != null,
                "締め後の年度状態が closed になっていません");

        SettlementForm settlement = new SettlementForm();
        settlement.setOpeningInventory("0");
        settlement.setClosingInventory("1000");
        settlement.setOpeningBalance("0");
        require(!taxReturnService.saveSettlement(userId, YEAR, settlement).isEmpty(), "締め済み年度の決算整理を変更できてしまいます");
        EntryForm blocked = entryForm(incomePage, "sales", "SES・準委任契約報酬", "2025-05-31", "1", incomeBank, "締め後");
        blocked.setIncomeType("sales");
        blocked.setReceiveMethodId(incomeBank);
        boolean entryBlocked;
        try {
            EntryPrepareResult prepared = entryService.prepareSubmission(EntryKind.INCOME, userId, blocked);
            entryBlocked = prepared.getValidatedEntry() == null;
            if (!entryBlocked) {
                entryService.save(prepared.getValidatedEntry());
            }
        } catch (RuntimeException ex) {
            entryBlocked = true;
        }
        require(entryBlocked, "締め済み年度に仕訳を登録できてしまいます");

        TaxReturnPageData closedPage = taxReturnService.load(userId, YEAR, null);
        require(closedPage.getErrors().isEmpty(), "締め直後にエラーが出ています: " + closedPage.getErrors());
        verifyFingerprintCompatibility(context, taxReturnService, userId, closedPage);
        byte[] pdf = "%PDF-test".getBytes(StandardCharsets.UTF_8);
        closing.recordOutput(userId, closedPage, "pdf:return", "tax-return_blue-return_2025.pdf", pdf, NOW);
        ClosingPageData view = closing.view(userId, closedPage);
        require(view.isClosed() && Integer.valueOf(1).equals(view.getCurrentVersion()), "締め版が1になっていません");
        require(view.getEvents().size() == 1 && "初回の締め".equals(view.getEvents().get(0).getChangeSummary()),
                "初回の締め履歴が想定と異なります");
        ClosingPageData.OutputView output = view.getOutputs().get(0);
        require(output.isMatchesCurrent() && "CURRENT".equals(output.getStatus())
                && Integer.valueOf(1).equals(output.getCloseVersion()) && "closed".equals(output.getYearStatus())
                && output.getFileSha256().length() == 64 && output.getFileSize() == pdf.length
                && output.getFormEdition().startsWith("R07"), "出力記録の内容が想定と異なります");
        boolean unknownRejected = false;
        try {
            closing.recordOutput(userId, closedPage, "pdf:../../etc", "x.pdf", pdf, NOW);
        } catch (IllegalArgumentException ex) {
            unknownRejected = true;
        }
        require(unknownRejected, "未知の出力種別を記録できてしまいます");

        List<String> laterClosed = closing.reopen(userId, YEAR - 1, "前年訂正", NOW);
        require(laterClosed.size() == 1 && laterClosed.get(0).contains("2025年分"), "翌年締め済みで前年の取消を拒否していません");

        tx.executeWithoutResult(status -> {
            FiscalYear fy = ownedYear(context, userId, YEAR);
            JournalEntry entry = context.getBean(JournalEntryRepository.class).findAllWithLinesByFiscalYearId(fy.getId())
                    .stream().filter(e -> "AWS".equals(e.getSummary())).findFirst().orElseThrow();
            entry.setSummary("AWS（直接変更）");
        });
        TaxReturnPageData drifted = taxReturnService.load(userId, YEAR, null);
        require(drifted.getErrors().stream().anyMatch(e -> e.contains("締め時点（第1版）から変化")),
                "確定済み年度のデータ変化を検知していません: " + drifted.getErrors());
        require(!closing.view(userId, drifted).getOutputs().get(0).isMatchesCurrent(), "出力記録と現データの不一致を検知していません");

        require(!closing.reopen(userId, YEAR, "  ", NOW).isEmpty(), "理由なしの締め取消を拒否していません");
        require(closing.reopen(userId, YEAR, "AWS摘要の訂正と売上計上漏れの追加", NOW.plusDays(1)).isEmpty(), "締めの取消に失敗しました");
        require("open".equals(status(context, tx, userId, YEAR)) && closedAt(context, tx, userId, YEAR) == null,
                "締め取消後の年度状態が open になっていません");
        TaxReturnPageData reopened = taxReturnService.load(userId, YEAR, null);
        require(reopened.getErrors().isEmpty(), "締め取消後にエラーが残っています: " + reopened.getErrors());
        require("SUPERSEDED".equals(closing.view(userId, reopened).getOutputs().get(0).getStatus()),
                "締め取消で出力記録が旧版になっていません");

        saveEntry(entryService, EntryKind.INCOME, userId, incomePage, "sales", "SES・準委任契約報酬", "2025-06-30",
                "30000", incomeBank, "計上漏れ");
        require(closing.close(userId, YEAR, true, null, NOW).get(0).contains("訂正理由"), "再締めで訂正理由を必須にしていません");
        require(closing.close(userId, YEAR, true, "売上計上漏れ 30,000円を追加", NOW.plusDays(2)).isEmpty(), "再締めに失敗しました");
        TaxReturnPageData reclosed = taxReturnService.load(userId, YEAR, null);
        require(reclosed.getErrors().isEmpty(), "再締め後にエラーがあります: " + reclosed.getErrors());
        ClosingPageData history = closing.view(userId, reclosed);
        require(Integer.valueOf(2).equals(history.getCurrentVersion()) && history.getEvents().size() == 3,
                "再締めの版・履歴件数が想定と異なります");
        ClosingPageData.EventView reopenEvent = history.getEvents().get(1);
        ClosingPageData.EventView second = history.getEvents().get(2);
        require("REOPEN".equals(reopenEvent.getEventType()) && reopenEvent.getCloseVersion() == 1
                && "AWS摘要の訂正と売上計上漏れの追加".equals(reopenEvent.getReason()), "締め取消の履歴が想定と異なります");
        require(second.isClose() && second.getCloseVersion() == 2
                && second.getChangeSummary().contains("売上 1,000,000円→1,030,000円（+30,000円）")
                && second.getChangeSummary().contains("仕訳行"), "訂正差分が想定と異なります: " + second.getChangeSummary());

        Long outputId = history.getOutputs().get(0).getId();
        Long otherUser = seedUser(context, tx, "other@example.com", YEAR + 1);
        require(throwsNotFound(() -> closing.cancelOutput(otherUser, outputId, "他人", NOW)), "他利用者の出力記録を取消できてしまいます");
        require(throwsNotFound(() -> closing.close(otherUser, YEAR - 1, true, null, NOW)), "存在しない年度を締められてしまいます");
        require(throwsNotFound(() -> closing.view(otherUser, closedPage)), "他利用者の年度の履歴を参照できてしまいます");
        require(!closing.cancelOutput(userId, outputId, "", NOW).isEmpty(), "理由なしの出力記録取消を拒否していません");
        require(closing.cancelOutput(userId, outputId, "誤って共有したため", NOW).isEmpty(), "出力記録の取消に失敗しました");
        require(!closing.cancelOutput(userId, outputId, "再度", NOW).isEmpty(), "取消済み出力記録の再取消を拒否していません");
        ReportOutputRecord cancelled = context.getBean(ReportOutputRecordRepository.class).findById(outputId).orElseThrow();
        require(ReportOutputRecord.CANCELLED.equals(cancelled.getRecordStatus())
                && "誤って共有したため".equals(cancelled.getCancelReason()), "出力記録の取消内容が保存されていません");
    }

    private static boolean throwsNotFound(Runnable action) {
        try {
            action.run();
            return false;
        } catch (EntryNotFoundException ex) {
            return true;
        }
    }

    private static String status(AnnotationConfigApplicationContext context, TransactionTemplate tx, Long userId, int year) {
        return tx.execute(s -> ownedYear(context, userId, year).getStatus());
    }

    private static LocalDateTime closedAt(AnnotationConfigApplicationContext context, TransactionTemplate tx, Long userId,
            int year) {
        return tx.execute(s -> ownedYear(context, userId, year).getClosedAt());
    }

    private static FiscalYear ownedYear(AnnotationConfigApplicationContext context, Long userId, int year) {
        BusinessProfile profile = context.getBean(BusinessProfileRepository.class).findByAppUserId(userId).orElseThrow();
        return context.getBean(FiscalYearRepository.class).findByBusinessProfileIdOrderByTaxYearDesc(profile.getId())
                .stream().filter(y -> y.getTaxYear() == year).findFirst().orElseThrow();
    }

    private Long seedUser(AnnotationConfigApplicationContext context, TransactionTemplate tx, String loginId, int... years) {
        return tx.execute(status -> {
            AppUser user = new AppUser();
            user.setLoginId(loginId);
            user.setEmail(loginId);
            user.setPasswordHash("hash");
            user.setDisplayName("Harness");
            user.setStatus("active");
            user = context.getBean(AppUserRepository.class).save(user);
            UserSetting setting = new UserSetting();
            setting.setAppUser(user);
            setting.setHomeApportionmentRate(new BigDecimal("0.00"));
            context.getBean(UserSettingRepository.class).save(setting);
            BusinessProfile profile = new BusinessProfile();
            profile.setAppUser(user);
            profile.setBusinessName("Harness");
            profile.setFilingType("blue");
            profile.setConsumptionTaxStatus("SIMPLIFIED");
            profile.setSimplifiedBusinessType(5);
            profile = context.getBean(BusinessProfileRepository.class).save(profile);
            for (int year : years) {
                FiscalYear fy = new FiscalYear();
                fy.setBusinessProfile(profile);
                fy.setTaxYear(year);
                fy.setPeriodStart(LocalDate.of(year, 1, 1));
                fy.setPeriodEnd(LocalDate.of(year, 12, 31));
                fy.setStatus("open");
                context.getBean(FiscalYearRepository.class).save(fy);
            }
            return user.getId();
        });
    }

    /**
     * 指紋は様式の欄番号・欄名に依存せず、欄番号整合前に締めた年度の旧指紋（第1版）も締め時点との一致として扱う。
     */
    private void verifyFingerprintCompatibility(AnnotationConfigApplicationContext context,
            TaxReturnService taxReturnService, Long userId, TaxReturnPageData closedPage) {
        YearSnapshot snapshot = YearSnapshot.of(closedPage);
        require(!snapshot.fingerprint().equals(snapshot.legacyFingerprint()), "指紋の版が区別されていません");

        List<TaxReturnPageData.StatementRow> rows = closedPage.getStatementRows();
        int index = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (AccountMasterSeed.LINE_INCOME.equals(rows.get(i).getCode())) {
                index = i;
            }
        }
        TaxReturnPageData.StatementRow original = rows.get(index);
        rows.set(index, new TaxReturnPageData.StatementRow(99, "(99)", original.getCode(), "改訂後の欄名",
                original.getAmount(), original.getType(), original.getAccounts(), original.isTotal()));
        YearSnapshot relabeled = YearSnapshot.of(closedPage);
        rows.set(index, original);
        require(snapshot.fingerprint().equals(relabeled.fingerprint()), "様式の欄番号・欄名の変更で指紋が変わっています");

        JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));
        Long fiscalYearId = jdbc.queryForObject("SELECT id FROM fiscal_year WHERE tax_year = ? AND business_profile_id = "
                + "(SELECT id FROM business_profile WHERE app_user_id = ?)", Long.class, YEAR, userId);
        String update = "UPDATE fiscal_year_closing_event SET data_fingerprint = ? WHERE fiscal_year_id = ? AND event_type = 'CLOSE'";
        jdbc.update(update, snapshot.legacyFingerprint(), fiscalYearId);
        TaxReturnPageData legacy = taxReturnService.load(userId, YEAR, null);
        jdbc.update(update, snapshot.fingerprint(), fiscalYearId);
        require(legacy.getErrors().isEmpty() && legacy.getChecks().stream()
                        .anyMatch(c -> "check-closing-integrity".equals(c.getId()) && c.isOk()),
                "欄番号整合前の締め指紋を締め時点との一致として扱っていません: " + legacy.getErrors());
    }

    private void saveEntry(EntryService service, EntryKind kind, Long userId, EntryPageData page, String group,
            String label, String date, String amount, String methodId, String details) {
        EntryForm form = entryForm(page, group, label, date, amount, methodId, details);
        if (kind == EntryKind.INCOME) {
            form.setIncomeType(group);
            form.setReceiveMethodId(methodId);
        } else {
            form.setExpenseType(group);
            form.setPaymentMethodId(methodId);
        }
        EntryPrepareResult prepared = service.prepareSubmission(kind, userId, form);
        require(prepared.getValidatedEntry() != null, "登録の検証に失敗しました: " + details);
        service.save(prepared.getValidatedEntry());
    }

    private EntryForm entryForm(EntryPageData page, String group, String label, String date, String amount,
            String methodId, String details) {
        EntryForm form = new EntryForm();
        form.setCategory(page.getItems().stream()
                .filter(i -> group.equals(i.getGroup()) && label.equals(i.getLabel()))
                .map(EntryOption::getId).findFirst()
                .orElseThrow(() -> new IllegalStateException("品目がありません: " + label)));
        form.setDate(date);
        form.setAmount(amount);
        form.setDetails(details);
        return form;
    }

    private String optionId(List<EntryOption> options, String label) {
        return options.stream().filter(o -> label.equals(o.getLabel())).map(EntryOption::getId).findFirst()
                .orElseThrow(() -> new IllegalStateException("選択肢がありません: " + label));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "org.example.repository")
    @Import({AccountMappingServiceImpl.class, EntryServiceImpl.class, TaxReturnServiceImpl.class,
            ClosingJournalServiceImpl.class, BalanceSheetService.class, TaxReturnInputService.class,
            org.example.features.taxreturn.retention.RetentionServiceImpl.class, YearClosingService.class})
    static class HarnessConfig {
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(System.getProperty("year.closing.harness.url"));
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            emf.setPackagesToScan("org.example.entity");
            emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            java.util.Properties properties = new java.util.Properties();
            properties.put("hibernate.hbm2ddl.auto", "create");
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            emf.setJpaProperties(properties);
            return emf;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
