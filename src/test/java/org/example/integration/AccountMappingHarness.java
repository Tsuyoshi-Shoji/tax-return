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
import org.example.features.taxreturn.consumption.ConsumptionTaxCalculator;
import org.example.features.taxreturn.export.EtaxAssistCsv;
import org.example.features.taxreturn.incometax.IncomeTaxCalculator;
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
        AccountMappingService mappingService = context.getBean(AccountMappingService.class);
        EntryService entryService = context.getBean(EntryService.class);
        TaxReturnService taxReturnService = context.getBean(TaxReturnService.class);
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        mappingService.ensureMasterData();
        long accounts = context.getBean(LedgerAccountRepository.class).count();
        long lines = context.getBean(StatementLineRepository.class).count();
        long itemMappings = context.getBean(UiItemAccountMappingRepository.class).count();
        mappingService.ensureMasterData();
        require(accounts > 0 && lines == 43 && itemMappings > 0, "マスタ初期値が投入されていません: lines=" + lines);
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
        verifyLedgersAndTax(context, taxReturnService, inputService, userId, tx);

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
    }

    private void verifyLedgersAndTax(AnnotationConfigApplicationContext context, TaxReturnService taxReturnService,
            TaxReturnInputService inputService, Long userId, TransactionTemplate tx) {
        TaxReturnPageData page = taxReturnService.load(userId, YEAR, null);
        require(page.isBalanced(), "貸借対照表が一致しません: 差額=" + page.getBalanceDifference());
        require(page.getErrors().isEmpty(), "想定外のエラー: " + page.getErrors());
        requireBs(page.getAssets(), "工具器具備品", "0", "180000");
        requireBs(page.getAssets(), "棚卸資産", "10000", "15000");
        requireBs(page.getLiabilities(), "元入金", "10000", "10000");
        requireBs(page.getLiabilities(), "青色申告特別控除前の所得金額", "0", "810060");
        require(page.getExtras().getAssets().size() == 1
                && page.getExtras().getDepreciationBusinessTotal().compareTo(new BigDecimal("12000")) == 0, "固定資産台帳の償却費が不正です");

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
        require(next.isBalanceCarriedForward(), "翌年の期首残高が繰り越されていません");
        require(next.isBalanced(), "翌年の貸借対照表が一致しません: " + next.getBalanceDifference());
        requireBs(next.getAssets(), "工具器具備品", "180000", "132000");
        requireBs(next.getAssets(), "棚卸資産", "15000", "15000");
        BigDecimal capital = next.getAssetsOpening().subtract(next.getLiabilities().stream()
                .filter(r -> !"元入金".equals(r.getLabel())).map(TaxReturnPageData.BsRow::getOpening).reduce(BigDecimal.ZERO, BigDecimal::add));
        requireBs(next.getLiabilities(), "元入金", capital.toPlainString(), capital.toPlainString());
        require(next.getExtras().getDepreciationBusinessTotal().compareTo(new BigDecimal("48000")) == 0, "翌年の償却費が不正です");

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

    private void requireBs(List<TaxReturnPageData.BsRow> rows, String label, String opening, String closing) {
        TaxReturnPageData.BsRow row = rows.stream().filter(r -> label.equals(r.getLabel())).findFirst()
                .orElseThrow(() -> new IllegalStateException("貸借対照表に科目がありません: " + label));
        require(row.getOpening().compareTo(new BigDecimal(opening)) == 0 && row.getClosing().compareTo(new BigDecimal(closing)) == 0,
                "貸借対照表 " + label + " が想定と異なります: " + row.getOpening() + " / " + row.getClosing());
    }

    private Long seedUser(AnnotationConfigApplicationContext context, TransactionTemplate tx) {
        return tx.execute(status -> {
            AppUser user = new AppUser();
            user.setLoginId("harness@example.com");
            user.setEmail("harness@example.com");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "org.example.repository")
    @Import({AccountMappingServiceImpl.class, EntryServiceImpl.class, TaxReturnServiceImpl.class,
            ClosingJournalServiceImpl.class, BalanceSheetService.class, TaxReturnInputService.class})
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
