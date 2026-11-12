package org.example.integration;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.AppUser;
import org.example.entity.BusinessProfile;
import org.example.entity.FiscalYear;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.entity.UserSetting;
import org.example.features.accounting.service.AccountMappingService;
import org.example.features.accounting.service.impl.AccountMappingServiceImpl;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryOption;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.form.EntryForm;
import org.example.features.entry.service.EntryPrepareResult;
import org.example.features.entry.service.EntryService;
import org.example.features.entry.service.impl.EntryServiceImpl;
import org.example.features.taxreturn.balance.BalanceSheetService;
import org.example.features.taxreturn.dto.TaxReturnPageData;
import org.example.features.taxreturn.dto.TaxReturnPageData.StatementRow;
import org.example.features.taxreturn.form.SettlementForm;
import org.example.features.taxreturn.form.TaxReturnForms;
import org.example.features.taxreturn.input.TaxReturnInputService;
import org.example.features.taxreturn.retention.RetentionPageData;
import org.example.features.taxreturn.retention.RetentionRecordForm;
import org.example.features.taxreturn.retention.RetentionService;
import org.example.features.taxreturn.consumption.ConsumptionTaxCalculator;
import org.example.features.taxreturn.export.EtaxAssistCsv;
import org.example.features.taxreturn.export.BooksCsv;
import org.example.features.taxreturn.export.FixedAssetLedgerCsv;
import org.example.features.taxreturn.incometax.IncomeTaxCalculator;
import org.example.features.taxreturn.incometax.BlueReturnEdition;
import org.example.features.taxreturn.incometax.TaxYearRules;
import org.example.features.taxreturn.service.ClosingJournalService;
import org.example.features.taxreturn.service.TaxReturnService;
import org.example.features.taxreturn.service.impl.ClosingJournalServiceImpl;
import org.example.features.taxreturn.service.impl.TaxReturnServiceImpl;
import org.example.repository.AppUserRepository;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.JournalEntryRepository;
import org.example.repository.LedgerAccountRepository;
import org.example.repository.StatementLineRepository;
import org.example.entity.StatementLine;
import org.example.features.accounting.AccountMasterSeed;
import org.example.repository.UiItemAccountMappingRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 勘定科目マスタ・品目→科目対応・科目→決算書欄対応の結合確認。
 * 実行: mvn test-compile 後に main を実行（H2ファイルDBを target/account-mapping-harness に作成）。
 */
public final class AccountMappingHarness {
    private static final int YEAR = 2025;

    public static void main(String[] args) throws Exception {
        Path dir = Path.of("target", "account-mapping-harness");
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(dir);
        System.setProperty("account.harness.url",
                "jdbc:h2:file:" + dir.toAbsolutePath().resolve("db") + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(HarnessConfig.class)) {
            new AccountMappingHarness().run(context);
        }
        System.out.println("Account mapping harness passed");
    }

    private void run(AnnotationConfigApplicationContext context) {
        verifyBlueReturnEditions();
        AccountMappingService mappingService = context.getBean(AccountMappingService.class);
        EntryService entryService = context.getBean(EntryService.class);
        TaxReturnService taxReturnService = context.getBean(TaxReturnService.class);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        mappingService.ensureMasterData();
        long accounts = context.getBean(LedgerAccountRepository.class).count();
        long lines = context.getBean(StatementLineRepository.class).count();
        long itemMappings = context.getBean(UiItemAccountMappingRepository.class).count();
        mappingService.ensureMasterData();
        require(accounts > 0 && lines == 45 && itemMappings > 0, "マスタ初期値が投入されていません: lines=" + lines);
        verifyStatementLineSync(context, mappingService, tx);
        require(accounts == context.getBean(LedgerAccountRepository.class).count()
                && itemMappings == context.getBean(UiItemAccountMappingRepository.class).count(), "マスタ投入が冪等ではありません");

        Long userId = seedUser(context, tx);
        EntryPageData expensePage = entryService.loadPage(EntryKind.EXPENSE, userId, null);
        EntryPageData incomePage = entryService.loadPage(EntryKind.INCOME, userId, null);
        String bank = optionId(expensePage.getMethods(), "銀行振込");
        String incomeBank = optionId(incomePage.getMethods(), "銀行振込");

        saveIncomeWithholding(entryService, userId, incomePage, "sales", "SES・準委任契約報酬", "2025-03-31", "1000000", incomeBank,
                "3月分SES", "102100", "株式会社テスト", "東京都千代田区1-1");
        saveIncome(entryService, userId, incomePage, "misc", "受取利息", "2025-06-30", "500", incomeBank, "利息");
        saveExpense(entryService, userId, expensePage, "expense", "クラウド利用料（AWS・Azure・GCP等）", "2025-04-10", "5000", bank, "AWS");
        saveExpense(entryService, userId, expensePage, "expense", "ガソリン代", "2025-04-11", "8000", bank, "ガソリン");
        saveExpense(entryService, userId, expensePage, "expense", "振込・決済手数料", "2025-04-12", "440", bank, "振込手数料");
        saveExpense(entryService, userId, expensePage, "expense", "デバイス購入", "2025-04-13", "60000", bank, "モニター");
        saveExpense(entryService, userId, expensePage, "expense", "青色事業専従者給与", "2025-04-25", "80000", bank, "専従者給与");
        saveExpense(entryService, userId, expensePage, "purchase", "商品仕入", "2025-05-01", "30000", bank, "仕入");
        saveExpense(entryService, userId, expensePage, "other", "借入金元本返済", "2025-05-02", "10000", bank, "元本返済");
        saveExpense(entryService, userId, expensePage, "other", "生活費", "2025-05-03", "20000", bank, "生活費");

        Map<String, List<JournalLine>> journal = tx.execute(status -> {
            FiscalYear fy = context.getBean(FiscalYearRepository.class).findAll().get(0);
            return context.getBean(JournalEntryRepository.class).findAllWithLinesByFiscalYearId(fy.getId()).stream()
                    .distinct()
                    .collect(Collectors.toMap(JournalEntry::getSummary, e -> List.copyOf(e.getLines()), (a, b) -> a));
        });
        requireLine(journal, "AWS", "D", "COMMUNICATION", "通信費");
        requireLine(journal, "AWS", "C", "BANK_DEPOSIT", "普通預金");
        requireLine(journal, "ガソリン", "D", "VEHICLE", "車両費");
        requireLine(journal, "3月分SES", "C", "SALES", "売上高");
        requireLine(journal, "3月分SES", "D", "OWNER_DRAW", "事業主貸");
        requireLine(journal, "利息", "C", "MISC_INCOME", "雑収入");
        requireLine(journal, "元本返済", "D", "LOANS", "借入金");
        requireLine(journal, "生活費", "D", "OWNER_DRAW", "事業主貸");

        SettlementForm settlement = new SettlementForm();
        settlement.setOpeningInventory("10000");
        settlement.setClosingInventory("15000");
        settlement.setOpeningBalance("0");
        require(taxReturnService.saveSettlement(userId, YEAR, settlement).isEmpty(), "決算整理の保存に失敗しました");

        TaxReturnInputService inputService = context.getBean(TaxReturnInputService.class);
        TaxReturnForms.FixedAssetForm asset = new TaxReturnForms.FixedAssetForm();
        asset.setAssetName("ノートPC");
        asset.setAccountCode("TOOLS_EQUIPMENT");
        asset.setAcquisitionDate("2025-10-01");
        asset.setAcquisitionCost("192000");
        asset.setUsefulLife("4");
        asset.setDepreciationMethod("STRAIGHT_LINE");
        asset.setBusinessRatio("100");
        asset.setAcquisitionFunding("OWNER_FUNDS");
        require(inputService.addFixedAsset(userId, YEAR, asset).isEmpty(), "固定資産の登録に失敗しました");
        require(taxReturnService.exportFixedAssetLedger(userId, YEAR).csv().isEmpty(),
                "償却要件未確認の台帳が出力されました");
        confirmDepreciationRules(inputService, userId, YEAR);
        verifyFixedAssetEvents(context, inputService, taxReturnService, userId);

        TaxReturnPageData page = taxReturnService.load(userId, YEAR, null);
        Map<String, StatementRow> rows = page.getStatementRows().stream()
                .collect(Collectors.toMap(StatementRow::getCode, Function.identity()));
        requireAmount(rows, "SALES", "1000500");
        requireAmount(rows, "OPENING_INVENTORY", "10000");
        requireAmount(rows, "PURCHASES", "30000");
        requireAmount(rows, "COGS_SUBTOTAL", "40000");
        requireAmount(rows, "CLOSING_INVENTORY", "15000");
        requireAmount(rows, "COGS", "25000");
        requireAmount(rows, "GROSS_PROFIT", "975500");
        requireAmount(rows, "COMMUNICATION", "5000");
        requireAmount(rows, "SUPPLIES", "60000");
        requireAmount(rows, "DEPRECIATION", "12000");
        require("支払手数料".equals(rows.get("FREE_1").getLabel()), "任意欄1が支払手数料ではありません: " + rows.get("FREE_1").getLabel());
        requireAmount(rows, "FREE_1", "440");
        require("車両費".equals(rows.get("FREE_2").getLabel()), "任意欄2が車両費ではありません: " + rows.get("FREE_2").getLabel());
        requireAmount(rows, "FREE_2", "8000");
        requireAmount(rows, "EXPENSE_TOTAL", "85440");
        requireAmount(rows, "NET_BEFORE_RESERVES", "890060");
        requireAmount(rows, "DEDICATED_SALARY", "80000");
        requireAmount(rows, "PROVISION_TOTAL", "80000");
        requireAmount(rows, "INCOME_BEFORE_BLUE", "810060");
        requireAmount(rows, "BLUE_DEDUCTION", "650000");
        requireAmount(rows, "INCOME", "160060");
        require(page.getIncome().compareTo(new BigDecimal("810060")) == 0, "損益計算書の所得と決算書が一致しません: " + page.getIncome());
        require(page.getErrors().stream().noneMatch(e -> e.contains("決算書")), "決算書と損益計算書の不一致エラー: " + page.getErrors());
        require(page.getWarnings().stream().anyMatch(w -> w.contains("デバイス購入")), "要確認品目の警告がありません: " + page.getWarnings());
        require(page.getPlRows().stream().anyMatch(r -> "通信費".equals(r.getLabel())), "損益計算書が勘定科目別になっていません");
        require(page.getPlRows().stream().noneMatch(r -> r.getLabel().startsWith("クラウド利用料")), "損益計算書に品目名が残っています");
        require(page.getChecks().stream().anyMatch(c -> "check-account-mapping".equals(c.getId()) && c.isOk()), "科目対応チェックが完了になりません");
        require(page.getChecks().stream().anyMatch(c ->
                "check-transaction-journal-reconciliation".equals(c.getId()) && c.isOk()), "取引と仕訳の照合が完了になりません");
        verifyTransactionJournalMismatch(context, taxReturnService, userId, tx);
        verifyLedgersAndTax(context, taxReturnService, inputService, userId, tx);
        verifyFixedAssetLedger(context, taxReturnService, userId, tx);
        verifyDepreciationRules(context, inputService, taxReturnService, tx);
        verifyExtendedDepreciation(context, inputService, taxReturnService, tx);

        tx.executeWithoutResult(status -> {
            JournalEntry entry = context.getBean(JournalEntryRepository.class).findAll().get(0);
            JournalLine legacy = new JournalLine();
            legacy.setLineNo(99);
            legacy.setDebitCredit("D");
            legacy.setAccountName("クラウド利用料（AWS・Azure・GCP等）");
            legacy.setAmount(BigDecimal.ONE);
            legacy.setJournalEntry(entry);
            entry.getLines().add(legacy);
        });
        require(mappingService.backfillJournalAccountCodes() >= 1, "旧仕訳の科目コード補完が行われませんでした");
        tx.executeWithoutResult(status -> {
            JournalLine legacy = context.getBean(JournalEntryRepository.class).findAll().stream()
                    .flatMap(e -> e.getLines().stream()).filter(l -> l.getLineNo() == 99).findFirst().orElseThrow();
            require("COMMUNICATION".equals(legacy.getAccountCode()) && "通信費".equals(legacy.getAccountName()),
                    "旧仕訳が通信費へ読み替えられていません: " + legacy.getAccountName());
        });
        verifyDeductionSelection(taxReturnService, userId);
        verifyRetentionRecords(context, userId, tx);
        verifyHouseholdAndSalaryDetails(context, userId, taxReturnService);
    }

    private void confirmDepreciationRules(TaxReturnInputService inputService, Long userId, int year) {
        TaxReturnForms.DepreciationRulesForm form = new TaxReturnForms.DepreciationRulesForm();
        form.setMethodBasis("STATUTORY");
        form.setScopeConfirmed(true);
        form.setSpecialConfirmed(true);
        form.setBusinessMonths("12");
        form.setEvidence("合成テスト：資産区分・適用要件を確認");
        require(inputService.saveDepreciationRules(userId, year, form).isEmpty(), "償却要件の利用者確認を保存できません");
    }

    private void verifyDepreciationRules(AnnotationConfigApplicationContext context, TaxReturnInputService input,
            TaxReturnService service, TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            Long userId = seedUser(context, tx, "depreciation-rules@example.com");
            BusinessProfile profile = context.getBean(BusinessProfileRepository.class).findByAppUserId(userId).orElseThrow();
            FiscalYear next = new FiscalYear();
            next.setBusinessProfile(profile);
            next.setTaxYear(2026);
            next.setPeriodStart(LocalDate.of(2026, 1, 1));
            next.setPeriodEnd(LocalDate.of(2026, 12, 31));
            next.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(next);

            TaxReturnForms.FixedAssetForm form = new TaxReturnForms.FixedAssetForm();
            form.setAssetName("改正境界の合成資産");
            form.setAccountCode("TOOLS_EQUIPMENT");
            form.setAcquisitionDate("2026-03-31");
            form.setAcquisitionCost("300000");
            form.setDepreciationMethod("SMALL_AMOUNT_SPECIAL");
            form.setBusinessRatio("100");
            form.setAcquisitionFunding("OWNER_FUNDS");
            require(!input.addFixedAsset(userId, 2026, form).isEmpty(), "改正前の30万円を少額特例に登録できました");
            form.setAcquisitionDate("2026-04-01");
            form.setAcquisitionCost("400000");
            require(!input.addFixedAsset(userId, 2026, form).isEmpty(), "改正後の40万円を少額特例に登録できました");
            form.setAcquisitionCost("99999");
            require(!input.addFixedAsset(userId, 2026, form).isEmpty(), "10万円未満を少額特例に登録できました");
            form.setAcquisitionCost("399999");
            require(input.addFixedAsset(userId, 2026, form).isEmpty(), "改正後の399999円を登録できません");
            require(service.exportBooks(userId, 2026).csv().isEmpty(), "特例要件未確認の帳簿が出力されました");
            confirmDepreciationRules(input, userId, 2026);
            require(service.exportBooks(userId, 2026).page().getErrors().isEmpty(),
                    "確認後の少額特例にエラーがあります：" + service.load(userId, 2026, null).getErrors());

            TaxReturnForms.DepreciationRulesForm confirmation = new TaxReturnForms.DepreciationRulesForm();
            confirmation.setMethodBasis("NOTIFIED");
            confirmation.setScopeConfirmed(true);
            confirmation.setEvidence("合成テスト：期限内届出・年分要件確認");
            confirmation.setBusinessMonths("1");
            require(!input.saveDepreciationRules(userId, 2026, confirmation).isEmpty(), "開業年の月数按分上限を超過できました");
            confirmation.setBusinessMonths("13");
            require(!input.saveDepreciationRules(userId, 2026, confirmation).isEmpty(), "不正な事業月数を保存できました");
            confirmation.setBusinessMonths("12");
            require(input.saveDepreciationRules(userId, 2026, confirmation).isEmpty(), "届出根拠を保存できません");
            require(service.exportBooks(userId, 2026).csv().isEmpty(), "少額特例確認なしで出力できました");
            confirmation.setSpecialConfirmed(true);
            require(input.saveDepreciationRules(userId, 2026, confirmation).isEmpty(), "特例確認を保存できません");
            confirmation.setScopeConfirmed(false);
            require(input.saveDepreciationRules(userId, 2026, confirmation).isEmpty()
                    && service.exportBooks(userId, 2026).csv().isEmpty(), "償却要件確認を取り消せません");
            form.setAcquisitionCost("390000");
            for (int i = 0; i < 6; i++) {
                require(input.addFixedAsset(userId, 2026, form).isEmpty(), "年300万円未満の資産を登録できません");
            }
            require(!input.addFixedAsset(userId, 2026, form).isEmpty(), "少額特例の年300万円超過を登録できました");
            form.setDepreciationMethod("STRAIGHT_LINE");
            form.setUsefulLife("5");
            form.setAcquisitionDate("2007-03-31");
            require(!input.addFixedAsset(userId, 2026, form).isEmpty(), "旧償却方式の取得日を新定額法で登録できました");
            form.setAcquisitionDate("2026-04-01");
            form.setDepreciationMethod("DECLINING_BALANCE");
            require(!input.addFixedAsset(userId, 2026, form).isEmpty(), "未対応の定率法を登録できました");

            org.example.entity.FixedAsset straight = ledgerAsset(context, profile, "中途売却", 2024, "240000", "STRAIGHT_LINE");
            TaxReturnForms.FixedAssetEventForm eventForm = new TaxReturnForms.FixedAssetEventForm();
            eventForm.setAssetId(straight.getId());
            eventForm.setEventType("SALE");
            eventForm.setEffectiveDate("2025-06-15");
            eventForm.setProceeds("30000");
            require(input.addFixedAssetEvent(userId, 2025, eventForm).isEmpty(), "年度途中の売却を登録できません");
            org.example.entity.FixedAssetEvent sale = context.getBean(org.example.repository.FixedAssetEventRepository.class)
                    .findByFixedAssetIdOrderByEffectiveDateAscIdAsc(straight.getId()).get(0);
            List<org.example.entity.FixedAsset> straightAssets = List.of(straight);
            List<org.example.entity.FixedAssetEvent> saleEvents = List.of(sale);
            var partial = org.example.features.taxreturn.asset.DepreciationCalculator.forYear(straight, 2025,
                    new BigDecimal("50"), saleEvents, straightAssets);
            require(partial.months() == 6 && partial.depreciation().intValueExact() == 24000
                    && partial.businessPortion().intValueExact() == 12000 && partial.closing().intValueExact() == 168000,
                    "中途売却の月割り・事業割合・簿価が誤っています");
            require(org.example.features.taxreturn.asset.DepreciationCalculator.forYear(straight, 2026,
                    BigDecimal.valueOf(100), saleEvents, straightAssets).depreciation().signum() == 0,
                    "売却後の翌年にも定額償却されます");
            confirmDepreciationRules(input, userId, 2025);
            TaxReturnPageData sold = service.load(userId, 2025, null);
            require(sold.getExtras().getTax() == null && sold.getExtras().getTaxUnsupportedReason().contains("譲渡所得"),
                    "譲渡所得が未計算なのに所得税額が表示されています");
            require(sold.getMonths().get(5).getExpense().intValueExact() == 24000
                    && sold.getMonths().get(11).getExpense().signum() == 0,
                    "中途処分までの償却費が処分月に計上されていません");
            JournalEntry saleJournal = context.getBean(JournalEntryRepository.class)
                    .findBySourceTypeAndSourceId("FIXED_ASSET_EVENT", sale.getId()).orElseThrow();
            require(saleJournal.getEntryDate().equals(LocalDate.of(2025, 6, 15))
                    && saleJournal.getLines().stream()
                            .filter(l -> "C".equals(l.getDebitCredit()) && "TOOLS_EQUIPMENT".equals(l.getAccountCode()))
                            .map(JournalLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).intValueExact() == 168000
                    && saleJournal.getLines().stream().noneMatch(l -> "減価償却費".equals(l.getAccountName())),
                    "年度分の償却仕訳と処分仕訳で償却が二重計上されています");
            String saleCsv = service.exportFixedAssetLedger(userId, 2025).csv();
            require(assetCsvRow(saleCsv, straight.getId()).contains("\"6\",\"192000\",\"24000\""),
                    "CSVの中途処分月数・償却額が共通計算と一致しません");
            require(input.deleteFixedAssetEvent(userId, 2025, sale.getId()).isEmpty(), "中途売却を訂正できません");
            require(service.load(userId, 2025, null).getExtras().getAssets().get(0).getDepreciation().intValueExact() == 48000,
                    "中途売却の削除後に年額償却へ戻りません");
            require(input.addFixedAssetEvent(userId, 2025, eventForm).isEmpty(), "中途売却を再登録できません");

            org.example.entity.FixedAsset lump1 = ledgerAsset(context, profile, "一括1", 2024, "100001", "LUMP_SUM_3Y");
            org.example.entity.FixedAsset lump2 = ledgerAsset(context, profile, "一括2", 2024, "100001", "LUMP_SUM_3Y");
            lump1.setAccountCode("LUMP_SUM_ASSETS");
            lump2.setAccountCode("LUMP_SUM_ASSETS");
            org.example.entity.FixedAssetEvent discarded = ledgerEvent(context, lump1, "DISPOSAL",
                    LocalDate.of(2025, 6, 1), null, null);
            List<org.example.entity.FixedAsset> pool = List.of(lump1, lump2);
            List<org.example.entity.FixedAssetEvent> history = List.of(discarded);
            BigDecimal firstTwoYears = BigDecimal.ZERO;
            BigDecimal finalYear = BigDecimal.ZERO;
            for (org.example.entity.FixedAsset member : pool) {
                var middle = org.example.features.taxreturn.asset.DepreciationCalculator.forYear(member, 2025,
                        BigDecimal.valueOf(100), history, pool);
                var last = org.example.features.taxreturn.asset.DepreciationCalculator.forYear(member, 2026,
                        BigDecimal.valueOf(100), history, pool);
                firstTwoYears = firstTwoYears.add(middle.depreciation());
                finalYear = finalYear.add(last.depreciation());
                require(last.closing().signum() == 0, "一括償却の3年目に端数残高があります");
            }
            require(firstTwoYears.intValueExact() == 66668 && finalYear.intValueExact() == 66666,
                    "一括償却の供用年別合算・端数配分が誤っています");
            eventForm.setAssetId(lump2.getId());
            eventForm.setEventType("BUSINESS_RATIO_CHANGE");
            eventForm.setEffectiveDate("2025-01-01");
            eventForm.setBusinessRatio("50");
            require(input.addFixedAssetEvent(userId, 2025, eventForm).isEmpty(), "一括資産の割合変更を登録できません");
            eventForm.setEventType("SALE");
            eventForm.setEffectiveDate("2025-06-02");
            require(input.addFixedAssetEvent(userId, 2025, eventForm).isEmpty(), "一括資産の中途売却を登録できません");
            context.getBean(ClosingJournalService.class).rebuildOpenYears(profile.getId());
            confirmDepreciationRules(input, userId, 2025);
            TaxReturnService.BooksExport books = service.exportBooks(userId, 2025);
            require(books.page().getErrors().isEmpty() && !books.csv().isEmpty(),
                    "中途処分・一括償却の仕訳と帳票が一致しません：" + books.page().getErrors());
            require(books.page().getExtras().getMiscIncome().intValueExact() == 15000,
                    "一括償却資産の売却収入の事業分が決算書へ反映されません");
            String lumpCsv = service.exportFixedAssetLedger(userId, 2025).csv();
            require(assetCsvRow(lumpCsv, lump1.getId()).contains("\"33333\",\"33333\",\"除却済み\""),
                    "一括資産を除却時に即時償却しているか期末残高が失われています");
            FiscalYear unsupported = new FiscalYear();
            unsupported.setBusinessProfile(profile);
            unsupported.setTaxYear(2027);
            unsupported.setPeriodStart(LocalDate.of(2027, 1, 1));
            unsupported.setPeriodEnd(LocalDate.of(2027, 12, 31));
            unsupported.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(unsupported);
            require(!input.saveDepreciationRules(userId, 2027, confirmation).isEmpty(),
                    "未対応の年分を確認済みにできました");
            boolean ownerRejected = false;
            try {
                input.saveDepreciationRules(999999L, 2025, confirmation);
            } catch (org.example.features.entry.EntryNotFoundException expected) {
                ownerRejected = true;
            }
            require(ownerRejected, "償却確認の更新で所有者チェックがありません");
            next.setStatus("closed");
            require(!input.saveDepreciationRules(userId, 2026, confirmation).isEmpty(),
                    "確定年度の償却要件を更新できました");
            org.example.entity.FixedAsset boundary = new org.example.entity.FixedAsset();
            boundary.setAssetName("上限検証の合成資産");
            boundary.setAcquisitionDate(LocalDate.of(2026, 4, 1));
            boundary.setServiceStartDate(LocalDate.of(2026, 4, 1));
            boundary.setAcquisitionCost(new BigDecimal("300000"));
            boundary.setDepreciationMethod("SMALL_AMOUNT_SPECIAL");
            org.example.entity.TaxReturnInput policy = new org.example.entity.TaxReturnInput();
            policy.setDepreciationRuleVersion(org.example.features.taxreturn.asset.DepreciationRules.version(2026));
            policy.setDepreciationMethodBasis("STATUTORY");
            policy.setDepreciationSpecialConfirmed(true);
            require(org.example.features.taxreturn.asset.DepreciationRules.annualErrors(2026,
                    java.util.Collections.nCopies(10, boundary), policy).isEmpty(), "年300万円ちょうどを拒否しています");
            require(!org.example.features.taxreturn.asset.DepreciationRules.annualErrors(2026,
                    java.util.Collections.nCopies(11, boundary), policy).isEmpty(), "年300万円超過を見逃しています");
            status.setRollbackOnly();
        });
    }

    private void verifyExtendedDepreciation(AnnotationConfigApplicationContext context, TaxReturnInputService input,
            TaxReturnService service, TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            Long userId = seedUser(context, tx, "extended-depreciation@example.com");
            BusinessProfile profile = context.getBean(BusinessProfileRepository.class).findByAppUserId(userId).orElseThrow();
            FiscalYear prior = new FiscalYear();
            prior.setBusinessProfile(profile);
            prior.setTaxYear(2024);
            prior.setPeriodStart(LocalDate.of(2024, 1, 1));
            prior.setPeriodEnd(LocalDate.of(2024, 12, 31));
            prior.setStatus("closed");
            context.getBean(FiscalYearRepository.class).save(prior);
            FiscalYear next = new FiscalYear();
            next.setBusinessProfile(profile);
            next.setTaxYear(2026);
            next.setPeriodStart(LocalDate.of(2026, 1, 1));
            next.setPeriodEnd(LocalDate.of(2026, 12, 31));
            next.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(next);

            TaxReturnForms.FixedAssetForm form = new TaxReturnForms.FixedAssetForm();
            form.setAssetName("中古転用の合成資産");
            form.setAccountCode("TOOLS_EQUIPMENT");
            form.setAcquisitionDate("2016-01-10");
            form.setServiceStartDate("2025-04-01");
            form.setAcquisitionCost("10000000");
            form.setUsefulLife("22");
            form.setDepreciationMethod("STRAIGHT_LINE");
            form.setBusinessRatio("100");
            form.setAcquisitionFunding("OWNER_FUNDS");
            form.setLifeBasis("USED_SIMPLIFIED");
            form.setUsedElapsedMonths("79");
            form.setPrivateUseConversion(true);
            require(!input.addFixedAsset(userId, YEAR, form).isEmpty(), "中古要件の未確認を登録できました");
            form.setUsedRequirementsConfirmed(true);
            form.setPrivateUseConversion(false);
            require(!input.addFixedAsset(userId, YEAR, form).isEmpty(), "確定年度に遡る通常取得を登録できました");
            form.setPrivateUseConversion(true);
            require(input.addFixedAsset(userId, YEAR, form).isEmpty(), "中古転用を登録できません");
            var repository = context.getBean(org.example.repository.FixedAssetRepository.class);
            var converted = repository.findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(profile.getId()).get(0);
            require(converted.getUsefulLife() == 16 && converted.getStatutoryUsefulLife() == 22
                    && converted.getUsedElapsedMonths() == 79 && converted.isUsedRequirementsConfirmed()
                    && converted.isPrivateUseConversion(), "中古・転用の入力根拠が保存されません");
            var acquisition = context.getBean(JournalEntryRepository.class)
                    .findBySourceTypeAndSourceId("FIXED_ASSET_ACQUISITION", converted.getId()).orElseThrow();
            require(acquisition.getEntryDate().equals(LocalDate.of(2025, 4, 1))
                    && acquisition.getLines().stream().allMatch(l -> l.getAmount().intValueExact() == 7489000),
                    "転用日・転用時簿価の取得仕訳ではありません");
            require(context.getBean(JournalEntryRepository.class).findAllWithLinesByFiscalYearId(prior.getId()).isEmpty(),
                    "転用前の確定年度に仕訳が生成されました");
            confirmDepreciationRules(input, userId, YEAR);
            String csv = service.exportFixedAssetLedger(userId, YEAR).csv();
            String convertedRow = assetCsvRow(csv, converted.getId());
            require(convertedRow.contains("\"9\",\"0\",\"472500\",\"100\",\"472500\",\"472500\",\"\",\"7016500\"")
                    && convertedRow.contains("\"USED_SIMPLIFIED\",\"22\",\"79\",\"true\",\"true\",\"2511000\",\"7489000\""),
                    "転用簿価・私用減価・事業償却累計・中古根拠の出力が不正です：" + convertedRow);
            require(service.exportBooks(userId, YEAR).csv().contains("供用開始時簿価=7489000"),
                    "帳簿一括CSVに転用計算根拠がありません");

            form.setAssetName("旧方式転用の合成資産");
            form.setAcquisitionDate("2006-10-20");
            form.setServiceStartDate("2025-02-01");
            form.setAcquisitionCost("30000000");
            form.setDepreciationMethod("OLD_STRAIGHT_LINE");
            form.setLifeBasis("STATUTORY");
            form.setUsedElapsedMonths(null);
            form.setUsedRequirementsConfirmed(false);
            require(input.addFixedAsset(userId, YEAR, form).isEmpty(), "旧方式転用を登録できません");
            require(service.exportBooks(userId, YEAR).csv().isEmpty(), "追加後の確認が取り消されません");

            form.setAssetName("定率法の合成資産");
            form.setAcquisitionDate("2025-01-01");
            form.setServiceStartDate("2025-01-01");
            form.setAcquisitionCost("1000000");
            form.setUsefulLife("10");
            form.setDepreciationMethod("DECLINING_200");
            form.setPrivateUseConversion(false);
            require(input.addFixedAsset(userId, YEAR, form).isEmpty(), "定率法を登録できません");
            confirmDepreciationRules(input, userId, YEAR);
            require(service.exportBooks(userId, YEAR).csv().isEmpty(), "法定方法だけの確認で定率法を出力できました");
            TaxReturnForms.DepreciationRulesForm confirmation = new TaxReturnForms.DepreciationRulesForm();
            confirmation.setMethodBasis("MIXED");
            confirmation.setScopeConfirmed(true);
            confirmation.setBusinessMonths("12");
            confirmation.setEvidence("合成テスト：資産区分別の選定届出・みなし選定等を確認");
            require(input.saveDepreciationRules(userId, YEAR, confirmation).isEmpty(), "定率法の根拠を保存できません");
            var books = service.exportBooks(userId, YEAR);
            require(!books.csv().isEmpty() && books.page().getErrors().isEmpty(),
                    "拡張方式の帳簿・決算書が一致しません：" + books.page().getErrors());
            require(books.page().getExtras().getAssets().stream().anyMatch(a ->
                    "旧方式転用の合成資産".equals(a.getName()) && a.getDepreciation().intValueExact() == 1138500
                    && a.getDepreciationBasis().intValueExact() == 27000000), "旧方式の帳票基礎額が不正です");
            require(input.saveDepreciationRules(userId, 2026, confirmation).isEmpty(), "翌年分の確認を保存できません");
            var nextBooks = service.exportBooks(userId, 2026);
            require(!nextBooks.csv().isEmpty() && nextBooks.page().getErrors().isEmpty()
                    && nextBooks.page().getExtras().getAssets().stream().anyMatch(a ->
                        converted.getId().equals(a.getId()) && a.getOpening().intValueExact() == 7016500
                        && a.getDepreciation().intValueExact() == 630000),
                    "転用資産の翌年繰越・当年償却が不正です：" + nextBooks.page().getErrors());
            TaxReturnForms.FixedAssetEventForm disposal = new TaxReturnForms.FixedAssetEventForm();
            disposal.setAssetId(converted.getId());
            disposal.setEventType("DISPOSAL");
            disposal.setEffectiveDate("2026-06-15");
            require(input.addFixedAssetEvent(userId, 2026, disposal).isEmpty(), "転用後の除却を登録できません");
            require(input.saveDepreciationRules(userId, 2026, confirmation).isEmpty(), "除却後の確認を保存できません");
            var disposalBooks = service.exportBooks(userId, 2026);
            require(!disposalBooks.csv().isEmpty() && disposalBooks.page().getErrors().isEmpty(),
                    "転用後の除却で会計データが一致しません：" + disposalBooks.page().getErrors());
            next.setStatus("closed");
            require(!input.deleteFixedAsset(userId, YEAR, converted.getId()).isEmpty(),
                    "確定した翌年に影響する転用資産を削除できました");
            status.setRollbackOnly();
        });
    }

    private void verifyFixedAssetLedger(AnnotationConfigApplicationContext context,
            TaxReturnService service, Long userId, TransactionTemplate tx) {
        TaxReturnService.BooksExport base = service.exportFixedAssetLedger(userId, YEAR);
        require(base.page().getErrors().isEmpty() && base.csv().startsWith("\uFEFF")
                && base.csv().contains("\"2025-10-01\",\"2025-10-01\",\"192000\"")
                && base.csv().contains("\"12000\",\"100.00\",\"12000\",\"12000\",\"\",\"180000\""),
                "固定資産台帳の独立出力に取得・供用・償却・期末残高がありません");
        tx.executeWithoutResult(status -> {
            BusinessProfile profile = context.getBean(BusinessProfileRepository.class).findByAppUserId(userId).orElseThrow();
            org.example.entity.FixedAsset retired = ledgerAsset(context, profile, "過年度売却資産", 2024,
                    "240000", "STRAIGHT_LINE");
            org.example.entity.FixedAsset writtenOff = ledgerAsset(context, profile, "償却済み資産", 2024,
                    "100000", "SMALL_AMOUNT_SPECIAL");
            org.example.entity.FixedAsset future = ledgerAsset(context, profile, "未来取得資産", 2026,
                    "240000", "STRAIGHT_LINE");
            org.example.entity.FixedAsset pending = ledgerAsset(context, profile, "未供用資産", 2025,
                    "240000", "STRAIGHT_LINE");
            pending.setServiceStartDate(LocalDate.of(2026, 1, 1));
            pending.setNotes(" \t=1+1");
            org.example.entity.FixedAssetEvent sale = ledgerEvent(context, retired, "SALE",
                    LocalDate.of(2024, 12, 31), "120000", null);
            org.example.entity.FixedAssetEvent ratio = ledgerEvent(context, writtenOff, "BUSINESS_RATIO_CHANGE",
                    LocalDate.of(2025, 1, 1), null, "50");
            org.example.entity.FixedAssetEvent futureDisposal = ledgerEvent(context, writtenOff, "DISPOSAL",
                    LocalDate.of(2026, 12, 31), null, null);
            String csv = FixedAssetLedgerCsv.build(YEAR, List.of(retired, writtenOff, future, pending),
                    List.of(futureDisposal, ratio, sale), context.getBean(AccountMappingService.class).snapshot());
            require(csv.lines().allMatch(line -> line.split("\",\"", -1).length == 42),
                    "固定資産台帳の資産行とイベント行で列数が一致しません");
            String retiredRow = assetCsvRow(csv, retired.getId());
            require(retiredRow.contains("\"0\",\"0\",\"100.00\",\"0\",\"48000\",\"192000\",\"0\",\"売却済み\""),
                    "過年度売却資産の当年償却がゼロにならないか、売却時簿価が失われています");
            require(csv.contains("\"SALE\",\"2024-12-31\",\"\",\"120000\"")
                    && csv.contains("\"BUSINESS_RATIO_CHANGE\",\"2025-01-01\",\"50\"")
                    && !csv.contains("2026-12-31") && !csv.contains("未来取得資産"),
                    "固定資産イベントの履歴または選択年末の境界が不正です");
            String writtenOffRow = assetCsvRow(csv, writtenOff.getId());
            require(writtenOffRow.contains("\"0\",\"0\",\"50\",\"0\",\"100000\",\"\",\"0\",\"保有\""),
                    "償却済み資産が省略されたか償却累計・割合変更が誤っています");
            String pendingRow = assetCsvRow(csv, pending.getId());
            require(pendingRow.contains("\"0\",\"0\",\"0\",\"100.00\",\"0\",\"0\",\"\",\"240000\",\"未供用\"")
                    && csv.contains("\"' \t=1+1\""),
                    "未供用資産の残高・償却またはCSV数式対策が誤っています");
            org.example.entity.FixedAsset ceased = ledgerAsset(context, profile, "廃業時残存資産", 2024,
                    "240000", "STRAIGHT_LINE");
            org.example.entity.FixedAssetEvent cessation = ledgerEvent(context, ceased, "BUSINESS_CESSATION",
                    LocalDate.of(2025, 6, 15), null, null);
            String cessationCsv = FixedAssetLedgerCsv.build(YEAR, List.of(ceased), List.of(cessation),
                    context.getBean(AccountMappingService.class).snapshot());
            require(assetCsvRow(cessationCsv, ceased.getId())
                    .contains("\"6\",\"192000\",\"24000\",\"100.00\",\"24000\",\"72000\",\"\",\"168000\",\"廃業\""),
                    "廃業月までの償却または廃業時残存簿価の表示が不正です");
            String postCessationCsv = FixedAssetLedgerCsv.build(YEAR + 1, List.of(ceased), List.of(cessation),
                    context.getBean(AccountMappingService.class).snapshot());
            String postCessationRow = postCessationCsv.lines()
                    .filter(line -> line.startsWith("\"2026\",\"資産\",\"" + ceased.getId() + "\","))
                    .findFirst().orElseThrow();
            require(postCessationRow.contains(
                    "\"0\",\"168000\",\"0\",\"100.00\",\"0\",\"72000\",\"\",\"168000\",\"廃業後（残存簿価の処理確認）\""),
                    "廃業後の残存簿価が後年の固定資産台帳に引き継がれていません");
            require(FixedAssetLedgerCsv.build(YEAR, List.of(), List.of(),
                    context.getBean(AccountMappingService.class).snapshot()).lines().count() == 1,
                    "資産なしの台帳に架空の明細が出力されました");
            Long foreignId = seedUser(context, tx, "ledger-other@example.com");
            BusinessProfile foreignProfile = context.getBean(BusinessProfileRepository.class)
                    .findByAppUserId(foreignId).orElseThrow();
            ledgerAsset(context, foreignProfile, "他利用者の専用資産", YEAR, "240000", "STRAIGHT_LINE");
            context.getBean(ClosingJournalService.class).rebuildOpenYears(foreignProfile.getId());
            confirmDepreciationRules(context.getBean(TaxReturnInputService.class), foreignId, YEAR);
            TaxReturnService.BooksExport foreignExport = service.exportFixedAssetLedger(foreignId, YEAR);
            require(foreignExport.page().getErrors().isEmpty()
                    && foreignExport.csv().contains("他利用者の専用資産")
                    && !foreignExport.csv().contains("ノートPC")
                    && !foreignExport.csv().contains("過年度売却資産"),
                    "固定資産台帳の利用者分離が機能していません");
            boolean rejected = false;
            try {
                service.exportFixedAssetLedger(foreignId, YEAR + 1);
            } catch (org.example.features.entry.EntryNotFoundException expected) {
                rejected = true;
            }
            require(rejected, "他利用者だけが持つ年分の固定資産台帳を取得できました");
            status.setRollbackOnly();
        });
    }

    private org.example.entity.FixedAsset ledgerAsset(AnnotationConfigApplicationContext context,
            BusinessProfile profile, String name, int year, String cost, String method) {
        org.example.entity.FixedAsset asset = new org.example.entity.FixedAsset();
        asset.setBusinessProfile(profile);
        asset.setAssetName(name);
        asset.setAccountCode("TOOLS_EQUIPMENT");
        asset.setAcquisitionDate(LocalDate.of(year, 1, 1));
        asset.setServiceStartDate(LocalDate.of(year, 1, 1));
        asset.setAcquisitionCost(new BigDecimal(cost));
        asset.setUsefulLife(5);
        asset.setDepreciationMethod(method);
        asset.setBusinessRatio(new BigDecimal("100.00"));
        asset.setAcquisitionFunding("OWNER_FUNDS");
        return context.getBean(org.example.repository.FixedAssetRepository.class).save(asset);
    }

    private org.example.entity.FixedAssetEvent ledgerEvent(AnnotationConfigApplicationContext context,
            org.example.entity.FixedAsset asset, String type, LocalDate date, String proceeds, String ratio) {
        org.example.entity.FixedAssetEvent event = new org.example.entity.FixedAssetEvent();
        event.setFixedAsset(asset);
        event.setEventType(type);
        event.setEffectiveDate(date);
        event.setProceeds(proceeds == null ? null : new BigDecimal(proceeds));
        event.setBusinessRatio(ratio == null ? null : new BigDecimal(ratio));
        return context.getBean(org.example.repository.FixedAssetEventRepository.class).save(event);
    }

    private String assetCsvRow(String csv, Long id) {
        return csv.lines().filter(line -> line.startsWith("\"2025\",\"資産\",\"" + id + "\","))
                .findFirst().orElseThrow(() -> new IllegalStateException("台帳に資産がありません: " + id));
    }

    /** 旧欄番号（令和5年分以降用様式と不一致）で登録済みの環境でも、起動時のマスタ投入で公式の欄番号へ揃う。 */
    private void verifyStatementLineSync(AnnotationConfigApplicationContext context, AccountMappingService mappingService,
            TransactionTemplate tx) {
        StatementLineRepository repository = context.getBean(StatementLineRepository.class);
        tx.executeWithoutResult(status -> repository.findAll().forEach(line -> {
            if (AccountMasterSeed.LINE_INCOME.equals(line.getLineCode())) {
                line.setLineNumber(43);
                line.setDisplayOrder(43);
                line.setLineLabel("所得金額（㊶－㊷）");
                repository.save(line);
            }
        }));
        mappingService.ensureMasterData();
        StatementLine income = repository.findAll().stream()
                .filter(l -> AccountMasterSeed.LINE_INCOME.equals(l.getLineCode())).findFirst().orElseThrow();
        require(income.getLineNumber() == 45 && income.getDisplayOrder() == 45
                && "所得金額（㊸－㊹）".equals(income.getLineLabel()) && repository.count() == 45,
                "既存の決算書欄が公式の欄番号へ更新されていません");
    }

    private void verifyOfficialLineNumbers(TaxReturnPageData page) {
        Map<String, Integer> expected = Map.of(
                AccountMasterSeed.LINE_NET_BEFORE_RESERVES, 33,
                AccountMasterSeed.LINE_REVERSAL_BAD_DEBT, 34,
                AccountMasterSeed.LINE_REVERSAL_TOTAL, 37,
                AccountMasterSeed.LINE_DEDICATED_SALARY, 38,
                AccountMasterSeed.LINE_PROVISION_BAD_DEBT, 39,
                AccountMasterSeed.LINE_PROVISION_TOTAL, 42,
                AccountMasterSeed.LINE_INCOME_BEFORE_BLUE, 43,
                AccountMasterSeed.LINE_BLUE_DEDUCTION, 44,
                AccountMasterSeed.LINE_INCOME, 45);
        Map<String, TaxReturnPageData.StatementRow> rows = new java.util.HashMap<>();
        page.getStatementRows().forEach(r -> rows.put(r.getCode(), r));
        expected.forEach((code, number) -> require(rows.containsKey(code) && rows.get(code).getNumber() == number,
                "決算書の欄番号が令和5年分以降用様式と一致しません: " + code));
        require("㊳".equals(rows.get(AccountMasterSeed.LINE_DEDICATED_SALARY).getNumberLabel())
                && rows.get(AccountMasterSeed.LINE_INCOME_BEFORE_BLUE).getLabel().contains("㉝＋㊲－㊷")
                && rows.get(AccountMasterSeed.LINE_INCOME).getLabel().contains("㊸－㊹"),
                "決算書の欄記号・計算式の表示が様式と一致しません");
        BigDecimal reserves = rows.get(AccountMasterSeed.LINE_NET_BEFORE_RESERVES).getAmount()
                .add(rows.get(AccountMasterSeed.LINE_REVERSAL_TOTAL).getAmount())
                .subtract(rows.get(AccountMasterSeed.LINE_PROVISION_TOTAL).getAmount());
        require(reserves.compareTo(rows.get(AccountMasterSeed.LINE_INCOME_BEFORE_BLUE).getAmount()) == 0
                && rows.get(AccountMasterSeed.LINE_PROVISION_TOTAL).getAmount()
                        .compareTo(rows.get(AccountMasterSeed.LINE_DEDICATED_SALARY).getAmount()) == 0,
                "繰戻額等・繰入額等の合計または控除前所得の計算が誤っています");
    }

    private void verifyBlueReturnEditions() {
        require(salaryRow("2025-04-01", "2025-04-30").getWorkMonths() == 1
                && salaryRow("2025-01-01", "2025-12-31").getWorkMonths() == 12
                && salaryRow("2024-11-01", "2025-12-31").getWorkMonths() == 12
                && salaryRow("2025-12-01", "2025-01-31").getWorkMonths() == 0
                && salaryRow("不正", "2025-12-31").getWorkMonths() == 0,
                "給与内訳の従事月数の境界値が誤っています");
        BlueReturnEdition r6 = BlueReturnEdition.forTaxYear(2024);
        BlueReturnEdition r7 = BlueReturnEdition.forTaxYear(2025);
        BlueReturnEdition r8 = BlueReturnEdition.forTaxYear(2026);
        BlueReturnEdition unsupported = BlueReturnEdition.forTaxYear(2027);
        require("2025年3月17日".equals(r6.getFilingDeadlineText())
                && r6.getSourceUrl().endsWith("/syotoku/r06.htm"),
                "令和6年分の公式資料または期限情報が誤っています");
        require("2026年3月16日".equals(r7.getFilingDeadlineText())
                && r7.getSourceUrl().endsWith("/syotoku/r07.htm")
                && r7.getStatus().contains("訂正票") && r7.getStatus().contains("影響なし"),
                "令和7年分の公式資料・訂正票または期限情報が誤っています");
        require(r6.getCode().contains("NTA0KOA21") && r7.getCode().contains("NTA0KOA21")
                && r6.getStatus().contains("照合済み") && r7.getStatus().contains("照合済み")
                && r6.getCode().length() <= 40 && r7.getCode().length() <= 40,
                "令和6・7年分の様式照合状態が記録されていません");
        require("R08-UNCONFIRMED".equals(r8.getCode()) && r8.getFilingDeadline() == null
                && r8.getStatus().contains("未公表"),
                "令和8年分を公表前に照合済み・期限確定として扱っています");
        require(r8.getFilingDeadlineText().startsWith("未確定")
                && r8.getStatus().contains("未確認"),
                "未確認の令和8年分に確定期限・様式を割り当てています");
        require(unsupported.getFilingDeadlineText().startsWith("未確定")
                && unsupported.getStatus().contains("未確認"),
                "未対応年分に確認済みの期限・様式を割り当てています");
    }

    private void verifyFixedAssetEvents(AnnotationConfigApplicationContext context, TaxReturnInputService inputService,
            TaxReturnService taxReturnService, Long userId) {
        TaxReturnPageData base = taxReturnService.load(userId, YEAR, null);
        Long assetId = base.getExtras().getAssets().get(0).getId();
        TaxReturnForms.FixedAssetEventForm ratio = new TaxReturnForms.FixedAssetEventForm();
        ratio.setAssetId(assetId);
        ratio.setEventType("BUSINESS_RATIO_CHANGE");
        ratio.setEffectiveDate("2025-01-01");
        ratio.setBusinessRatio("50");
        require(inputService.addFixedAssetEvent(userId, YEAR, ratio).isEmpty(),
                "事業専用割合の変更登録に失敗しました");
        require(taxReturnService.exportBooks(userId, YEAR).csv().isEmpty(),
                "資産変更後に要件の再確認なしで出力できました");
        confirmDepreciationRules(inputService, userId, YEAR);
        TaxReturnPageData changedRatio = taxReturnService.load(userId, YEAR, null);
        Map<String, StatementRow> ratioRows = changedRatio.getStatementRows().stream()
                .collect(Collectors.toMap(StatementRow::getCode, Function.identity()));
        requireAmount(ratioRows, "DEPRECIATION", "6000");

        TaxReturnForms.FixedAssetEventForm sale = new TaxReturnForms.FixedAssetEventForm();
        sale.setAssetId(assetId);
        sale.setEventType("SALE");
        sale.setEffectiveDate("2025-12-31");
        sale.setProceeds("100000");
        require(inputService.addFixedAssetEvent(userId, YEAR, sale).isEmpty(), "固定資産売却の登録に失敗しました");
        confirmDepreciationRules(inputService, userId, YEAR);
        TaxReturnPageData sold = taxReturnService.load(userId, YEAR, null);
        require(sold.getExtras().getAssets().get(0).getClosing().signum() == 0,
                "売却後の固定資産期末残高がゼロになりません");
        TaxReturnService.BooksExport soldBooks = taxReturnService.exportBooks(userId, YEAR);
        require(soldBooks.page().getErrors().isEmpty()
                && soldBooks.csv().contains("\"SALE\",\"100000\""),
                "固定資産の売却が帳簿CSVにありません");
        String soldLedger = taxReturnService.exportFixedAssetLedger(userId, YEAR).csv();
        require(soldLedger.contains("\"SALE\",\"2025-12-31\",\"\",\"100000\"")
                && assetCsvRow(soldLedger, assetId).contains("\"12000\",\"50.00\",\"6000\",\"12000\",\"180000\",\"0\",\"売却済み\""),
                "単独台帳の当年売却・残高が会計データと一致しません");
        Long saleId = sold.getExtras().getFixedAssetEvents().stream()
                .filter(row -> "SALE".equals(row.getEventType())).findFirst().orElseThrow().getId();
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        boolean saleJournalValid = tx.execute(status -> {
            JournalEntry saleJournal = context.getBean(JournalEntryRepository.class)
                    .findBySourceTypeAndSourceId("FIXED_ASSET_EVENT", saleId).orElseThrow();
            return saleJournal.getLines().stream().anyMatch(line -> "普通預金".equals(line.getAccountName())
                            && "D".equals(line.getDebitCredit()) && line.getAmount().compareTo(new BigDecimal("100000")) == 0)
                    && saleJournal.getLines().stream().anyMatch(line -> "事業主貸".equals(line.getAccountName())
                            && "D".equals(line.getDebitCredit()) && line.getAmount().compareTo(new BigDecimal("80000")) == 0);
        });
        require(Boolean.TRUE.equals(saleJournalValid), "固定資産売却・売却損の仕訳がありません");
        require(inputService.deleteFixedAssetEvent(userId, YEAR, saleId).isEmpty(), "固定資産売却イベントの削除に失敗しました");

        TaxReturnForms.FixedAssetEventForm disposal = new TaxReturnForms.FixedAssetEventForm();
        disposal.setAssetId(assetId);
        disposal.setEventType("DISPOSAL");
        disposal.setEffectiveDate("2025-12-31");
        require(inputService.addFixedAssetEvent(userId, YEAR, disposal).isEmpty(), "固定資産除却の登録に失敗しました");
        confirmDepreciationRules(inputService, userId, YEAR);
        TaxReturnPageData disposed = taxReturnService.load(userId, YEAR, null);
        Long disposalId = disposed.getExtras().getFixedAssetEvents().stream()
                .filter(row -> "DISPOSAL".equals(row.getEventType())).findFirst().orElseThrow().getId();
        boolean disposalJournalValid = tx.execute(status -> {
            JournalEntry disposalJournal = context.getBean(JournalEntryRepository.class)
                    .findBySourceTypeAndSourceId("FIXED_ASSET_EVENT", disposalId).orElseThrow();
            return disposalJournal.getLines().stream().anyMatch(line -> "雑費".equals(line.getAccountName())
                    && "D".equals(line.getDebitCredit()) && line.getAmount().compareTo(new BigDecimal("90000")) == 0);
        });
        require(Boolean.TRUE.equals(disposalJournalValid), "固定資産除却損の仕訳がありません");
        String disposalLedger = taxReturnService.exportFixedAssetLedger(userId, YEAR).csv();
        require(disposalLedger.contains("\"DISPOSAL\",\"2025-12-31\"")
                && assetCsvRow(disposalLedger, assetId).contains("\"180000\",\"0\",\"除却済み\""),
                "単独台帳に除却履歴または残高がありません");
        require(inputService.deleteFixedAssetEvent(userId, YEAR, disposalId).isEmpty(), "固定資産除却イベントの削除に失敗しました");
        require(inputService.deleteFixedAssetEvent(userId, YEAR,
                changedRatio.getExtras().getFixedAssetEvents().get(0).getId()).isEmpty(),
                "事業専用割合変更イベントの削除に失敗しました");
        confirmDepreciationRules(inputService, userId, YEAR);
        TaxReturnPageData restored = taxReturnService.load(userId, YEAR, null);
        Map<String, StatementRow> restoredRows = restored.getStatementRows().stream()
                .collect(Collectors.toMap(StatementRow::getCode, Function.identity()));
        requireAmount(restoredRows, "DEPRECIATION", "12000");
    }

    private void verifyTransactionJournalMismatch(AnnotationConfigApplicationContext context,
            TaxReturnService taxReturnService, Long userId, TransactionTemplate tx) {
        JournalMutation mutation = tx.execute(status -> {
            JournalEntry entry = context.getBean(JournalEntryRepository.class)
                    .findAllWithLinesByFiscalYearId(context.getBean(FiscalYearRepository.class).findAll().get(0).getId())
                    .stream().filter(e -> e.getAccountingTransaction() != null).findFirst().orElseThrow();
            JournalLine line = entry.getLines().get(0);
            BigDecimal original = line.getAmount();
            line.setAmount(original.add(BigDecimal.ONE));
            return new JournalMutation(entry.getId(), line.getLineNo(), original);
        });
        try {
            TaxReturnPageData page = taxReturnService.load(userId, YEAR, null);
            require(page.getErrors().stream().anyMatch(e -> e.contains("取引データと仕訳")),
                    "取引と仕訳の不整合がエラーとして検出されません");
            require(taxReturnService.exportBooks(userId, YEAR).csv().isEmpty(),
                    "不整合のある帳簿CSVが生成されました");
            require(taxReturnService.exportFixedAssetLedger(userId, YEAR).csv().isEmpty(),
                    "不整合のある固定資産台帳CSVが生成されました");
        } finally {
            tx.executeWithoutResult(status -> {
                JournalEntry entry = context.getBean(JournalEntryRepository.class).findById(mutation.entryId()).orElseThrow();
                entry.getLines().stream().filter(line -> line.getLineNo() == mutation.lineNo()).findFirst().orElseThrow()
                        .setAmount(mutation.originalAmount());
            });
        }
    }

    private record JournalMutation(Long entryId, Integer lineNo, BigDecimal originalAmount) {
    }

    private void verifyHouseholdAndSalaryDetails(AnnotationConfigApplicationContext context, Long userId,
            TaxReturnService taxReturnService) {
        TaxReturnInputService inputService = context.getBean(TaxReturnInputService.class);
        TaxReturnForms.HouseholdConsumptionForm consumption = new TaxReturnForms.HouseholdConsumptionForm();
        consumption.setTransactionDate("2025-08-15");
        consumption.setDetails("自家消費の野菜");
        consumption.setAmount("2500");
        require(inputService.addHouseholdConsumption(userId, YEAR, consumption).isEmpty(), "家事消費等の登録に失敗しました");

        TaxReturnForms.EmployeeSalaryForm salary = new TaxReturnForms.EmployeeSalaryForm();
        salary.setSalaryKind("DEDICATED_SALARY");
        salary.setPersonName("専従者テスト");
        salary.setJobDescription("経理補助");
        salary.setWorkPeriodStart("2025-01-01");
        salary.setWorkPeriodEnd("2025-12-31");
        salary.setPaymentPeriod("毎月末日");
        salary.setAmount("80000");
        require(inputService.addEmployeeSalaryDetail(userId, YEAR, salary).isEmpty(), "給与個人別内訳の登録に失敗しました");
        TaxReturnForms.EmployeeSalaryForm invalidSalary = new TaxReturnForms.EmployeeSalaryForm();
        invalidSalary.setSalaryKind("OTHER");
        require(!inputService.addEmployeeSalaryDetail(userId, YEAR, invalidSalary).isEmpty(), "不正な給与区分が受理されました");

        TaxReturnPageData page = taxReturnService.load(userId, YEAR, null);
        Map<String, StatementRow> rows = page.getStatementRows().stream()
                .collect(Collectors.toMap(StatementRow::getCode, Function.identity()));
        requireAmount(rows, "SALES", "1003000");
        require(page.getMonths().stream().filter(m -> m.getMonth() == 8).findFirst().orElseThrow()
                .getIncome().compareTo(new BigDecimal("2500")) >= 0, "家事消費等が月別売上に反映されていません");
        require(page.getExtras().getHouseholdConsumptionTotal().compareTo(new BigDecimal("2500")) == 0,
                "家事消費等の年計が不正です");
        require(page.getExtras().getHouseholdConsumptionRows().size() == 1, "家事消費等の明細が表示されていません");
        require(page.getExtras().getEmployeeSalaryRows().size() == 1, "給与の個人別明細が表示されていません");
        require(page.getChecks().stream().anyMatch(c -> "check-salary-details".equals(c.getId()) && c.isOk()),
                "給与個人別内訳と会計データの照合が成功していません: " + page.getWarnings()
                        + " / dedicated=" + page.getExtras().getDedicatedSalary()
                        + " / detail=" + page.getExtras().getEmployeeSalaryRows().get(0).getAmount());
        require(page.getJournalAll().stream().anyMatch(line -> "事業主貸".equals(line.getDebitAccount())
                && "売上高".equals(line.getCreditAccount()) && line.getDebitAmount().compareTo(new BigDecimal("2500")) == 0),
                "家事消費等の仕訳がありません");

        TaxReturnForms.EmployeeSalaryForm mismatchedSalary = new TaxReturnForms.EmployeeSalaryForm();
        mismatchedSalary.setSalaryKind("SALARIES");
        mismatchedSalary.setPersonName("給与不一致テスト");
        mismatchedSalary.setJobDescription("検証");
        mismatchedSalary.setWorkPeriodStart("2025-01-01");
        mismatchedSalary.setWorkPeriodEnd("2025-12-31");
        mismatchedSalary.setPaymentPeriod("年末");
        mismatchedSalary.setAmount("1000");
        require(inputService.addEmployeeSalaryDetail(userId, YEAR, mismatchedSalary).isEmpty(),
                "不一致検証用の給与内訳登録に失敗しました");
        TaxReturnPageData mismatchedPage = taxReturnService.load(userId, YEAR, null);
        require(mismatchedPage.getChecks().stream().anyMatch(c ->
                "check-salary-details".equals(c.getId()) && !c.isOk()), "給与内訳と会計金額の不一致が検出されません");
        Long mismatchedId = mismatchedPage.getExtras().getEmployeeSalaryRows().stream()
                .filter(row -> "SALARIES".equals(row.getSalaryKind())).findFirst().orElseThrow().getId();
        require(inputService.deleteEmployeeSalaryDetail(userId, YEAR, mismatchedId).isEmpty(),
                "不一致検証用給与内訳の削除に失敗しました");

        require(inputService.deleteHouseholdConsumption(userId, YEAR,
                page.getExtras().getHouseholdConsumptionRows().get(0).getId()).isEmpty(), "家事消費等の削除に失敗しました");
        require(inputService.deleteEmployeeSalaryDetail(userId, YEAR,
                page.getExtras().getEmployeeSalaryRows().get(0).getId()).isEmpty(), "給与個人別内訳の削除に失敗しました");
    }

    private void verifyRetentionRecords(AnnotationConfigApplicationContext context, Long userId, TransactionTemplate tx) {
        RetentionService retentionService = context.getBean(RetentionService.class);
        RetentionPageData initial = retentionService.load(userId, YEAR);
        require(initial.getRecords().isEmpty(), "保存台帳に初期データがあります");
        require(!initial.getJournalEntries().isEmpty(), "関連仕訳の選択肢がありません");

        RetentionRecordForm book = new RetentionRecordForm();
        book.setRecordKind("BOOK");
        book.setDocumentType("仕訳帳");
        book.setTitle("2025年分の仕訳帳");
        book.setRetentionStartDate("2026-03-16");
        book.setRetentionDeadline("2032-03-15");
        book.setLegalBasis("2025年分の公式資料に基づき利用者が確認");
        require(retentionService.add(userId, YEAR, book).isEmpty(), "帳簿の保存期限を登録できません");

        Long journalId = initial.getJournalEntries().get(0).getId();
        RetentionRecordForm transaction = new RetentionRecordForm();
        transaction.setRecordKind("TRANSACTION");
        transaction.setDocumentType("売上取引");
        transaction.setTitle("関連取引");
        transaction.setJournalEntryId(journalId);
        transaction.setRetentionStartDate("2026-03-16");
        transaction.setRetentionDeadline("2032-03-15");
        transaction.setLegalBasis("2025年分の公式資料に基づき利用者が確認");
        require(retentionService.add(userId, YEAR, transaction).isEmpty(), "取引の保存期限を登録できません");

        RetentionRecordForm evidence = new RetentionRecordForm();
        evidence.setRecordKind("EVIDENCE");
        evidence.setDocumentType("領収書");
        evidence.setTitle("取引に関する領収書メタデータ");
        evidence.setJournalEntryId(journalId);
        evidence.setRetentionStartDate("2026-03-16");
        evidence.setRetentionDeadline("2032-03-15");
        evidence.setLegalBasis("2025年分の公式資料に基づき利用者が確認");
        require(retentionService.add(userId, YEAR, evidence).isEmpty(), "証憑メタデータを仕訳に紐付けられません");

        RetentionPageData saved = retentionService.load(userId, YEAR);
        require(saved.getRecords().size() == 3, "保存管理台帳の登録件数が不正です");
        require(saved.getRecords().stream().anyMatch(record -> "EVIDENCE".equals(record.getRecordKind())
                && journalId.equals(record.getJournalEntryId())), "証憑メタデータの仕訳リンクが保存されていません");

        RetentionRecordForm invalidDate = new RetentionRecordForm();
        invalidDate.setRecordKind("BOOK");
        invalidDate.setDocumentType("帳簿");
        invalidDate.setTitle("期限不正");
        invalidDate.setRetentionStartDate("2026-03-16");
        invalidDate.setRetentionDeadline("2026-03-15");
        invalidDate.setLegalBasis("確認済みの根拠");
        require(!retentionService.add(userId, YEAR, invalidDate).isEmpty(), "起算日より前の期限が受け入れられました");

        RetentionRecordForm invalidLink = new RetentionRecordForm();
        invalidLink.setRecordKind("TRANSACTION");
        invalidLink.setDocumentType("取引記録");
        invalidLink.setTitle("仕訳なし");
        Long otherYearJournalId = tx.execute(status -> {
            BusinessProfile profile = context.getBean(BusinessProfileRepository.class).findByAppUserId(userId)
                    .orElseThrow();
            FiscalYear otherYear = new FiscalYear();
            otherYear.setBusinessProfile(profile);
            otherYear.setTaxYear(YEAR - 1);
            otherYear.setPeriodStart(LocalDate.of(YEAR - 1, 1, 1));
            otherYear.setPeriodEnd(LocalDate.of(YEAR - 1, 12, 31));
            otherYear.setStatus("open");
            otherYear = context.getBean(FiscalYearRepository.class).save(otherYear);

            JournalEntry otherYearEntry = new JournalEntry();
            otherYearEntry.setFiscalYear(otherYear);
            otherYearEntry.setSourceType("HARNESS");
            otherYearEntry.setEntryDate(LocalDate.of(YEAR - 1, 6, 1));
            otherYearEntry.setSummary("別年度の仕訳");
            otherYearEntry.setStatus("confirmed");
            return context.getBean(JournalEntryRepository.class).save(otherYearEntry).getId();
        });
        invalidLink.setJournalEntryId(otherYearJournalId);
        invalidLink.setRetentionStartDate("2026-03-16");
        invalidLink.setRetentionDeadline("2032-03-15");
        invalidLink.setLegalBasis("確認済みの根拠");
        require(!retentionService.add(userId, YEAR, invalidLink).isEmpty(),
                "対象年度に属さない仕訳へのリンクが受け入れられました");

        RetentionForeignFixture foreign = tx.execute(status -> {
            AppUser otherUser = new AppUser();
            otherUser.setLoginId("retention-other@example.com");
            otherUser.setEmail("retention-other@example.com");
            otherUser.setPasswordHash("hash");
            otherUser.setDisplayName("Other");
            otherUser.setStatus("active");
            otherUser = context.getBean(AppUserRepository.class).save(otherUser);

            BusinessProfile otherProfile = new BusinessProfile();
            otherProfile.setAppUser(otherUser);
            otherProfile.setFilingType("blue");
            otherProfile = context.getBean(BusinessProfileRepository.class).save(otherProfile);

            FiscalYear otherYear = new FiscalYear();
            otherYear.setBusinessProfile(otherProfile);
            otherYear.setTaxYear(YEAR);
            otherYear.setPeriodStart(LocalDate.of(YEAR, 1, 1));
            otherYear.setPeriodEnd(LocalDate.of(YEAR, 12, 31));
            otherYear.setStatus("open");
            otherYear = context.getBean(FiscalYearRepository.class).save(otherYear);

            JournalEntry otherEntry = new JournalEntry();
            otherEntry.setFiscalYear(otherYear);
            otherEntry.setSourceType("HARNESS");
            otherEntry.setEntryDate(LocalDate.of(YEAR, 6, 1));
            otherEntry.setSummary("他利用者の仕訳");
            otherEntry.setStatus("confirmed");
            otherEntry = context.getBean(JournalEntryRepository.class).save(otherEntry);
            return new RetentionForeignFixture(otherUser.getId(), otherEntry.getId());
        });
        RetentionRecordForm foreignEvidence = new RetentionRecordForm();
        foreignEvidence.setRecordKind("EVIDENCE");
        foreignEvidence.setDocumentType("請求書");
        foreignEvidence.setTitle("他利用者の証憑メタデータ");
        foreignEvidence.setJournalEntryId(foreign.journalEntryId());
        foreignEvidence.setRetentionStartDate("2026-03-16");
        foreignEvidence.setRetentionDeadline("2032-03-15");
        foreignEvidence.setLegalBasis("2025年分の公式資料に基づき利用者が確認");
        require(retentionService.add(foreign.userId(), YEAR, foreignEvidence).isEmpty(),
                "他利用者が自分の証憑メタデータを登録できません");
        require(retentionService.load(userId, YEAR).getRecords().size() == 3,
                "他利用者の保存管理台帳が表示されています");
        foreignEvidence.setTitle("他利用者の仕訳への不正リンク");
        require(!retentionService.add(userId, YEAR, foreignEvidence).isEmpty(),
                "他利用者の仕訳へのリンクが受け入れられました");
        TaxReturnService taxReturnService = context.getBean(TaxReturnService.class);
        String ownedBooks = taxReturnService.exportBooks(userId, YEAR).csv();
        require(!ownedBooks.contains("他利用者の仕訳"), "他利用者の仕訳が帳簿に混入しました");
        boolean rejected = false;
        try {
            taxReturnService.exportBooks(foreign.userId(), YEAR - 1);
        } catch (org.example.features.entry.EntryNotFoundException expected) {
            rejected = true;
        }
        require(rejected, "他利用者だけが持つ年分が選択できました");
    }

    private void verifyDeductionSelection(TaxReturnService taxReturnService, Long userId) {
        TaxReturnPageData initial = taxReturnService.load(userId, YEAR, null);
        require(initial.getDeduction() == 65 && !initial.isBlueDeductionRequirementsConfirmed()
                && "UNCONFIRMED".equals(initial.getBlueDeduction65Method()),
                "控除要件の未確認状態が既定値に反映されていません");
        require(initial.getChecks().stream().anyMatch(c -> "check-blue-deduction-confirmation".equals(c.getId()) && !c.isOk()),
                "未確認の控除要件が警告対象になっていません");
        require(taxReturnService.updateDeduction(userId, YEAR, "55", true, null).isEmpty(),
                "55万円控除の選択・確認を保存できません");
        TaxReturnPageData fiftyFive = taxReturnService.load(userId, YEAR, null);
        require(fiftyFive.getDeduction() == 55 && fiftyFive.isBlueDeductionRequirementsConfirmed()
                && "NONE".equals(fiftyFive.getBlueDeduction65Method()),
                "55万円控除の選択・確認が保存されていません");
        require(fiftyFive.getFilingRequirements() != null
                && fiftyFive.getFilingRequirements().item("req-balance-sheet").getNecessity()
                        == org.example.features.taxreturn.requirements.FilingRequirements.Necessity.REQUIRED
                && fiftyFive.getChecks().stream().anyMatch(c -> "check-filing-requirements".equals(c.getId())),
                "55万円控除の必要書類が画面データに反映されていません");
        require(taxReturnService.updateDeduction(userId, YEAR, "65", true, "ETAX").isEmpty(),
                "65万円控除のe-Tax経路を保存できません");
        TaxReturnPageData sixtyFive = taxReturnService.load(userId, YEAR, null);
        require(sixtyFive.getDeduction() == 65 && sixtyFive.isBlueDeductionRequirementsConfirmed()
                && "ETAX".equals(sixtyFive.getBlueDeduction65Method()),
                "65万円控除の選択・追加要件経路が保存されていません");
        require(sixtyFive.getWarnings().stream().anyMatch(w -> w.contains("本アプリでは送信・完了確認できません")),
                "e-Tax要件のアプリ側の検証範囲が明示されていません");
        require(!taxReturnService.updateDeduction(userId, YEAR, "65", true, "INVALID").isEmpty(),
                "不正な65万円控除経路が受け入れられました");
        require(taxReturnService.updateDeduction(userId, YEAR, "65", true, "QUALIFIED_EBOOKS").isEmpty(),
                "優良な電子帳簿経路を保存できません");
        require(taxReturnService.load(userId, YEAR, null).getWarnings().stream()
                .anyMatch(w -> w.contains("優良な電子帳簿の要件・届出は本アプリでは実装・検証していません")),
                "未実装の優良な電子帳簿要件を明示していません");
        require(taxReturnService.updateDeduction(userId, YEAR, "10", true, null).isEmpty(),
                "10万円控除の選択・確認を保存できません");
        TaxReturnPageData ten = taxReturnService.load(userId, YEAR, null);
        require(ten.getDeduction() == 10 && ten.isBlueDeductionRequirementsConfirmed(),
                "10万円控除の選択・確認が保存されていません");
        require(ten.getFilingRequirements().item("req-balance-sheet").getNecessity()
                        == org.example.features.taxreturn.requirements.FilingRequirements.Necessity.OPTIONAL
                && ten.getFilingRequirements().item("req-simple-books").getNecessity()
                        == org.example.features.taxreturn.requirements.FilingRequirements.Necessity.REQUIRED,
                "10万円控除の必要書類が区別されていません");
    }

    private void verifyLedgersAndTax(AnnotationConfigApplicationContext context, TaxReturnService taxReturnService,
            TaxReturnInputService inputService, Long userId, TransactionTemplate tx) {
        TaxReturnPageData page = taxReturnService.load(userId, YEAR, null);
        require(page.getBlueReturnEdition() != null
                && page.getBlueReturnEdition().getSourceUrl().endsWith("/syotoku/r07.htm"),
                "申告帳票に年分別の公式資料参照が設定されていません");
        require(page.isBalanced(), "貸借対照表が一致しません: 差額=" + page.getBalanceDifference());
        require(page.getErrors().isEmpty(), "想定外のエラー: " + page.getErrors());
        verifyOfficialLineNumbers(page);
        require(page.getExtras().getSalesPartnerRows().size() == 1
                && "株式会社テスト".equals(page.getExtras().getSalesPartnerRows().get(0).getName())
                && page.getExtras().getSalesPartnerRows().get(0).getAmount().compareTo(page.getExtras().getSalesTotal()) == 0
                && page.getExtras().getSalesPartnerOther().signum() == 0,
                "売上（収入）金額の明細が取引先別に集計されていません");
        require(page.getExtras().getPurchasePartnerRows().stream().map(r -> r.getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add).add(page.getExtras().getPurchasePartnerOther())
                        .compareTo(page.getExtras().getPurchaseTotal()) == 0,
                "仕入金額の明細の合計が月別仕入金額の合計と一致しません");
        requireBs(page.getAssets(), "工具器具備品", "0", "180000");
        requireBs(page.getAssets(), "棚卸資産", "10000", "15000");
        requireBs(page.getLiabilities(), "元入金", "10000", "10000");
        requireBs(page.getLiabilities(), "青色申告特別控除前の所得金額", "0", "810060");
        require(page.getExtras().getAssets().size() == 1
                && page.getExtras().getDepreciationBusinessTotal().compareTo(new BigDecimal("12000")) == 0, "固定資産台帳の償却費が不正です");
        String books = taxReturnService.exportBooks(userId, YEAR).csv();
        require(books.contains("\"主要簿\",\"仕訳帳\"") && books.contains("\"主要簿\",\"総勘定元帳\"")
                && books.contains("\"簡易記帳\",\"経費帳\"") && books.contains("\"補助簿\",\"売上帳\"")
                && books.contains("\"補助簿\",\"仕入帳\"") && books.contains("\"簡易記帳\",\"固定資産台帳\"")
                && books.contains("\"主要簿\",\"仕訳帳\",\"2025\""),
                "帳簿の分類または年分が不正です");
        require(!books.contains("\"補助簿\",\"買掛帳\""), "取引・残高のない買掛帳が出力されました");
        TaxReturnService.BooksExport periodBooks = taxReturnService.exportBooks(
                userId, YEAR, "2025-03-01", "2025-03-31", "SALES");
        require(periodBooks.page().getErrors().isEmpty()
                && periodBooks.csv().contains("\"2025-03-31\"")
                && periodBooks.csv().contains("\"SALES\",\"売上高\"")
                && periodBooks.csv().contains("3月分SES")
                && !periodBooks.csv().contains("AWS")
                && !periodBooks.csv().contains("\"残高\""),
                "期間・科目指定の帳簿出力が正しく絞り込まれていません");
        TaxReturnService.BooksExport invalidPeriod = taxReturnService.exportBooks(
                userId, YEAR, "2025-12-01", "2025-02-01", null);
        require(invalidPeriod.csv().isEmpty() && !invalidPeriod.page().getErrors().isEmpty(),
                "不正な期間で帳簿を出力できました");
        TaxReturnService.BooksExport invalidAccount = taxReturnService.exportBooks(
                userId, YEAR, null, null, "NOT_AN_ACCOUNT");
        require(invalidAccount.csv().isEmpty() && !invalidAccount.page().getErrors().isEmpty(),
                "存在しない科目で帳簿を出力できました");
        String filteredAssets = taxReturnService.exportFixedAssetLedger(
                userId, YEAR, "2025-01-01", "2025-09-30", "TOOLS_EQUIPMENT").csv();
        require(!filteredAssets.contains("ノートPC")
                && filteredAssets.lines().allMatch(line -> line.split("\",\"", -1).length == 42)
                && filteredAssets.contains("\"2025-01-01\",\"2025-09-30\""),
                "取得日が期間外の固定資産が科目・期間指定に含まれました");
        String balancesOnly = BooksCsv.build(page, List.of(),
                context.getBean(AccountMappingService.class).snapshot(), List.of());
        require(balancesOnly.contains("\"補助簿\",\"預金出納帳\"")
                && !balancesOnly.contains("\"補助簿\",\"売上帳\""),
                "残高のみの補助簿または存在しない売上帳が不正です");

        JournalEntry compound = new JournalEntry();
        compound.setEntryDate(LocalDate.of(YEAR, 1, 1));
        compound.setSummary(" \t=1+1");
        compound.setStatus("DRAFT");
        JournalLine debit = testLine(1, "D", "CASH", "現金", "80");
        JournalLine credit1 = testLine(2, "C", "SALES", "売上高", "60");
        JournalLine credit2 = testLine(3, "C", "ACCOUNTS_PAYABLE", "買掛金", "40");
        JournalLine receivable = testLine(4, "D", "ACCOUNTS_RECEIVABLE", "売掛金", "20");
        compound.setLines(List.of(debit, credit1, credit2, receivable));
        String synthetic = BooksCsv.build(page, List.of(compound),
                context.getBean(AccountMappingService.class).snapshot(), List.of());
        require(synthetic.contains("\"' \t=1+1\"") && synthetic.contains("\"借方\"")
                && synthetic.contains("\"補助簿\",\"買掛帳\"")
                && synthetic.contains("\"簡易記帳\",\"売掛帳\"")
                && synthetic.contains("\"補助簿\",\"売掛帳\"")
                && synthetic.contains("\"簡易記帳\",\"現金出納帳\"")
                && !synthetic.contains("\"補助簿\",\"仕入帳\"")
                && synthetic.contains("\"売上高／買掛金\"")
                && synthetic.contains("\"60\"") && synthetic.contains("\"40\""),
                "複合仕訳、補助簿またはCSV数式の無害化が不正です");
        compound.setSummary("\u00a0+1");
        credit1.setMemo("-123");
        String injection = BooksCsv.build(page, List.of(compound),
                context.getBean(AccountMappingService.class).snapshot(), List.of());
        require(injection.contains("\"'\u00a0+1\"") && injection.contains("\"'-123\""),
                "空白・数値に見える文字列のCSV数式を無害化できません");

        IncomeTaxCalculator.Result tax = page.getExtras().getTax();
        require(tax != null, "税額計算が行われていません");
        require(tax.getBusinessIncome() == 160060 && tax.getBasic() == 950000 && tax.getTaxableIncome() == 0, "2025年分の所得・基礎控除が不正です");
        require(tax.getWithholding() == 102100 && tax.getRefund() == 102100, "源泉徴収税額・還付額が不正です: " + tax.getRefund());
        require(page.getExtras().getIncomeBreakdown().stream().anyMatch(r -> "株式会社テスト".equals(r.getPayerName())),
                "第二表の所得の内訳に支払者がありません");

        TaxReturnForms.DeductionForm medical = new TaxReturnForms.DeductionForm();
        medical.setDeductionType("MEDICAL");
        medical.setPaidDate("2025-08-01");
        medical.setPaidAmount("150000");
        require(inputService.addDeduction(userId, YEAR, medical).isEmpty(), "医療費の登録に失敗しました");
        Long medicalId = tx.execute(s -> context.getBean(org.example.repository.TaxDeductionRecordRepository.class).findAll().stream()
                .filter(r -> "MEDICAL".equals(r.getDeductionType())).findFirst().orElseThrow().getId());
        require(taxReturnService.load(userId, YEAR, null).getExtras().getTax().getMedical() == 0, "未確認の控除が反映されています");
        require(inputService.updateDeductionStatus(userId, YEAR, medicalId, "VERIFIED").isEmpty(), "控除の確認に失敗しました");
        require(taxReturnService.load(userId, YEAR, null).getExtras().getTax().getMedical() == 141997, "医療費控除額が不正です");

        ConsumptionTaxCalculator.Result ct = page.getExtras().getConsumption();
        require(ct != null && ct.getTaxableBase().compareTo(new BigDecimal("909000")) == 0
                && ct.getNetTax().compareTo(new BigDecimal("35400")) == 0
                && ct.getLocalTax().compareTo(new BigDecimal("9900")) == 0
                && ct.getTotalTax().compareTo(new BigDecimal("45300")) == 0, "消費税（簡易課税）の計算が不正です");

        String csv = EtaxAssistCsv.build(page);
        require(csv.contains("\"第3期分の税額（還付される税金）\",\"102100\""), "e-Tax入力用CSVに還付額がありません");
        require("\"'=1+1\"".equals(EtaxAssistCsv.cell("=1+1")) && "\"-500\"".equals(EtaxAssistCsv.cell("-500")), "CSVの数式エスケープが不正です");

        tx.executeWithoutResult(s -> {
            FiscalYear y2025 = context.getBean(FiscalYearRepository.class).findAll().get(0);
            FiscalYear next = new FiscalYear();
            next.setBusinessProfile(y2025.getBusinessProfile());
            next.setTaxYear(YEAR + 1);
            next.setPeriodStart(LocalDate.of(YEAR + 1, 1, 1));
            next.setPeriodEnd(LocalDate.of(YEAR + 1, 12, 31));
            next.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(next);
            context.getBean(ClosingJournalService.class).rebuildOpenYears(y2025.getBusinessProfile().getId());
        });
        TaxReturnPageData next = taxReturnService.load(userId, YEAR + 1, null);
        require(next.getBlueReturnEdition() != null
                && next.getBlueReturnEdition().getFilingDeadlineText().startsWith("未確定"),
                "翌年分に未確認の申告期限が確定値として表示されています");
        require(next.isBalanceCarriedForward(), "翌年の期首残高が繰り越されていません");
        require(next.isBalanced(), "翌年の貸借対照表が一致しません: " + next.getBalanceDifference());
        requireBs(next.getAssets(), "工具器具備品", "180000", "132000");
        requireBs(next.getAssets(), "棚卸資産", "15000", "15000");
        BigDecimal capital = next.getAssetsOpening().subtract(next.getLiabilities().stream()
                .filter(r -> !"元入金".equals(r.getLabel())).map(TaxReturnPageData.BsRow::getOpening).reduce(BigDecimal.ZERO, BigDecimal::add));
        requireBs(next.getLiabilities(), "元入金", capital.toPlainString(), capital.toPlainString());
        require(next.getExtras().getDepreciationBusinessTotal().compareTo(new BigDecimal("48000")) == 0, "翌年の償却費が不正です");
        require(taxReturnService.exportBooks(userId, YEAR + 1).csv().isEmpty(),
                "年分を跨いで償却要件が確認済みになりました");
        confirmDepreciationRules(inputService, userId, YEAR + 1);
        String nextBooks = taxReturnService.exportBooks(userId, YEAR + 1).csv();
        require(nextBooks.contains("\"主要簿\",\"仕訳帳\",\"2026\"") && !nextBooks.contains("3月分SES"),
                "帳簿の年度分離が不正です");

        TaxYearRules r2024 = TaxYearRules.of(2024).orElseThrow();
        TaxYearRules r2025 = TaxYearRules.of(2025).orElseThrow();
        TaxYearRules r2026 = TaxYearRules.of(2026).orElseThrow();
        require(r2024.salaryIncome(2_000_000) == 1_320_000 && r2025.salaryIncome(1_900_000) == 1_250_000
                && r2026.salaryIncome(2_000_000) == 1_260_000 && r2026.salaryIncome(2_192_000) == 1_451_000, "給与所得の計算が不正です");
        require(r2024.basicDeduction(3_000_000) == 480_000 && r2025.basicDeduction(3_000_000) == 880_000
                && r2026.basicDeduction(4_000_000) == 1_040_000 && r2026.basicDeduction(7_000_000) == 620_000, "基礎控除が不正です");
        require(TaxYearRules.incomeTax(7_000_000) == 974_000, "所得税の速算表が不正です");
        IncomeTaxCalculator.Result flat = IncomeTaxCalculator.calculate(new IncomeTaxCalculator.Input(
                r2024, 5_000_000, 3_000_000, 0, null, List.of(), List.of()));
        require(flat.getIncomeTax() == 154_500 && flat.getFlatRateReduction() == 30_000 && flat.getReconstructionTax() == 2_614
                && flat.getTaxDue() == 127_100, "令和6年分の定額減税・復興特別所得税の計算が不正です: " + flat.getTaxDue());
    }

    private static JournalLine testLine(int number, String side, String code, String name, String amount) {
        JournalLine line = new JournalLine();
        line.setLineNo(number);
        line.setDebitCredit(side);
        line.setAccountCode(code);
        line.setAccountName(name);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private void requireBs(List<TaxReturnPageData.BsRow> rows, String label, String opening, String closing) {
        TaxReturnPageData.BsRow row = rows.stream().filter(r -> label.equals(r.getLabel())).findFirst()
                .orElseThrow(() -> new IllegalStateException("貸借対照表に科目がありません: " + label));
        require(row.getOpening().compareTo(new BigDecimal(opening)) == 0 && row.getClosing().compareTo(new BigDecimal(closing)) == 0,
                "貸借対照表 " + label + " が想定と異なります: " + row.getOpening() + " / " + row.getClosing());
    }

    private Long seedUser(AnnotationConfigApplicationContext context, TransactionTemplate tx) {
        return seedUser(context, tx, "harness@example.com");
    }

    private Long seedUser(AnnotationConfigApplicationContext context, TransactionTemplate tx, String loginId) {
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
            FiscalYear fy = new FiscalYear();
            fy.setBusinessProfile(profile);
            fy.setTaxYear(YEAR);
            fy.setPeriodStart(LocalDate.of(YEAR, 1, 1));
            fy.setPeriodEnd(LocalDate.of(YEAR, 12, 31));
            fy.setStatus("open");
            context.getBean(FiscalYearRepository.class).save(fy);
            return user.getId();
        });
    }

    private void saveIncomeWithholding(EntryService service, Long userId, EntryPageData page, String group, String label,
            String date, String amount, String methodId, String details, String withholding, String payer, String address) {
        EntryForm form = new EntryForm();
        form.setIncomeType(group);
        form.setCategory(itemId(page, group, label));
        form.setDate(date);
        form.setAmount(amount);
        form.setReceiveMethodId(methodId);
        form.setDetails(details);
        form.setWithholdingTax(withholding);
        form.setCounterpartyName(payer);
        form.setCounterpartyAddress(address);
        save(service, EntryKind.INCOME, userId, form);
    }

    private void saveIncome(EntryService service, Long userId, EntryPageData page, String group, String label,
            String date, String amount, String methodId, String details) {
        EntryForm form = new EntryForm();
        form.setIncomeType(group);
        form.setCategory(itemId(page, group, label));
        form.setDate(date);
        form.setAmount(amount);
        form.setReceiveMethodId(methodId);
        form.setDetails(details);
        save(service, EntryKind.INCOME, userId, form);
    }

    private void saveExpense(EntryService service, Long userId, EntryPageData page, String group, String label,
            String date, String amount, String methodId, String details) {
        EntryForm form = new EntryForm();
        form.setExpenseType(group);
        form.setCategory(itemId(page, group, label));
        form.setDate(date);
        form.setAmount(amount);
        form.setPaymentMethodId(methodId);
        form.setDetails(details);
        save(service, EntryKind.EXPENSE, userId, form);
    }

    private void save(EntryService service, EntryKind kind, Long userId, EntryForm form) {
        EntryPrepareResult prepared = service.prepareSubmission(kind, userId, form);
        require(prepared.getValidatedEntry() != null, "登録の検証に失敗しました: " + form.getDetails());
        service.save(prepared.getValidatedEntry());
    }

    private String itemId(EntryPageData page, String group, String label) {
        return page.getItems().stream()
                .filter(i -> group.equals(i.getGroup()) && label.equals(i.getLabel()))
                .map(EntryOption::getId).findFirst()
                .orElseThrow(() -> new IllegalStateException("品目がありません: " + label));
    }

    private String optionId(List<EntryOption> options, String label) {
        return options.stream().filter(o -> label.equals(o.getLabel())).map(EntryOption::getId).findFirst()
                .orElseThrow(() -> new IllegalStateException("選択肢がありません: " + label));
    }

    private void requireLine(Map<String, List<JournalLine>> journal, String summary, String dc, String code, String name) {
        List<JournalLine> lines = journal.get(summary);
        require(lines != null, "仕訳がありません: " + summary);
        require(lines.stream().anyMatch(l -> dc.equals(l.getDebitCredit()) && code.equals(l.getAccountCode())
                && name.equals(l.getAccountName())),
                "仕訳の科目が想定と異なります: " + summary + " " + dc + " " + lines.stream()
                        .map(l -> l.getDebitCredit() + ":" + l.getAccountCode() + ":" + l.getAccountName()).toList());
    }

    private void requireAmount(Map<String, StatementRow> rows, String code, String expected) {
        StatementRow row = rows.get(code);
        require(row != null, "欄がありません: " + code);
        require(row.getAmount().compareTo(new BigDecimal(expected)) == 0,
                "欄 " + code + " の金額が想定と異なります: " + row.getAmount() + " != " + expected);
    }

    private static org.example.features.taxreturn.dto.TaxReturnExtras.EmployeeSalaryRow salaryRow(String start, String end) {
        return new org.example.features.taxreturn.dto.TaxReturnExtras.EmployeeSalaryRow(null, "SALARIES", "従業員",
                "事務", start, end, "毎月", BigDecimal.ZERO, false);
    }
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record RetentionForeignFixture(Long userId, Long journalEntryId) {
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "org.example.repository")
    @Import({AccountMappingServiceImpl.class, EntryServiceImpl.class, TaxReturnServiceImpl.class,
            ClosingJournalServiceImpl.class, BalanceSheetService.class, TaxReturnInputService.class,
            org.example.features.taxreturn.retention.RetentionServiceImpl.class})
    static class HarnessConfig {
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(System.getProperty("account.harness.url"));
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
