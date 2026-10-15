package org.example.integration;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.example.config.H2SchemaMigration;
import org.example.entity.AccountingTransaction;
import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseEntry;
import org.example.entity.FiscalYear;
import org.example.entity.UserSetting;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.form.EntryForm;
import org.example.features.entry.service.EntryPrepareResult;
import org.example.features.entry.service.EntrySaveResult;
import org.example.features.entry.service.EntryService;
import org.example.features.entry.service.ValidatedEntryCommand;
import org.example.features.entry.service.impl.EntryServiceImpl;
import org.example.repository.AccountingTransactionRepository;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseCategoryRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.ExpenseItemRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.IncomeCategoryRepository;
import org.example.repository.IncomeEntryRepository;
import org.example.repository.IncomeItemRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.PaymentMethodRepository;
import org.example.repository.ReceiveMethodRepository;
import org.example.repository.TaxDeductionRecordRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

public final class EntryBackendReviewHarness {
    private static final Long USER_ID = 1L;
    private static final Long FISCAL_YEAR_ID = 1L;

    public static void main(String[] args) throws Exception {
        new EntryBackendReviewHarness().run();
    }

    private void run() throws Exception {
        Path baseDir = Path.of("target", "review-harness");
        resetDirectory(baseDir);
        verifyLegacyMigrationAndVersionZeroEdit(baseDir.resolve("legacy"));
        verifyPartialMigrationIsReentrant(baseDir.resolve("partial"));
        verifyConcurrentClosureBlocksSave(baseDir.resolve("closure"));
        verifyConcurrentFinalizationBlocksSave(baseDir.resolve("finalize"));
        verifyExpandedCatalogs(baseDir.resolve("catalogs"));
        verifyRuleAlignedBusinessLogic(baseDir.resolve("rule-aligned"));
        System.out.println("Entry backend review harness passed");
    }

    private void verifyRuleAlignedBusinessLogic(Path dbDir) throws Exception {
        String url = createDbUrl(dbDir, "rule-aligned-db");
        try (HarnessContext harness = openHarness(url)) {
            seedCurrentAppData(harness);

            EntryPageData expensePage = harness.entryService.loadPage(EntryKind.EXPENSE, USER_ID, null);
            String expenseMethodId = expensePage.getMethods().stream()
                    .filter(option -> "現金".equals(option.getLabel()))
                    .map(option -> option.getId())
                    .findFirst()
                    .orElseThrow();
            String livingExpensesItemId = expensePage.getItems().stream()
                    .filter(item -> "other".equals(item.getGroup()))
                    .filter(item -> "生活費".equals(item.getLabel()))
                    .map(item -> item.getId())
                    .findFirst()
                    .orElseThrow();
            String erroneousPaymentItemId = expensePage.getItems().stream()
                    .filter(item -> "other".equals(item.getGroup()))
                    .filter(item -> "誤出金・返金処理".equals(item.getLabel()))
                    .map(item -> item.getId())
                    .findFirst()
                    .orElseThrow();

            EntryForm invalidApportionment = new EntryForm();
            invalidApportionment.setExpenseType("other");
            invalidApportionment.setCategory(livingExpensesItemId);
            invalidApportionment.setDate("2025-05-01");
            invalidApportionment.setAmount("12000");
            invalidApportionment.setPaymentMethodId(expenseMethodId);
            invalidApportionment.setHomeApportionment(true);
            invalidApportionment.setExcludeFromReturn(false);
            invalidApportionment.setDetails("私用の生活費");
            EntryPrepareResult rejected = harness.entryService.prepareSubmission(EntryKind.EXPENSE, USER_ID, invalidApportionment);
            require(rejected.getValidatedEntry() == null, "other支出で按分適用が通ってしまいました");
            require(rejected.getPageData().getFieldErrors().containsKey("homeApportionment"), "按分禁止エラーがありません");

            EntryForm transferExpense = new EntryForm();
            transferExpense.setExpenseType("other");
            transferExpense.setCategory(livingExpensesItemId);
            transferExpense.setDate("2025-05-02");
            transferExpense.setAmount("13000");
            transferExpense.setPaymentMethodId(expenseMethodId);
            transferExpense.setExcludeFromReturn(false);
            transferExpense.setDetails("私用の生活費");
            ValidatedEntryCommand transferExpenseCommand = requireCommand(harness.entryService
                    .prepareSubmission(EntryKind.EXPENSE, USER_ID, transferExpense));
            require(transferExpenseCommand.isExcludeFromReturn(), "other支出が申告対象外に強制されていません");
            harness.entryService.save(transferExpenseCommand);

            EntryForm adjustmentExpense = new EntryForm();
            adjustmentExpense.setExpenseType("other");
            adjustmentExpense.setCategory(erroneousPaymentItemId);
            adjustmentExpense.setDate("2025-05-03");
            adjustmentExpense.setAmount("14000");
            adjustmentExpense.setPaymentMethodId(expenseMethodId);
            adjustmentExpense.setExcludeFromReturn(false);
            adjustmentExpense.setDetails("誤出金の返金調整");
            ValidatedEntryCommand adjustmentExpenseCommand = requireCommand(harness.entryService
                    .prepareSubmission(EntryKind.EXPENSE, USER_ID, adjustmentExpense));
            harness.entryService.save(adjustmentExpenseCommand);

            EntryPageData incomePage = harness.entryService.loadPage(EntryKind.INCOME, USER_ID, null);
            String incomeMethodId = incomePage.getMethods().stream()
                    .filter(option -> "現金".equals(option.getLabel()))
                    .map(option -> option.getId())
                    .findFirst()
                    .orElseThrow();
            String ownerLoanItemId = incomePage.getItems().stream()
                    .filter(item -> "other".equals(item.getGroup()))
                    .filter(item -> "事業主借入金（事業主借）".equals(item.getLabel()))
                    .map(item -> item.getId())
                    .findFirst()
                    .orElseThrow();

            EntryForm transferIncome = new EntryForm();
            transferIncome.setIncomeType("other");
            transferIncome.setCategory(ownerLoanItemId);
            transferIncome.setDate("2025-05-04");
            transferIncome.setAmount("20000");
            transferIncome.setReceiveMethodId(incomeMethodId);
            transferIncome.setExcludeFromReturn(false);
            transferIncome.setDetails("個人資金の事業投入");
            ValidatedEntryCommand transferIncomeCommand = requireCommand(harness.entryService
                    .prepareSubmission(EntryKind.INCOME, USER_ID, transferIncome));
            require(transferIncomeCommand.isExcludeFromReturn(), "other収益が申告対象外に強制されていません");
            harness.entryService.save(transferIncomeCommand);

            harness.tx.executeWithoutResult(status -> {
                List<ExpenseEntry> expenses = harness.expenseEntries.findAllOwnedByFiscalYear(FISCAL_YEAR_ID, USER_ID);
                ExpenseEntry transfer = expenses.stream().filter(e -> "私用の生活費".equals(e.getDetails())).findFirst().orElseThrow();
                require(transfer.isExcludeFromReturn(), "生活費が申告対象外になっていません");
                require(new BigDecimal("0.00").compareTo(transfer.getDeductibleAmount()) == 0, "生活費の控除額が0ではありません");
                require("TRANSFER".equals(transfer.getAccountingTransaction().getTransactionType()), "生活費の取引種別がTRANSFERではありません");

                ExpenseEntry adjustment = expenses.stream().filter(e -> "誤出金の返金調整".equals(e.getDetails())).findFirst().orElseThrow();
                require("ADJUSTMENT".equals(adjustment.getAccountingTransaction().getTransactionType()), "誤出金調整の取引種別がADJUSTMENTではありません");

                List<org.example.entity.IncomeEntry> incomes = harness.incomeEntries.findAllOwnedByFiscalYear(FISCAL_YEAR_ID, USER_ID);
                org.example.entity.IncomeEntry transferIn = incomes.stream()
                        .filter(e -> "個人資金の事業投入".equals(e.getDetails()))
                        .findFirst()
                        .orElseThrow();
                require(transferIn.isExcludeFromReturn(), "事業主借入金が申告対象外になっていません");
                require("TRANSFER".equals(transferIn.getAccountingTransaction().getTransactionType()), "事業主借入金の取引種別がTRANSFERではありません");
            });
        }
    }

    private void verifyExpandedCatalogs(Path dbDir) throws Exception {
        String url = createDbUrl(dbDir, "catalogs-db");
        try (HarnessContext harness = openHarness(url)) {
            seedCurrentAppData(harness);
            EntryPageData incomePage = harness.entryService.loadPage(EntryKind.INCOME, USER_ID, null);
            EntryPageData expensePage = harness.entryService.loadPage(EntryKind.EXPENSE, USER_ID, null);

            assertCatalogItems(incomePage, "sales", List.of(
                    "SES・準委任契約報酬", "受託システム開発", "IT講師・技術研修", "技術記事・コンテンツ制作",
                    "自社サービス・SaaS利用料", "ソフトウェア・ライセンス販売", "広告・アフィリエイト収益", "その他売上"));
            assertCatalogItems(incomePage, "misc", List.of(
                    "補助金・助成金（事業関連）", "事業関連の保険金・補償金", "取引先からの謝礼金",
                    "事業関連のキャッシュバック", "事業関連の雑収益", "その他雑収入"));
            assertCatalogItems(incomePage, "other", List.of(
                    "事業主借入金（事業主借）", "借入金入金", "預り金の受入",
                    "立替金の回収", "誤入金・返金調整", "その他の資金移動"));
            assertCatalogItems(expensePage, "expense", List.of(
                    "クラウド利用料（AWS・Azure・GCP等）", "サーバー・ホスティング費用", "ドメイン取得・更新料",
                    "SSL証明書・セキュリティサービス", "IDE・開発ツール利用料", "業務ツール利用料",
                    "ソフトウェアライセンス購入費", "API利用料・従量課金", "データベース・ストレージ利用料",
                    "デバイス購入", "周辺機器・ケーブル類", "デスク・チェア・オフィス家具", "文房具・事務用品",
                    "機器修理・メンテナンス費", "通信料金", "電気料金", "ガス・水道料金", "事務所家賃",
                    "自宅兼事務所家賃", "レンタルスペース利用料", "郵便・宅配料金", "交通費", "出張宿泊費",
                    "駐車場・高速道路料金", "ガソリン代", "接待交際費", "贈答品費", "営業・会議費用",
                    "展示会・イベント参加費", "広告宣伝費", "教材費", "オンライン学習サービス",
                    "技術研修・セミナー受講費", "資格試験受験料", "資格更新料", "コミュニティ会費",
                    "税理士・会計士報酬", "弁護士・専門家報酬", "業務委託・外注費", "振込・決済手数料",
                    "事業用保険料", "租税公課（事業関連）", "借入金利息", "減価償却費",
                    "法定福利費（従業員分）", "従業員福利厚生費", "青色事業専従者給与",
                    "採用・求人広告費", "人材紹介手数料", "外部エンジニア委託費", "デザイン・制作外注費"));
            assertCatalogItems(expensePage, "purchase", List.of(
                    "商品仕入", "ソフトウェア仕入", "ハードウェア仕入", "販売用デジタルコンテンツ仕入",
                    "原材料・部品仕入", "その他仕入"));
            assertCatalogItems(expensePage, "other", List.of(
                    "生活費", "娯楽費", "所得税・住民税の納付", "国民健康保険・国民年金",
                    "借入金元本返済", "固定資産購入", "前払費用", "敷金・保証金", "立替金の支払い",
                    "預り金の支払い", "誤出金・返金処理", "その他資金移動"));
            require(java.util.stream.Stream.concat(incomePage.getItems().stream(), expensePage.getItems().stream())
                    .noneMatch(item -> item.getLabel().contains("：")), "勘定処理の補足が選択肢に含まれています");
        }
    }

    private void assertCatalogItems(EntryPageData page, String group, List<String> expectedLabels) {
        List<String> labels = page.getItems().stream()
                .filter(item -> group.equals(item.getGroup()))
                .map(item -> item.getLabel())
                .toList();
        require(labels.containsAll(expectedLabels), "catalog items missing for " + group);
    }

    private void verifyLegacyMigrationAndVersionZeroEdit(Path dbDir) throws Exception {
        String url = createDbUrl(dbDir, "legacy-db");
        createLegacySchema(url);
        seedLegacyRows(url);
        try (HarnessContext harness = openHarness(url)) {
            harness.tx.executeWithoutResult(status -> {
                ExpenseEntry legacyExpense = harness.expenseEntries.findOwnedById(1L, USER_ID).orElseThrow();
                require(Objects.equals(legacyExpense.getVersion(), 0L), "legacy expense version not backfilled");
                require(legacyExpense.getAccountingTransaction() == null, "legacy expense unexpectedly linked");
                require(new BigDecimal("3400.00").compareTo(legacyExpense.getAmount()) == 0, "legacy amount changed");
                require("legacy expense".equals(legacyExpense.getDetails()), "legacy details changed");
                require(harness.expenseEntries.sumAllAmountInRange(
                        FISCAL_YEAR_ID,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 12, 31)).compareTo(new BigDecimal("3400.00")) == 0, "legacy sum changed");
                require(Objects.equals(
                        harness.incomeEntries.findOwnedById(1L, USER_ID).orElseThrow().getVersion(),
                        0L), "legacy income version not backfilled");
            });

            EntryPageData editPage = harness.entryService.loadPage(EntryKind.EXPENSE, USER_ID, 1L);
            require(Objects.equals(editPage.getEntryForm().getVersion(), 0L), "edit form version should be zero");

            EntryForm editForm = editPage.getEntryForm();
            editForm.setDate("2025-02-11");
            editForm.setAmount("3600");
            editForm.setDetails("edited legacy expense");
            EntryPrepareResult preparedEdit = harness.entryService.prepareSubmission(EntryKind.EXPENSE, USER_ID, editForm);
            require(preparedEdit.getValidatedEntry() != null, "legacy edit command was not prepared");
            EntrySaveResult editResult = harness.entryService.save(preparedEdit.getValidatedEntry());
            require("支出を更新しました。".equals(editResult.getSuccessMessage()), "legacy edit did not save");

            EntryForm createForm = new EntryForm();
            createForm.setExpenseType("expense");
            createForm.setCategory("1");
            createForm.setDate("2025-03-01");
            createForm.setAmount("5000");
            createForm.setPaymentMethodId("1");
            createForm.setDetails("new expense");
            EntryPrepareResult preparedCreate = harness.entryService.prepareSubmission(EntryKind.EXPENSE, USER_ID, createForm);
            require(preparedCreate.getValidatedEntry() != null, "new create command was not prepared");
            harness.entryService.save(preparedCreate.getValidatedEntry());

            harness.tx.executeWithoutResult(status -> {
                ExpenseEntry editedExpense = harness.expenseEntries.findOwnedById(1L, USER_ID).orElseThrow();
                require(Objects.equals(editedExpense.getVersion(), 1L), "legacy edit version did not advance");
                require(editedExpense.getAccountingTransaction() != null, "edited legacy entry should now have transaction");
                require(harness.expenseEntries.count() == 2, "new expense was not created");
            });
        }
    }

    private void verifyPartialMigrationIsReentrant(Path dbDir) throws Exception {
        String url = createDbUrl(dbDir, "partial-db");
        createLegacySchema(url);
        seedLegacyRows(url);
        applyPartialMigrationState(url);
        try (HarnessContext harness = openHarness(url)) {
            harness.tx.executeWithoutResult(status -> {
                ExpenseEntry first = harness.expenseEntries.findOwnedById(1L, USER_ID).orElseThrow();
                ExpenseEntry second = harness.expenseEntries.findOwnedById(2L, USER_ID).orElseThrow();
                require(Objects.equals(first.getVersion(), 5L), "existing version should be preserved");
                require(Objects.equals(second.getVersion(), 0L), "null version should be backfilled");
                require(first.isHomeApportionmentApplied(), "existing home apportionment flag should be preserved");
                require(!second.isHomeApportionmentApplied(), "null home apportionment flag should default false");
                require(new BigDecimal("8800.00").compareTo(second.getDeductibleAmount()) == 0, "deductible amount changed");
                require("partial legacy expense".equals(second.getDetails()), "details changed during migration");
            });
        }
        try (HarnessContext harness = openHarness(url)) {
            harness.tx.executeWithoutResult(status -> {
                ExpenseEntry first = harness.expenseEntries.findOwnedById(1L, USER_ID).orElseThrow();
                ExpenseEntry second = harness.expenseEntries.findOwnedById(2L, USER_ID).orElseThrow();
                require(Objects.equals(first.getVersion(), 5L), "reentrant migration changed valid version");
                require(Objects.equals(second.getVersion(), 0L), "reentrant migration changed backfilled version");
                require(!second.isHomeApportionmentApplied(), "reentrant migration changed false default");
            });
        }
    }

    private void verifyConcurrentClosureBlocksSave(Path dbDir) throws Exception {
        String url = createDbUrl(dbDir, "closure-db");
        try (HarnessContext harness = openHarness(url)) {
            seedCurrentAppData(harness);
            EntryForm createForm = new EntryForm();
            createForm.setExpenseType("expense");
            createForm.setCategory("1");
            createForm.setDate("2025-04-01");
            createForm.setAmount("7000");
            createForm.setPaymentMethodId("1");
            createForm.setDetails("closure race");
            ValidatedEntryCommand command = requireCommand(harness.entryService.prepareSubmission(EntryKind.EXPENSE, USER_ID, createForm));

            CountDownLatch closureLocked = new CountDownLatch(1);
            CountDownLatch releaseClosure = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<Void> closer = executor.submit(() -> {
                    harness.tx.executeWithoutResult(status -> {
                        FiscalYear fiscalYear = harness.fiscalYears.findOwnedByIdForUpdate(FISCAL_YEAR_ID, USER_ID).orElseThrow();
                        closureLocked.countDown();
                        await(releaseClosure);
                        fiscalYear.setStatus("closed");
                        fiscalYear.setClosedAt(LocalDateTime.of(2025, 4, 30, 12, 0));
                    });
                    return null;
                });
                require(closureLocked.await(5, TimeUnit.SECONDS), "closure transaction did not lock fiscal year");
                Future<EntrySaveResult> saveFuture = executor.submit(() -> harness.entryService.save(command));
                Thread.sleep(200L);
                releaseClosure.countDown();
                closer.get(5, TimeUnit.SECONDS);
                expectConflict(saveFuture, "年度が締められているため更新できません。");
                harness.tx.executeWithoutResult(status -> require(harness.expenseEntries.count() == 0, "save should not create entry after closure"));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private void verifyConcurrentFinalizationBlocksSave(Path dbDir) throws Exception {
        String url = createDbUrl(dbDir, "finalize-db");
        try (HarnessContext harness = openHarness(url)) {
            seedCurrentAppData(harness);
            Long entryId = createDraftLinkedExpense(harness);

            EntryPageData editPage = harness.entryService.loadPage(EntryKind.EXPENSE, USER_ID, entryId);
            EntryForm editForm = editPage.getEntryForm();
            editForm.setAmount("9100");
            editForm.setDetails("updated after finalize");
            ValidatedEntryCommand command = requireCommand(harness.entryService.prepareSubmission(EntryKind.EXPENSE, USER_ID, editForm));

            CountDownLatch finalizeLocked = new CountDownLatch(1);
            CountDownLatch releaseFinalize = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<Void> finalizer = executor.submit(() -> {
                    harness.tx.executeWithoutResult(status -> {
                        harness.fiscalYears.findOwnedByIdForUpdate(FISCAL_YEAR_ID, USER_ID).orElseThrow();
                        AccountingTransaction transaction = harness.accountingTransactions.findOwnedByIdForUpdate(1L, USER_ID).orElseThrow();
                        finalizeLocked.countDown();
                        await(releaseFinalize);
                        transaction.setStatus("finalized");
                    });
                    return null;
                });
                require(finalizeLocked.await(5, TimeUnit.SECONDS), "finalizer did not lock transaction");
                Future<EntrySaveResult> saveFuture = executor.submit(() -> harness.entryService.save(command));
                Thread.sleep(200L);
                releaseFinalize.countDown();
                finalizer.get(5, TimeUnit.SECONDS);
                expectConflict(saveFuture, "確定済みデータは更新できません。");
                harness.tx.executeWithoutResult(status -> {
                    AccountingTransaction transaction = harness.accountingTransactions.findOwnedById(1L, USER_ID).orElseThrow();
                    ExpenseEntry entry = harness.expenseEntries.findOwnedById(entryId, USER_ID).orElseThrow();
                    require("finalized".equals(transaction.getStatus()), "finalized status was overwritten");
                    require(new BigDecimal("9000.00").compareTo(transaction.getTotalAmount()) == 0, "finalized amount was overwritten");
                    require("draft linked expense".equals(transaction.getSummary()), "finalized summary was overwritten");
                    require(Objects.equals(entry.getVersion(), 0L), "entry version should not advance after conflict");
                });
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private Long createDraftLinkedExpense(HarnessContext harness) {
        return harness.tx.execute(status -> {
            FiscalYear fiscalYear = harness.fiscalYears.findOwnedById(FISCAL_YEAR_ID, USER_ID).orElseThrow();
            AccountingTransaction transaction = new AccountingTransaction();
            transaction.setFiscalYear(fiscalYear);
            transaction.setTransactionDate(LocalDate.of(2025, 4, 2));
            transaction.setTransactionType("expense");
            transaction.setSummary("draft linked expense");
            transaction.setTotalAmount(new BigDecimal("9000.00"));
            transaction.setStatus("draft");
            transaction = harness.accountingTransactions.save(transaction);

            ExpenseEntry entry = new ExpenseEntry();
            entry.setFiscalYear(fiscalYear);
            entry.setExpenseItem(harness.expenseItems.findById(1L).orElseThrow());
            entry.setPaymentMethod(harness.paymentMethods.findById(1L).orElseThrow());
            entry.setTransactionDate(LocalDate.of(2025, 4, 2));
            entry.setAmount(new BigDecimal("9000.00"));
            entry.setExcludeFromReturn(false);
            entry.setHomeApportionmentApplied(false);
            entry.setHomeApportionmentRate(BigDecimal.ZERO.setScale(2));
            entry.setDeductibleAmount(new BigDecimal("9000.00"));
            entry.setDetails("draft linked expense");
            entry.setAccountingTransaction(transaction);
            return harness.expenseEntries.save(entry).getId();
        });
    }

    private void seedCurrentAppData(HarnessContext harness) {
        harness.tx.executeWithoutResult(status -> {
            AppUser user = new AppUser();
            user.setLoginId("demo@example.com");
            user.setEmail("demo@example.com");
            user.setPasswordHash("hash");
            user.setDisplayName("Demo");
            user.setStatus("active");
            user = harness.appUsers.save(user);

            UserSetting setting = new UserSetting();
            setting.setAppUser(user);
            setting.setHomeApportionmentRate(new BigDecimal("25.00"));
            harness.userSettings.save(setting);

            BusinessProfile profile = new BusinessProfile();
            profile.setAppUser(user);
            profile.setBusinessName("Demo Business");
            profile.setFilingType("blue");
            profile = harness.businessProfiles.save(profile);

            FiscalYear fiscalYear = new FiscalYear();
            fiscalYear.setBusinessProfile(profile);
            fiscalYear.setTaxYear(2025);
            fiscalYear.setPeriodStart(LocalDate.of(2025, 1, 1));
            fiscalYear.setPeriodEnd(LocalDate.of(2025, 12, 31));
            fiscalYear.setStatus("open");
            harness.fiscalYears.save(fiscalYear);
        });
        harness.entryService.loadPage(EntryKind.EXPENSE, USER_ID, null);
    }

    private ValidatedEntryCommand requireCommand(EntryPrepareResult result) {
        require(result.getValidatedEntry() != null, "expected validated command");
        return result.getValidatedEntry();
    }

    private HarnessContext openHarness(String url) {
        System.setProperty("entry.harness.datasource.url", url);
        System.setProperty("entry.harness.datasource.username", "sa");
        System.setProperty("entry.harness.datasource.password", "");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(HarnessConfig.class);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        return new HarnessContext(
                context,
                tx,
                context.getBean(EntryService.class),
                context.getBean(AppUserRepository.class),
                context.getBean(UserSettingRepository.class),
                context.getBean(BusinessProfileRepository.class),
                context.getBean(FiscalYearRepository.class),
                context.getBean(IncomeEntryRepository.class),
                context.getBean(ExpenseEntryRepository.class),
                context.getBean(AccountingTransactionRepository.class),
                context.getBean(ExpenseItemRepository.class),
                context.getBean(PaymentMethodRepository.class));
    }

    private void createLegacySchema(String url) throws SQLException {
        executeSql(url, List.of(
                "CREATE TABLE app_user (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, login_id VARCHAR(100) NOT NULL UNIQUE, email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, display_name VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'active', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
                "CREATE TABLE user_setting (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, app_user_id BIGINT NOT NULL UNIQUE, home_apportionment_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_user_setting_app_user FOREIGN KEY (app_user_id) REFERENCES app_user(id))",
                "CREATE TABLE business_profile (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, app_user_id BIGINT NOT NULL UNIQUE, business_name VARCHAR(200), filing_type VARCHAR(50) NOT NULL, bookkeeping_method VARCHAR(50), start_date DATE, end_date DATE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_business_profile_app_user FOREIGN KEY (app_user_id) REFERENCES app_user(id))",
                "CREATE TABLE fiscal_year (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, business_profile_id BIGINT NOT NULL, tax_year INTEGER NOT NULL, period_start DATE NOT NULL, period_end DATE NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'open', closed_at TIMESTAMP NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_fiscal_year_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile(id))",
                "CREATE TABLE receive_method (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, method_code VARCHAR(50) NOT NULL UNIQUE, method_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
                "CREATE TABLE payment_method (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, method_code VARCHAR(50) NOT NULL UNIQUE, method_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
                "CREATE TABLE income_category (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, business_profile_id BIGINT NOT NULL, category_code VARCHAR(50) NOT NULL, category_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_income_category_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile(id))",
                "CREATE TABLE income_item (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, income_category_id BIGINT NOT NULL, item_code VARCHAR(50) NOT NULL, item_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_income_item_income_category FOREIGN KEY (income_category_id) REFERENCES income_category(id))",
                "CREATE TABLE expense_category (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, business_profile_id BIGINT NOT NULL, category_code VARCHAR(50) NOT NULL, category_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_expense_category_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profile(id))",
                "CREATE TABLE expense_item (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, expense_category_id BIGINT NOT NULL, item_code VARCHAR(50) NOT NULL, item_name VARCHAR(100) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_expense_item_expense_category FOREIGN KEY (expense_category_id) REFERENCES expense_category(id))",
                "CREATE TABLE accounting_transaction (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, fiscal_year_id BIGINT NOT NULL, transaction_date DATE NOT NULL, transaction_type VARCHAR(30) NOT NULL, summary VARCHAR(500), total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00, status VARCHAR(20) NOT NULL DEFAULT 'draft', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_accounting_transaction_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year(id))",
                "CREATE TABLE income_entry (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, fiscal_year_id BIGINT NOT NULL, income_item_id BIGINT NOT NULL, receive_method_id BIGINT NOT NULL, transaction_date DATE NOT NULL, amount DECIMAL(15,2) NOT NULL DEFAULT 0.00, exclude_from_return BOOLEAN NOT NULL DEFAULT FALSE, details CLOB, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_income_entry_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year(id), CONSTRAINT fk_income_entry_income_item FOREIGN KEY (income_item_id) REFERENCES income_item(id), CONSTRAINT fk_income_entry_receive_method FOREIGN KEY (receive_method_id) REFERENCES receive_method(id))",
                "CREATE TABLE expense_entry (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, fiscal_year_id BIGINT NOT NULL, expense_item_id BIGINT NOT NULL, payment_method_id BIGINT NOT NULL, transaction_date DATE NOT NULL, amount DECIMAL(15,2) NOT NULL DEFAULT 0.00, exclude_from_return BOOLEAN NOT NULL DEFAULT FALSE, home_apportionment_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00, deductible_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00, details CLOB, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_expense_entry_fiscal_year FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_year(id), CONSTRAINT fk_expense_entry_expense_item FOREIGN KEY (expense_item_id) REFERENCES expense_item(id), CONSTRAINT fk_expense_entry_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method(id))"));
    }

    private void seedLegacyRows(String url) throws SQLException {
        executeSql(url, List.of(
                "INSERT INTO app_user (id, login_id, email, password_hash, display_name, status) VALUES (1, 'legacy@example.com', 'legacy@example.com', 'hash', 'Legacy User', 'active')",
                "INSERT INTO user_setting (id, app_user_id, home_apportionment_rate) VALUES (1, 1, 25.00)",
                "INSERT INTO business_profile (id, app_user_id, business_name, filing_type) VALUES (1, 1, 'Legacy Business', 'blue')",
                "INSERT INTO fiscal_year (id, business_profile_id, tax_year, period_start, period_end, status) VALUES (1, 1, 2025, DATE '2025-01-01', DATE '2025-12-31', 'open')",
                "INSERT INTO receive_method (id, method_code, method_name, is_active) VALUES (1, 'cash', '現金', TRUE)",
                "INSERT INTO payment_method (id, method_code, method_name, is_active) VALUES (1, 'cash', '現金', TRUE)",
                "INSERT INTO income_category (id, business_profile_id, category_code, category_name, is_active) VALUES (1, 1, 'sales', '売上', TRUE)",
                "INSERT INTO income_item (id, income_category_id, item_code, item_name, is_active) VALUES (1, 1, 'product', '商品売上', TRUE)",
                "INSERT INTO expense_category (id, business_profile_id, category_code, category_name, is_active) VALUES (1, 1, 'expense', '経費', TRUE)",
                "INSERT INTO expense_item (id, expense_category_id, item_code, item_name, is_active) VALUES (1, 1, 'supplies', '消耗品費', TRUE)",
                "INSERT INTO income_entry (id, fiscal_year_id, income_item_id, receive_method_id, transaction_date, amount, exclude_from_return, details) VALUES (1, 1, 1, 1, DATE '2025-02-10', 12000.00, FALSE, 'legacy income')",
                "INSERT INTO expense_entry (id, fiscal_year_id, expense_item_id, payment_method_id, transaction_date, amount, exclude_from_return, home_apportionment_rate, deductible_amount, details) VALUES (1, 1, 1, 1, DATE '2025-02-11', 3400.00, FALSE, 15.00, 3400.00, 'legacy expense')"));
    }

    private void applyPartialMigrationState(String url) throws SQLException {
        executeSql(url, List.of(
                "INSERT INTO expense_entry (id, fiscal_year_id, expense_item_id, payment_method_id, transaction_date, amount, exclude_from_return, home_apportionment_rate, deductible_amount, details) VALUES (2, 1, 1, 1, DATE '2025-02-12', 8800.00, FALSE, 0.00, 8800.00, 'partial legacy expense')",
                "ALTER TABLE income_entry ADD COLUMN version BIGINT",
                "ALTER TABLE expense_entry ADD COLUMN home_apportionment_applied BOOLEAN",
                "ALTER TABLE expense_entry ADD COLUMN version BIGINT",
                "UPDATE income_entry SET version = NULL",
                "UPDATE expense_entry SET home_apportionment_applied = TRUE, version = 5 WHERE id = 1",
                "UPDATE expense_entry SET home_apportionment_applied = NULL, version = NULL WHERE id = 2"));
    }

    private void executeSql(String url, List<String> statements) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            for (String sql : statements) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        }
    }

    private String createDbUrl(Path dbDir, String dbName) throws IOException {
        Files.createDirectories(dbDir);
        return "jdbc:h2:file:" + dbDir.resolve(dbName).toAbsolutePath()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000";
    }

    private void resetDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to clean harness directory", e);
                        }
                    });
        }
        Files.createDirectories(dir);
    }

    private void expectConflict(Future<EntrySaveResult> future, String expectedMessage) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            throw new IllegalStateException("Expected conflict was not thrown");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (!(cause instanceof EntryConflictException conflict) || !expectedMessage.equals(conflict.getMessage())) {
                throw new IllegalStateException("Unexpected exception", cause);
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for latch");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", ex);
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
    static class HarnessConfig {
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(System.getProperty("entry.harness.datasource.url"));
            dataSource.setUsername(System.getProperty("entry.harness.datasource.username", "sa"));
            dataSource.setPassword(System.getProperty("entry.harness.datasource.password", ""));
            H2SchemaMigration.migrateIfConfigured(dataSource, dataSource.getUrl());
            return dataSource;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
            entityManagerFactory.setDataSource(dataSource);
            entityManagerFactory.setPackagesToScan("org.example.entity");
            entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            java.util.Properties properties = new java.util.Properties();
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            properties.put("hibernate.show_sql", "false");
            properties.put("hibernate.format_sql", "false");
            entityManagerFactory.setJpaProperties(properties);
            return entityManagerFactory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }

        @Bean
        PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
            return new PersistenceExceptionTranslationPostProcessor();
        }

        @Bean
        EntryService entryService(
                BusinessProfileRepository businessProfileRepository,
                FiscalYearRepository fiscalYearRepository,
                IncomeEntryRepository incomeEntryRepository,
                ExpenseEntryRepository expenseEntryRepository,
                AccountingTransactionRepository accountingTransactionRepository,
                UserSettingRepository userSettingRepository,
                IncomeCategoryRepository incomeCategoryRepository,
                IncomeItemRepository incomeItemRepository,
                ExpenseCategoryRepository expenseCategoryRepository,
                ExpenseItemRepository expenseItemRepository,
                ReceiveMethodRepository receiveMethodRepository,
                PaymentMethodRepository paymentMethodRepository,
                JournalEntryRepository journalEntryRepository,
                TaxDeductionRecordRepository taxDeductionRecordRepository) {
            return new EntryServiceImpl(
                    businessProfileRepository,
                    fiscalYearRepository,
                    incomeEntryRepository,
                    expenseEntryRepository,
                    accountingTransactionRepository,
                    userSettingRepository,
                    incomeCategoryRepository,
                    incomeItemRepository,
                    expenseCategoryRepository,
                    expenseItemRepository,
                    receiveMethodRepository,
                    paymentMethodRepository,
                    journalEntryRepository,
                    taxDeductionRecordRepository);
        }
    }

    private record HarnessContext(
            AnnotationConfigApplicationContext context,
            TransactionTemplate tx,
            EntryService entryService,
            AppUserRepository appUsers,
            UserSettingRepository userSettings,
            BusinessProfileRepository businessProfiles,
            FiscalYearRepository fiscalYears,
            IncomeEntryRepository incomeEntries,
            ExpenseEntryRepository expenseEntries,
            AccountingTransactionRepository accountingTransactions,
            ExpenseItemRepository expenseItems,
            PaymentMethodRepository paymentMethods) implements AutoCloseable {
        @Override
        public void close() {
            context.close();
        }
    }
}
