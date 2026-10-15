package org.example.features.entry.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.example.entity.AccountingTransaction;
import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseCategory;
import org.example.entity.ExpenseEntry;
import org.example.entity.ExpenseItem;
import org.example.entity.FiscalYear;
import org.example.entity.IncomeCategory;
import org.example.entity.IncomeEntry;
import org.example.entity.IncomeItem;
import org.example.entity.JournalEntry;
import org.example.entity.JournalLine;
import org.example.entity.PaymentMethod;
import org.example.entity.ReceiveMethod;
import org.example.entity.TaxDeductionRecord;
import org.example.entity.UserSetting;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.entry.EntryOption;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.form.EntryForm;
import org.example.features.entry.form.MonthlyExpenseRowForm;
import org.example.features.entry.service.EntryPrepareResult;
import org.example.features.entry.service.EntrySaveResult;
import org.example.features.entry.service.EntryService;
import org.example.features.entry.service.ValidatedEntryCommand;
import org.example.repository.AccountingTransactionRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntryServiceImpl implements EntryService {
    private static final int DEFAULT_MONTHLY_ROWS = 5;
    private static final int MAX_MONTHLY_ROWS = 50;
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TARGET_MONTH = DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999");
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("100000000");
    private static final BigDecimal ZERO = new BigDecimal("0");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[0-9]{1,13}$");
    private static final Pattern FORBIDDEN_DETAILS = Pattern.compile("[<>\"'`\\\\]");
    private static final String STATUS_OPEN = "open";
    private static final String STATUS_DRAFT = "draft";
    private static final String TX_TYPE_INCOME = "INCOME";
    private static final String TX_TYPE_OUTGO = "OUTGO";
    private static final String TX_TYPE_TRANSFER = "TRANSFER";
    private static final String TX_TYPE_ADJUSTMENT = "ADJUSTMENT";
    private static final String SETTLEMENT_STATUS_SETTLED = "SETTLED";
    private static final String SETTLEMENT_STATUS_UNSETTLED = "UNSETTLED";
    private static final String CATEGORY_OTHER = "other";
    private static final String EXPENSE_OTHER_ERRONEOUS_PAYMENT_REFUND = "erroneous-payment-refund";
    private static final String INCOME_OTHER_ERRONEOUS_DEPOSIT_ADJUSTMENT = "erroneous-deposit-adjustment";
    private static final String EXPENSE_OTHER_SOCIAL_INSURANCE = "national-health-pension";
    private static final String DEDUCTION_STATUS_CANDIDATE = "CANDIDATE";
    private static final String DEDUCTION_STATUS_INELIGIBLE = "INELIGIBLE";
    private static final String DEDUCTION_TYPE_SOCIAL_INSURANCE_NHI = "SOCIAL_INSURANCE_NHI";
    private static final String DEDUCTION_TYPE_SOCIAL_INSURANCE_PENSION = "SOCIAL_INSURANCE_PENSION";
    private static final String DEDUCTION_TYPE_SOCIAL_INSURANCE = "SOCIAL_INSURANCE";
    private static final String SOURCE_TYPE_ACCOUNTING_TRANSACTION = "ACCOUNTING_TRANSACTION";
    private static final Set<String> SOCIAL_INSURANCE_DETAIL_KEYWORDS = Set.of("国民健康保険", "国民年金");
    private static final List<CatalogSeed> INCOME_CATEGORY_SEEDS = List.of(
            new CatalogSeed("sales", "売上", List.of(
                    new CatalogItemSeed("product", "商品売上"),
                    new CatalogItemSeed("service", "サービス売上"),
                    new CatalogItemSeed("ses-contract", "SES・準委任契約報酬"),
                    new CatalogItemSeed("contract-system-development", "受託システム開発"),
                    new CatalogItemSeed("it-training", "IT講師・技術研修"),
                    new CatalogItemSeed("technical-content", "技術記事・コンテンツ制作"),
                    new CatalogItemSeed("saas-subscription", "自社サービス・SaaS利用料"),
                    new CatalogItemSeed("software-license-sales", "ソフトウェア・ライセンス販売"),
                    new CatalogItemSeed("advertising-affiliate", "広告・アフィリエイト収益"),
                    new CatalogItemSeed("other-sales", "その他売上"))),
            new CatalogSeed("misc", "雑収入", List.of(
                    new CatalogItemSeed("interest", "受取利息"),
                    new CatalogItemSeed("commission", "手数料収入"),
                    new CatalogItemSeed("business-subsidy", "補助金・助成金（事業関連）"),
                    new CatalogItemSeed("business-insurance", "事業関連の保険金・補償金"),
                    new CatalogItemSeed("client-gratuity", "取引先からの謝礼金"),
                    new CatalogItemSeed("business-cashback", "事業関連のキャッシュバック"),
                    new CatalogItemSeed("business-misc-income", "事業関連の雑収益"),
                    new CatalogItemSeed("other-misc-income", "その他雑収入"))),
            new CatalogSeed("other", "その他", List.of(
                    new CatalogItemSeed("other-income", "その他収益"),
                    new CatalogItemSeed("owner-loan", "事業主借入金（事業主借）"),
                    new CatalogItemSeed("loan-proceeds", "借入金入金"),
                    new CatalogItemSeed("deposit-received", "預り金の受入"),
                    new CatalogItemSeed("reimbursement-collected", "立替金の回収"),
                    new CatalogItemSeed("erroneous-deposit-adjustment", "誤入金・返金調整"),
                    new CatalogItemSeed("other-fund-transfer", "その他の資金移動"))));
    private static final List<CatalogSeed> EXPENSE_CATEGORY_SEEDS = List.of(
            new CatalogSeed("expense", "経費", List.of(
                    new CatalogItemSeed("supplies", "消耗品費"),
                    new CatalogItemSeed("communication", "通信費"),
                    new CatalogItemSeed("travel", "旅費交通費"),
                    new CatalogItemSeed("utilities", "水道光熱費"),
                    new CatalogItemSeed("cloud-services", "クラウド利用料（AWS・Azure・GCP等）"),
                    new CatalogItemSeed("server-hosting", "サーバー・ホスティング費用"),
                    new CatalogItemSeed("domain", "ドメイン取得・更新料"),
                    new CatalogItemSeed("ssl-security", "SSL証明書・セキュリティサービス"),
                    new CatalogItemSeed("ide-development-tools", "IDE・開発ツール利用料"),
                    new CatalogItemSeed("business-tools", "業務ツール利用料"),
                    new CatalogItemSeed("software-license-purchase", "ソフトウェアライセンス購入費"),
                    new CatalogItemSeed("api-usage", "API利用料・従量課金"),
                    new CatalogItemSeed("database-storage", "データベース・ストレージ利用料"),
                    new CatalogItemSeed("devices", "デバイス購入"),
                    new CatalogItemSeed("peripherals", "周辺機器・ケーブル類"),
                    new CatalogItemSeed("office-furniture", "デスク・チェア・オフィス家具"),
                    new CatalogItemSeed("office-supplies", "文房具・事務用品"),
                    new CatalogItemSeed("equipment-maintenance", "機器修理・メンテナンス費"),
                    new CatalogItemSeed("business-communication", "通信料金"),
                    new CatalogItemSeed("electricity", "電気料金"),
                    new CatalogItemSeed("gas-water", "ガス・水道料金"),
                    new CatalogItemSeed("office-rent", "事務所家賃"),
                    new CatalogItemSeed("home-office-rent", "自宅兼事務所家賃"),
                    new CatalogItemSeed("workspace-rent", "レンタルスペース利用料"),
                    new CatalogItemSeed("postage-delivery", "郵便・宅配料金"),
                    new CatalogItemSeed("transportation", "交通費"),
                    new CatalogItemSeed("business-trip-lodging", "出張宿泊費"),
                    new CatalogItemSeed("parking-tolls", "駐車場・高速道路料金"),
                    new CatalogItemSeed("fuel", "ガソリン代"),
                    new CatalogItemSeed("entertainment", "接待交際費"),
                    new CatalogItemSeed("gifts", "贈答品費"),
                    new CatalogItemSeed("sales-meetings", "営業・会議費用"),
                    new CatalogItemSeed("events", "展示会・イベント参加費"),
                    new CatalogItemSeed("advertising", "広告宣伝費"),
                    new CatalogItemSeed("learning-materials", "教材費"),
                    new CatalogItemSeed("online-learning", "オンライン学習サービス"),
                    new CatalogItemSeed("training-seminars", "技術研修・セミナー受講費"),
                    new CatalogItemSeed("certification-exam", "資格試験受験料"),
                    new CatalogItemSeed("certification-renewal", "資格更新料"),
                    new CatalogItemSeed("community-membership", "コミュニティ会費"),
                    new CatalogItemSeed("tax-accountant", "税理士・会計士報酬"),
                    new CatalogItemSeed("legal-professional", "弁護士・専門家報酬"),
                    new CatalogItemSeed("outsourcing", "業務委託・外注費"),
                    new CatalogItemSeed("payment-fees", "振込・決済手数料"),
                    new CatalogItemSeed("business-insurance", "事業用保険料"),
                    new CatalogItemSeed("business-taxes", "租税公課（事業関連）"),
                    new CatalogItemSeed("loan-interest", "借入金利息"),
                    new CatalogItemSeed("depreciation", "減価償却費"),
                    new CatalogItemSeed("employee-benefits-statutory", "法定福利費（従業員分）"),
                    new CatalogItemSeed("employee-welfare", "従業員福利厚生費"),
                    new CatalogItemSeed("blue-dedicated-employee-salary", "青色事業専従者給与"),
                    new CatalogItemSeed("recruitment-advertising", "採用・求人広告費"),
                    new CatalogItemSeed("recruitment-agency", "人材紹介手数料"),
                    new CatalogItemSeed("external-engineer", "外部エンジニア委託費"),
                    new CatalogItemSeed("design-production-outsourcing", "デザイン・制作外注費"))),
            new CatalogSeed("purchase", "仕入", List.of(
                    new CatalogItemSeed("goods", "商品仕入"),
                    new CatalogItemSeed("materials", "材料仕入"),
                    new CatalogItemSeed("software", "ソフトウェア仕入"),
                    new CatalogItemSeed("hardware", "ハードウェア仕入"),
                    new CatalogItemSeed("digital-content", "販売用デジタルコンテンツ仕入"),
                    new CatalogItemSeed("raw-material-components", "原材料・部品仕入"),
                    new CatalogItemSeed("other-purchase", "その他仕入"))),
            new CatalogSeed("other", "その他", List.of(
                    new CatalogItemSeed("other-expense", "その他支出"),
                    new CatalogItemSeed("living-expenses", "生活費"),
                    new CatalogItemSeed("entertainment-expenses", "娯楽費"),
                    new CatalogItemSeed("income-resident-tax", "所得税・住民税の納付"),
                    new CatalogItemSeed("national-health-pension", "国民健康保険・国民年金"),
                    new CatalogItemSeed("loan-principal-repayment", "借入金元本返済"),
                    new CatalogItemSeed("fixed-asset-purchase", "固定資産購入"),
                    new CatalogItemSeed("prepaid-expense", "前払費用"),
                    new CatalogItemSeed("security-deposit", "敷金・保証金"),
                    new CatalogItemSeed("reimbursement-paid", "立替金の支払い"),
                    new CatalogItemSeed("deposit-paid", "預り金の支払い"),
                    new CatalogItemSeed("erroneous-payment-refund", "誤出金・返金処理"),
                    new CatalogItemSeed("other-fund-transfer", "その他資金移動"))));
    private static final List<MethodSeed> METHOD_SEEDS = List.of(
            new MethodSeed("cash", "現金"),
            new MethodSeed("card", "クレジットカード"),
            new MethodSeed("electronic", "電子決済"),
            new MethodSeed("bank", "銀行振込"),
            new MethodSeed("other", "その他"));

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final AccountingTransactionRepository accountingTransactionRepository;
    private final UserSettingRepository userSettingRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final IncomeItemRepository incomeItemRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final ReceiveMethodRepository receiveMethodRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TaxDeductionRecordRepository taxDeductionRecordRepository;

    public EntryServiceImpl(
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
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
        this.accountingTransactionRepository = accountingTransactionRepository;
        this.userSettingRepository = userSettingRepository;
        this.incomeCategoryRepository = incomeCategoryRepository;
        this.incomeItemRepository = incomeItemRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.expenseItemRepository = expenseItemRepository;
        this.receiveMethodRepository = receiveMethodRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.taxDeductionRecordRepository = taxDeductionRecordRepository;
    }

    @Override
    @Transactional
    public EntryPageData loadPage(EntryKind kind, Long appUserId, Long editId) {
        BusinessProfile profile = ensureCatalogs(appUserId);
        MasterData masterData = loadMasterData(kind, profile.getId());
        EntryPageData pageData = createBasePage(kind, masterData, loadCurrentApportionmentRate(appUserId).orElse(null));
        if (editId == null) {
            pageData.setEntryForm(createDefaultForm(kind, masterData));
            return pageData;
        }
        if (kind.isIncome()) {
            IncomeEntry entry = incomeEntryRepository.findOwnedById(editId, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            pageData.setFiscalYearLabel(formatFiscalYear(entry.getFiscalYear()));
            pageData.setEntryForm(toIncomeForm(entry));
            return pageData;
        }
        ExpenseEntry entry = expenseEntryRepository.findOwnedById(editId, appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
        pageData.setFiscalYearLabel(formatFiscalYear(entry.getFiscalYear()));
        pageData.setEntryForm(toExpenseForm(entry));
        return pageData;
    }

    @Override
    @Transactional
    public EntryPrepareResult prepareSubmission(EntryKind kind, Long appUserId, EntryForm entryForm) {
        BusinessProfile profile = ensureCatalogs(appUserId);
        MasterData masterData = loadMasterData(kind, profile.getId());
        Optional<BigDecimal> currentRate = loadCurrentApportionmentRate(appUserId);
        EntryForm sanitizedForm = sanitize(entryForm);
        EntryPageData pageData = createBasePage(kind, masterData, currentRate.orElse(null));
        pageData.setEntryForm(sanitizedForm);
        Map<String, String> fieldErrors = pageData.getFieldErrors();

        if (kind.isExpense() && sanitizedForm.isMonthlyMode()) {
            return prepareMonthlySubmission(appUserId, sanitizedForm, pageData, masterData, currentRate);
        }

        validateLengths(kind, sanitizedForm, fieldErrors);

        String typeCode = trimToEmpty(kind.readType(sanitizedForm));
        CategorySelection categorySelection = resolveCategory(kind, masterData, typeCode, fieldErrors);
        Object itemSelection = resolveItem(kind, masterData, categorySelection, sanitizedForm.getCategory(), fieldErrors);
        LocalDate transactionDate = resolveDate(sanitizedForm.getDate(), fieldErrors, kind);
        BigDecimal amount = resolveAmount(sanitizedForm.getAmount(), fieldErrors);
        Object methodSelection = resolveMethod(kind, masterData, kind.readMethodId(sanitizedForm), fieldErrors);
        String details = resolveDetails(sanitizedForm.getDetails(), fieldErrors, kind);
        boolean effectiveExcludeFromReturn = sanitizedForm.isExcludeFromReturn();

        if (kind.isIncome() && itemSelection instanceof IncomeItem incomeItem) {
            if (requiresExcludeFromBusinessPl(incomeItem)) {
                effectiveExcludeFromReturn = true;
            }
        }

        if (kind.isExpense() && itemSelection instanceof ExpenseItem expenseItem) {
            if (requiresExcludeFromBusinessPl(expenseItem)) {
                effectiveExcludeFromReturn = true;
                if (sanitizedForm.isHomeApportionment()) {
                    fieldErrors.putIfAbsent(
                            "homeApportionment",
                            "資金移動・私的支出項目は確定申告対象外として扱うため、家事按分は適用できません。");
                }
            }
            if (isSocialInsuranceExpense(expenseItem) && details != null && !containsSocialInsuranceDetail(details)) {
                fieldErrors.putIfAbsent("details", "国民健康保険か国民年金かが分かるように支出詳細へ記載してください。");
            }
        }

        FiscalYear fiscalYear = null;
        Long normalizedVersion = normalizeVersion(sanitizedForm.getVersion());
        if (sanitizedForm.getEditId() != null) {
            if (kind.isIncome()) {
                IncomeEntry existing = incomeEntryRepository.findOwnedById(sanitizedForm.getEditId(), appUserId)
                        .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
                pageData.setFiscalYearLabel(formatFiscalYear(existing.getFiscalYear()));
                validateLockState(existing.getFiscalYear(), existing.getAccountingTransaction(), pageData);
                if (!Objects.equals(normalizedVersion, normalizeVersion(existing.getVersion()))) {
                    fieldErrors.putIfAbsent("version", "別の更新が行われたため、再読み込みしてください。");
                }
                if (transactionDate != null && !isWithinFiscalYear(transactionDate, existing.getFiscalYear())) {
                    fieldErrors.putIfAbsent("date", "編集対象の年度外には変更できません。");
                }
                fiscalYear = existing.getFiscalYear();
            } else {
                ExpenseEntry existing = expenseEntryRepository.findOwnedById(sanitizedForm.getEditId(), appUserId)
                        .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
                pageData.setFiscalYearLabel(formatFiscalYear(existing.getFiscalYear()));
                validateLockState(existing.getFiscalYear(), existing.getAccountingTransaction(), pageData);
                if (!Objects.equals(normalizedVersion, normalizeVersion(existing.getVersion()))) {
                    fieldErrors.putIfAbsent("version", "別の更新が行われたため、再読み込みしてください。");
                }
                if (transactionDate != null && !isWithinFiscalYear(transactionDate, existing.getFiscalYear())) {
                    fieldErrors.putIfAbsent("date", "編集対象の年度外には変更できません。");
                }
                fiscalYear = existing.getFiscalYear();
            }
        } else if (transactionDate != null) {
            List<FiscalYear> fiscalYears = fiscalYearRepository.findOpenByAppUserIdAndDate(appUserId, transactionDate);
            if (fiscalYears.isEmpty()) {
                fieldErrors.putIfAbsent("date", "取引日が登録可能な年度に含まれていません。");
            } else {
                fiscalYear = fiscalYears.get(0);
                pageData.setFiscalYearLabel(formatFiscalYear(fiscalYear));
            }
        }

        BigDecimal apportionmentRate = ZERO;
        BigDecimal deductibleAmount = amount;
        if (kind.isExpense()) {
            if (sanitizedForm.isHomeApportionment()) {
                if (currentRate.isEmpty()
                        || currentRate.get().compareTo(BigDecimal.ZERO) < 0
                        || currentRate.get().compareTo(new BigDecimal("100")) > 0) {
                    fieldErrors.putIfAbsent(
                            "homeApportionment",
                            "設定されている家事按分率が不正です。設定画面で0〜100%の範囲に修正してください。");
                } else {
                    apportionmentRate = currentRate.get();
                }
            }
            deductibleAmount = calculateDeductibleAmount(amount, effectiveExcludeFromReturn,
                    sanitizedForm.isHomeApportionment(), apportionmentRate);
        }

        if (!fieldErrors.isEmpty() || pageData.getFailureMessage() != null || fiscalYear == null
                || amount == null || transactionDate == null || details == null
                || itemSelection == null || methodSelection == null || categorySelection == null) {
            return new EntryPrepareResult(pageData, null);
        }

        String payloadHash = hashPayload(
                kind,
                appUserId,
                sanitizedForm.getEditId(),
                normalizedVersion,
                fiscalYear.getId(),
                categorySelection.code(),
                kind.isIncome() ? ((IncomeItem) itemSelection).getId() : ((ExpenseItem) itemSelection).getId(),
                kind.isIncome() ? ((ReceiveMethod) methodSelection).getId() : ((PaymentMethod) methodSelection).getId(),
                transactionDate,
                amount,
                effectiveExcludeFromReturn,
                sanitizedForm.isHomeApportionment(),
                apportionmentRate,
                deductibleAmount,
                details);
        boolean confirmationRequired = amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0;
        ValidatedEntryCommand command = new ValidatedEntryCommand(
                kind,
                appUserId,
                sanitizedForm.getEditId(),
                normalizedVersion,
                fiscalYear,
                kind.isIncome() ? (IncomeItem) itemSelection : null,
                kind.isExpense() ? (ExpenseItem) itemSelection : null,
                kind.isIncome() ? (ReceiveMethod) methodSelection : null,
                kind.isExpense() ? (PaymentMethod) methodSelection : null,
                transactionDate,
                amount,
                effectiveExcludeFromReturn,
                sanitizedForm.isHomeApportionment(),
                apportionmentRate,
                deductibleAmount,
                details,
                payloadHash,
                confirmationRequired);
        return new EntryPrepareResult(pageData, command);
    }

    @Override
    @Transactional
    public EntrySaveResult save(ValidatedEntryCommand command) {
        if (command.isMonthlyMode()) {
            return saveMonthlyExpense(command);
        }
        if (command.getKind().isIncome()) {
            return saveIncome(command);
        }
        return saveExpense(command);
    }

    private EntrySaveResult saveMonthlyExpense(ValidatedEntryCommand command) {
        if (!command.getKind().isExpense()) {
            throw new EntryConflictException("月間登録モードは支出のみ利用できます。");
        }
        for (ValidatedEntryCommand rowCommand : command.getMonthlyEntries()) {
            saveExpense(rowCommand);
        }
        return new EntrySaveResult(command.getKind().getPath(), "支出を" + command.getMonthlyEntries().size() + "件登録しました。");
    }

    private EntrySaveResult saveIncome(ValidatedEntryCommand command) {
        IncomeEntry entry;
        FiscalYear fiscalYear;
        if (command.getEditId() == null) {
            fiscalYear = lockOwnedFiscalYear(command.getFiscalYear().getId(), command.getAppUserId());
            entry = new IncomeEntry();
        } else {
            fiscalYear = lockOwnedFiscalYearForIncomeEntry(command.getEditId(), command.getAppUserId());
            entry = incomeEntryRepository.findOwnedByIdForUpdate(command.getEditId(), command.getAppUserId())
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            ensureExpectedFiscalYear(fiscalYear, entry.getFiscalYear());
            AccountingTransaction lockedTransaction = lockOwnedAccountingTransaction(entry.getAccountingTransaction(), command.getAppUserId());
            ensureEditable(command.getVersion(), entry.getVersion(), fiscalYear, lockedTransaction);
            if (!isWithinFiscalYear(command.getTransactionDate(), fiscalYear)) {
                throw new EntryConflictException("編集対象の年度外には変更できません。");
            }
            entry.setAccountingTransaction(lockedTransaction);
        }
        ensureOpenFiscalYear(fiscalYear);
        entry.setFiscalYear(fiscalYear);
        AccountingTransaction transaction = entry.getAccountingTransaction();
        if (transaction != null && !STATUS_DRAFT.equalsIgnoreCase(transaction.getStatus())) {
            throw new EntryConflictException("確定済みデータは更新できません。");
        }
        if (transaction == null) {
            transaction = new AccountingTransaction();
            transaction.setStatus(STATUS_DRAFT);
        }
        transaction.setFiscalYear(fiscalYear);
        transaction.setTransactionDate(command.getTransactionDate());
        transaction.setTransactionType(resolveIncomeTransactionType(command.getIncomeItem()));
        transaction.setSummary(command.getDetails());
        transaction.setTotalAmount(command.getAmount());
        transaction.setSettlementStatus(resolveIncomeSettlementStatus(command.getReceiveMethod()));
        transaction.setSettlementReferenceId(null);
        accountingTransactionRepository.save(transaction);

        entry.setIncomeItem(command.getIncomeItem());
        entry.setReceiveMethod(command.getReceiveMethod());
        entry.setTransactionDate(command.getTransactionDate());
        entry.setAmount(command.getAmount());
        entry.setExcludeFromReturn(command.isExcludeFromReturn());
        entry.setDetails(command.getDetails());
        entry.setAccountingTransaction(transaction);
        incomeEntryRepository.save(entry);
        syncIncomeJournalEntry(entry, transaction);
        syncTaxDeductionRecordForIncome(transaction, entry);
        return new EntrySaveResult(command.getKind().getPath(),
                command.getEditId() == null ? "収益を登録しました。" : "収益を更新しました。");
    }

    private EntrySaveResult saveExpense(ValidatedEntryCommand command) {
        ExpenseEntry entry;
        FiscalYear fiscalYear;
        if (command.getEditId() == null) {
            fiscalYear = lockOwnedFiscalYear(command.getFiscalYear().getId(), command.getAppUserId());
            entry = new ExpenseEntry();
        } else {
            fiscalYear = lockOwnedFiscalYearForExpenseEntry(command.getEditId(), command.getAppUserId());
            entry = expenseEntryRepository.findOwnedByIdForUpdate(command.getEditId(), command.getAppUserId())
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            ensureExpectedFiscalYear(fiscalYear, entry.getFiscalYear());
            AccountingTransaction lockedTransaction = lockOwnedAccountingTransaction(entry.getAccountingTransaction(), command.getAppUserId());
            ensureEditable(command.getVersion(), entry.getVersion(), fiscalYear, lockedTransaction);
            if (!isWithinFiscalYear(command.getTransactionDate(), fiscalYear)) {
                throw new EntryConflictException("編集対象の年度外には変更できません。");
            }
            entry.setAccountingTransaction(lockedTransaction);
        }
        ensureOpenFiscalYear(fiscalYear);
        entry.setFiscalYear(fiscalYear);
        AccountingTransaction transaction = entry.getAccountingTransaction();
        if (transaction != null && !STATUS_DRAFT.equalsIgnoreCase(transaction.getStatus())) {
            throw new EntryConflictException("確定済みデータは更新できません。");
        }
        if (transaction == null) {
            transaction = new AccountingTransaction();
            transaction.setStatus(STATUS_DRAFT);
        }
        transaction.setFiscalYear(fiscalYear);
        transaction.setTransactionDate(command.getTransactionDate());
        transaction.setTransactionType(resolveExpenseTransactionType(command.getExpenseItem()));
        transaction.setSummary(command.getDetails());
        transaction.setTotalAmount(command.getAmount());
        transaction.setSettlementStatus(resolveExpenseSettlementStatus(command.getPaymentMethod()));
        transaction.setSettlementReferenceId(null);
        accountingTransactionRepository.save(transaction);

        entry.setExpenseItem(command.getExpenseItem());
        entry.setPaymentMethod(command.getPaymentMethod());
        entry.setTransactionDate(command.getTransactionDate());
        entry.setAmount(command.getAmount());
        entry.setExcludeFromReturn(command.isExcludeFromReturn());
        entry.setHomeApportionmentApplied(command.isHomeApportionment());
        entry.setHomeApportionmentRate(command.getHomeApportionmentRate());
        entry.setDeductibleAmount(command.getDeductibleAmount());
        entry.setDetails(command.getDetails());
        entry.setAccountingTransaction(transaction);
        expenseEntryRepository.save(entry);
        syncExpenseJournalEntry(entry, transaction);
        syncTaxDeductionRecordForExpense(transaction, entry);
        return new EntrySaveResult(command.getKind().getPath(),
                command.getEditId() == null ? "支出を登録しました。" : "支出を更新しました。");
    }

    private void ensureEditable(Long submittedVersion, Long currentVersion, FiscalYear fiscalYear, AccountingTransaction transaction) {
        if (!Objects.equals(submittedVersion, normalizeVersion(currentVersion))) {
            throw new EntryConflictException("別の更新が行われたため、再読み込みしてください。");
        }
        ensureOpenFiscalYear(fiscalYear);
        if (transaction != null && !STATUS_DRAFT.equalsIgnoreCase(transaction.getStatus())) {
            throw new EntryConflictException("確定済みデータは更新できません。");
        }
    }

    private void ensureOpenFiscalYear(FiscalYear fiscalYear) {
        if (fiscalYear == null
                || !STATUS_OPEN.equalsIgnoreCase(fiscalYear.getStatus())
                || fiscalYear.getClosedAt() != null) {
            throw new EntryConflictException("年度が締められているため更新できません。");
        }
    }

    private void validateLockState(FiscalYear fiscalYear, AccountingTransaction transaction, EntryPageData pageData) {
        if (!STATUS_OPEN.equalsIgnoreCase(fiscalYear.getStatus()) || fiscalYear.getClosedAt() != null) {
            pageData.setFailureMessage("年度が締められているため更新できません。");
            return;
        }
        if (transaction != null && !STATUS_DRAFT.equalsIgnoreCase(transaction.getStatus())) {
            pageData.setFailureMessage("確定済みデータは更新できません。");
        }
    }

    private FiscalYear lockOwnedFiscalYear(Long fiscalYearId, Long appUserId) {
        FiscalYear fiscalYear = fiscalYearRepository.findOwnedByIdForUpdate(fiscalYearId, appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
        ensureOpenFiscalYear(fiscalYear);
        return fiscalYear;
    }

    private FiscalYear lockOwnedFiscalYearForIncomeEntry(Long entryId, Long appUserId) {
        Long fiscalYearId = incomeEntryRepository.findOwnedFiscalYearId(entryId, appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
        return lockOwnedFiscalYear(fiscalYearId, appUserId);
    }

    private FiscalYear lockOwnedFiscalYearForExpenseEntry(Long entryId, Long appUserId) {
        Long fiscalYearId = expenseEntryRepository.findOwnedFiscalYearId(entryId, appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
        return lockOwnedFiscalYear(fiscalYearId, appUserId);
    }

    private AccountingTransaction lockOwnedAccountingTransaction(AccountingTransaction transaction, Long appUserId) {
        if (transaction == null || transaction.getId() == null) {
            return null;
        }
        return accountingTransactionRepository.findOwnedByIdForUpdate(transaction.getId(), appUserId)
                .orElseThrow(() -> new EntryConflictException("取引データの状態が変更されたため、再読み込みしてください。"));
    }

    private void ensureExpectedFiscalYear(FiscalYear expectedFiscalYear, FiscalYear actualFiscalYear) {
        if (expectedFiscalYear == null
                || actualFiscalYear == null
                || !Objects.equals(expectedFiscalYear.getId(), actualFiscalYear.getId())) {
            throw new EntryConflictException("対象データの年度が変更されたため、再読み込みしてください。");
        }
    }

    private EntryPageData createBasePage(EntryKind kind, MasterData masterData, BigDecimal apportionmentRate) {
        EntryPageData pageData = new EntryPageData();
        pageData.setEntryKind(kind.getCode());
        pageData.setCategories(masterData.categoryOptions);
        pageData.setItems(masterData.itemOptions);
        pageData.setMethods(masterData.methodOptions);
        pageData.setApportionmentRate(apportionmentRate);
        pageData.setToday(LocalDate.now(TOKYO).format(ISO_DATE));
        pageData.setActiveMenu(kind.getActiveMenu());
        return pageData;
    }

    private EntryForm createDefaultForm(EntryKind kind, MasterData masterData) {
        EntryForm form = new EntryForm();
        if (!masterData.categoryOptions.isEmpty()) {
            kind.writeType(form, masterData.categoryOptions.get(0).getId());
        }
        form.setCategory("");
        form.setDate("");
        form.setAmount("");
        kind.writeMethodId(form, masterData.defaultMethodId);
        form.setDetails("");
        form.setEntryMode("normal");
        form.setTargetMonth("");
        form.setMonthlyRows(new ArrayList<>());
        if (kind.isExpense()) {
            appendDefaultMonthlyRows(form);
        }
        return form;
    }

    private EntryForm toIncomeForm(IncomeEntry entry) {
        EntryForm form = new EntryForm();
        form.setEditId(entry.getId());
        form.setVersion(normalizeVersion(entry.getVersion()));
        if (entry.getIncomeItem() != null && entry.getIncomeItem().getIncomeCategory() != null) {
            form.setIncomeType(entry.getIncomeItem().getIncomeCategory().getCategoryCode());
            form.setCategory(String.valueOf(entry.getIncomeItem().getId()));
        } else {
            form.setIncomeType("");
            form.setCategory("");
        }
        form.setDate(formatDate(entry.getTransactionDate()));
        form.setAmount(formatAmount(entry.getAmount()));
        form.setReceiveMethodId(entry.getReceiveMethod() == null ? "" : String.valueOf(entry.getReceiveMethod().getId()));
        form.setPaymentMethodId("");
        form.setExcludeFromReturn(entry.isExcludeFromReturn());
        form.setHomeApportionment(false);
        form.setDetails(entry.getDetails() == null ? "" : entry.getDetails());
        form.setEntryMode("normal");
        form.setTargetMonth("");
        form.setMonthlyRows(new ArrayList<>());
        return form;
    }

    private EntryForm toExpenseForm(ExpenseEntry entry) {
        EntryForm form = new EntryForm();
        form.setEditId(entry.getId());
        form.setVersion(normalizeVersion(entry.getVersion()));
        if (entry.getExpenseItem() != null && entry.getExpenseItem().getExpenseCategory() != null) {
            form.setExpenseType(entry.getExpenseItem().getExpenseCategory().getCategoryCode());
            form.setCategory(String.valueOf(entry.getExpenseItem().getId()));
        } else {
            form.setExpenseType("");
            form.setCategory("");
        }
        form.setDate(formatDate(entry.getTransactionDate()));
        form.setAmount(formatAmount(entry.getAmount()));
        form.setPaymentMethodId(entry.getPaymentMethod() == null ? "" : String.valueOf(entry.getPaymentMethod().getId()));
        form.setReceiveMethodId("");
        form.setExcludeFromReturn(entry.isExcludeFromReturn());
        form.setHomeApportionment(entry.isHomeApportionmentApplied());
        form.setDetails(entry.getDetails() == null ? "" : entry.getDetails());
        form.setEntryMode("normal");
        form.setTargetMonth("");
        form.setMonthlyRows(new ArrayList<>());
        appendDefaultMonthlyRows(form);
        return form;
    }

    private void appendDefaultMonthlyRows(EntryForm form) {
        for (int i = 0; i < DEFAULT_MONTHLY_ROWS; i++) {
            form.getMonthlyRows().add(new MonthlyExpenseRowForm());
        }
    }

    private EntryForm sanitize(EntryForm original) {
        EntryForm sanitized = new EntryForm();
        if (original == null) {
            return sanitized;
        }
        sanitized.setEditId(original.getEditId());
        sanitized.setVersion(original.getVersion());
        sanitized.setIncomeType(trimToEmpty(original.getIncomeType()));
        sanitized.setExpenseType(trimToEmpty(original.getExpenseType()));
        sanitized.setCategory(trimToEmpty(original.getCategory()));
        sanitized.setDate(trimToEmpty(original.getDate()));
        sanitized.setAmount(trimToEmpty(original.getAmount()));
        sanitized.setReceiveMethodId(trimToEmpty(original.getReceiveMethodId()));
        sanitized.setPaymentMethodId(trimToEmpty(original.getPaymentMethodId()));
        sanitized.setExcludeFromReturn(original.isExcludeFromReturn());
        sanitized.setHomeApportionment(original.isHomeApportionment());
        sanitized.setDetails(original.getDetails() == null ? "" : original.getDetails().trim());
        sanitized.setEntryMode(trimToEmpty(original.getEntryMode()));
        sanitized.setTargetMonth(trimToEmpty(original.getTargetMonth()));
        List<MonthlyExpenseRowForm> monthlyRows = new ArrayList<>();
        if (original.getMonthlyRows() != null) {
            for (MonthlyExpenseRowForm row : original.getMonthlyRows()) {
                if (row == null) {
                    continue;
                }
                MonthlyExpenseRowForm sanitizedRow = new MonthlyExpenseRowForm();
                sanitizedRow.setExpenseType(trimToEmpty(row.getExpenseType()));
                sanitizedRow.setCategory(trimToEmpty(row.getCategory()));
                sanitizedRow.setDate(trimToEmpty(row.getDate()));
                sanitizedRow.setAmount(trimToEmpty(row.getAmount()));
                sanitizedRow.setPaymentMethodId(trimToEmpty(row.getPaymentMethodId()));
                sanitizedRow.setExcludeFromReturn(row.isExcludeFromReturn());
                sanitizedRow.setHomeApportionment(row.isHomeApportionment());
                sanitizedRow.setDetails(row.getDetails() == null ? "" : row.getDetails().trim());
                monthlyRows.add(sanitizedRow);
            }
        }
        sanitized.setMonthlyRows(monthlyRows);
        sanitized.setCsrfToken(trimToEmpty(original.getCsrfToken()));
        sanitized.setSubmissionToken(trimToEmpty(original.getSubmissionToken()));
        sanitized.setConfirmationToken(trimToEmpty(original.getConfirmationToken()));
        return sanitized;
    }

    private void validateLengths(EntryKind kind, EntryForm form, Map<String, String> errors) {
        checkMaxLength(form.getCategory(), 30, "category", "選択された項目がマスタに存在しません。", errors);
        checkMaxLength(form.getDate(), 10, "date", kind.isIncome() ? "存在する日付を選択してください。" : "存在する日付を選択してください。", errors);
        checkMaxLength(form.getAmount(), 32, "amount", "金額は数値のみで入力してください。", errors);
        checkMaxLength(kind.readMethodId(form), 30, kind.isIncome() ? "receiveMethodId" : "paymentMethodId",
                kind.isIncome() ? "受取方法を選択してください。" : "支払方法を選択してください。", errors);
        checkMaxLength(kind.readType(form), 30, kind.isIncome() ? "incomeType" : "expenseType",
                "区分を選択してください。", errors);
        checkMaxLength(form.getDetails(), 1024, "details",
                kind.isIncome() ? "収益詳細は1〜255文字で入力してください。" : "支出詳細は1〜255文字で入力してください。", errors);
        checkMaxLength(form.getEntryMode(), 20, "entryMode", "登録モードが不正です。", errors);
        checkMaxLength(form.getTargetMonth(), 7, "targetMonth", "登録対象月の形式が不正です。", errors);
        checkMaxLength(form.getCsrfToken(), 200, "csrfToken", "フォームの送信情報が無効です。画面を開き直してください。", errors);
        checkMaxLength(form.getSubmissionToken(), 200, "submissionToken", "フォームの送信情報が無効です。画面を開き直してください。", errors);
        checkMaxLength(form.getConfirmationToken(), 200, "confirmationToken", "確認トークンが不正です。再度確認してください。", errors);
    }

    private EntryPrepareResult prepareMonthlySubmission(
            Long appUserId,
            EntryForm sanitizedForm,
            EntryPageData pageData,
            MasterData masterData,
            Optional<BigDecimal> currentRate) {
        Map<String, String> fieldErrors = pageData.getFieldErrors();
        if (sanitizedForm.getEditId() != null) {
            fieldErrors.putIfAbsent("entryMode", "月間登録モードでは編集できません。通常モードを利用してください。");
            return new EntryPrepareResult(pageData, null);
        }
        YearMonth targetMonth = parseTargetMonth(sanitizedForm.getTargetMonth(), fieldErrors);
        List<MonthlyExpenseRowForm> rows = sanitizedForm.getMonthlyRows() == null ? List.of() : sanitizedForm.getMonthlyRows();
        if (rows.isEmpty()) {
            fieldErrors.putIfAbsent("monthlyRows", "登録する支出行を1行以上入力してください。");
            return new EntryPrepareResult(pageData, null);
        }
        if (rows.size() > MAX_MONTHLY_ROWS) {
            fieldErrors.putIfAbsent("monthlyRows", "月間登録モードの入力行は最大" + MAX_MONTHLY_ROWS + "件です。");
            return new EntryPrepareResult(pageData, null);
        }

        List<ValidatedEntryCommand> validatedRows = new ArrayList<>();
        boolean confirmationRequired = false;
        int rowNo = 0;
        for (MonthlyExpenseRowForm row : rows) {
            rowNo++;
            if (row == null) {
                fieldErrors.putIfAbsent("monthlyRows[" + (rowNo - 1) + "].row", "行" + rowNo + "の入力が不正です。");
                continue;
            }
            checkMaxLength(row.getExpenseType(), 30, buildRowField(rowNo - 1, "expenseType"), "区分を選択してください。", fieldErrors);
            checkMaxLength(row.getCategory(), 30, buildRowField(rowNo - 1, "category"), "選択された項目がマスタに存在しません。", fieldErrors);
            checkMaxLength(row.getDate(), 10, buildRowField(rowNo - 1, "date"), "存在する日付を選択してください。", fieldErrors);
            checkMaxLength(row.getAmount(), 32, buildRowField(rowNo - 1, "amount"), "金額は数値のみで入力してください。", fieldErrors);
            checkMaxLength(row.getPaymentMethodId(), 30, buildRowField(rowNo - 1, "paymentMethodId"), "支払方法を選択してください。", fieldErrors);
            checkMaxLength(row.getDetails(), 1024, buildRowField(rowNo - 1, "details"), "支出詳細は1〜255文字で入力してください。", fieldErrors);

            String typeCode = trimToEmpty(row.getExpenseType());
            CategorySelection categorySelection = resolveCategory(EntryKind.EXPENSE, masterData, typeCode, fieldErrors, buildRowField(rowNo - 1, "expenseType"));
            ExpenseItem itemSelection = resolveExpenseItem(masterData, categorySelection, row.getCategory(), fieldErrors, buildRowField(rowNo - 1, "category"));
            LocalDate transactionDate = resolveDate(row.getDate(), fieldErrors, EntryKind.EXPENSE, buildRowField(rowNo - 1, "date"));
            BigDecimal amount = resolveAmount(row.getAmount(), fieldErrors, buildRowField(rowNo - 1, "amount"));
            PaymentMethod methodSelection = resolvePaymentMethod(masterData, row.getPaymentMethodId(), fieldErrors, buildRowField(rowNo - 1, "paymentMethodId"));
            String details = resolveDetails(row.getDetails(), fieldErrors, EntryKind.EXPENSE, buildRowField(rowNo - 1, "details"));

            if (transactionDate != null && targetMonth != null && !YearMonth.from(transactionDate).equals(targetMonth)) {
                fieldErrors.putIfAbsent(buildRowField(rowNo - 1, "date"), "支出発生日は対象月内の日付を入力してください。");
            }

            BigDecimal apportionmentRate = ZERO;
            boolean effectiveExcludeFromReturn = row.isExcludeFromReturn();
            if (itemSelection != null && requiresExcludeFromBusinessPl(itemSelection)) {
                effectiveExcludeFromReturn = true;
                if (row.isHomeApportionment()) {
                    fieldErrors.putIfAbsent(buildRowField(rowNo - 1, "homeApportionment"),
                            "資金移動・私的支出項目は確定申告対象外として扱うため、家事按分は適用できません。");
                }
            }
            if (itemSelection != null && isSocialInsuranceExpense(itemSelection) && details != null && !containsSocialInsuranceDetail(details)) {
                fieldErrors.putIfAbsent(buildRowField(rowNo - 1, "details"),
                        "国民健康保険か国民年金かが分かるように支出詳細へ記載してください。");
            }
            if (row.isHomeApportionment()) {
                if (currentRate.isEmpty()
                        || currentRate.get().compareTo(BigDecimal.ZERO) < 0
                        || currentRate.get().compareTo(new BigDecimal("100")) > 0) {
                    fieldErrors.putIfAbsent(buildRowField(rowNo - 1, "homeApportionment"),
                            "設定されている家事按分率が不正です。設定画面で0〜100%の範囲に修正してください。");
                } else {
                    apportionmentRate = currentRate.get();
                }
            }
            BigDecimal deductibleAmount = calculateDeductibleAmount(amount, effectiveExcludeFromReturn, row.isHomeApportionment(), apportionmentRate);

            FiscalYear fiscalYear = null;
            if (transactionDate != null) {
                List<FiscalYear> fiscalYears = fiscalYearRepository.findOpenByAppUserIdAndDate(appUserId, transactionDate);
                if (fiscalYears.isEmpty()) {
                    fieldErrors.putIfAbsent(buildRowField(rowNo - 1, "date"), "取引日が登録可能な年度に含まれていません。");
                } else {
                    fiscalYear = fiscalYears.get(0);
                }
            }

            if (fiscalYear == null || amount == null || transactionDate == null || details == null || itemSelection == null || methodSelection == null || categorySelection == null) {
                continue;
            }

            String payloadHash = hashPayload(
                    EntryKind.EXPENSE,
                    appUserId,
                    null,
                    0L,
                    fiscalYear.getId(),
                    categorySelection.code(),
                    itemSelection.getId(),
                    methodSelection.getId(),
                    transactionDate,
                    amount,
                    effectiveExcludeFromReturn,
                    row.isHomeApportionment(),
                    apportionmentRate,
                    deductibleAmount,
                    details);
            if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
                confirmationRequired = true;
            }
            validatedRows.add(new ValidatedEntryCommand(
                    EntryKind.EXPENSE,
                    appUserId,
                    null,
                    0L,
                    fiscalYear,
                    null,
                    itemSelection,
                    null,
                    methodSelection,
                    transactionDate,
                    amount,
                    effectiveExcludeFromReturn,
                    row.isHomeApportionment(),
                    apportionmentRate,
                    deductibleAmount,
                    details,
                    payloadHash,
                    amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0));
        }

        if (!fieldErrors.isEmpty() || validatedRows.isEmpty()) {
            return new EntryPrepareResult(pageData, null);
        }

        String aggregatePayload = String.join("|",
                "expense-monthly",
                String.valueOf(appUserId),
                trimToEmpty(sanitizedForm.getTargetMonth()),
                String.valueOf(validatedRows.size()),
                validatedRows.stream().map(ValidatedEntryCommand::getPayloadHash).reduce("", (a, b) -> a + ";" + b));
        String payloadHash = hashMonthlyPayload(aggregatePayload);
        ValidatedEntryCommand command = ValidatedEntryCommand.monthlyExpenseBatch(
                appUserId,
                sanitizedForm.getTargetMonth(),
                validatedRows,
                payloadHash,
                confirmationRequired);
        return new EntryPrepareResult(pageData, command);
    }

    private YearMonth parseTargetMonth(String targetMonth, Map<String, String> errors) {
        String value = trimToEmpty(targetMonth);
        if (value.isBlank()) {
            errors.putIfAbsent("targetMonth", "登録対象月を選択してください。");
            return null;
        }
        try {
            return YearMonth.parse(value, TARGET_MONTH);
        } catch (DateTimeParseException ex) {
            errors.putIfAbsent("targetMonth", "登録対象月の形式が不正です。");
            return null;
        }
    }

    private String buildRowField(int rowIndex, String field) {
        return "monthlyRows[" + rowIndex + "]." + field;
    }

    private String hashMonthlyPayload(String payload) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private void checkMaxLength(String value, int maxLength, String fieldName, String message, Map<String, String> errors) {
        if (value != null && value.length() > maxLength) {
            errors.putIfAbsent(fieldName, message);
        }
    }

    private CategorySelection resolveCategory(
            EntryKind kind,
            MasterData masterData,
            String typeCode,
            Map<String, String> errors) {
        return resolveCategory(kind, masterData, typeCode, errors, kind.isIncome() ? "incomeType" : "expenseType");
    }

    private CategorySelection resolveCategory(
            EntryKind kind,
            MasterData masterData,
            String typeCode,
            Map<String, String> errors,
            String fieldName) {
        if (typeCode.isBlank()) {
            errors.putIfAbsent(fieldName, "区分を選択してください。");
            return null;
        }
        CategorySelection selection = masterData.categoriesByCode.get(typeCode);
        if (selection == null) {
            errors.putIfAbsent(fieldName, "区分を選択してください。");
        }
        return selection;
    }

    private Object resolveItem(
            EntryKind kind,
            MasterData masterData,
            CategorySelection categorySelection,
            String rawItemId,
            Map<String, String> errors) {
        return resolveItem(kind, masterData, categorySelection, rawItemId, errors, "category");
    }

    private Object resolveItem(
            EntryKind kind,
            MasterData masterData,
            CategorySelection categorySelection,
            String rawItemId,
            Map<String, String> errors,
            String fieldName) {
        String trimmed = trimToEmpty(rawItemId);
        if (trimmed.isBlank()) {
            errors.putIfAbsent(fieldName, "項目を選択してください。");
            return null;
        }
        Long itemId = parseLong(trimmed);
        if (itemId == null) {
            errors.putIfAbsent(fieldName, "選択された項目がマスタに存在しません。");
            return null;
        }
        if (kind.isIncome()) {
            IncomeItem item = masterData.incomeItemsById.get(itemId);
            if (item == null || categorySelection == null
                    || !categorySelection.code().equals(item.getIncomeCategory().getCategoryCode())
                    || !item.isActive()) {
                errors.putIfAbsent(fieldName, "選択された項目がマスタに存在しません。");
                return null;
            }
            return item;
        }
        ExpenseItem item = masterData.expenseItemsById.get(itemId);
        if (item == null || categorySelection == null
                || !categorySelection.code().equals(item.getExpenseCategory().getCategoryCode())
                || !item.isActive()) {
            errors.putIfAbsent(fieldName, "選択された項目がマスタに存在しません。");
            return null;
        }
        return item;
    }

    private ExpenseItem resolveExpenseItem(
            MasterData masterData,
            CategorySelection categorySelection,
            String rawItemId,
            Map<String, String> errors,
            String fieldName) {
        Object item = resolveItem(EntryKind.EXPENSE, masterData, categorySelection, rawItemId, errors, fieldName);
        return item instanceof ExpenseItem expenseItem ? expenseItem : null;
    }

    private Object resolveMethod(EntryKind kind, MasterData masterData, String rawMethodId, Map<String, String> errors) {
        return resolveMethod(kind, masterData, rawMethodId, errors, kind.isIncome() ? "receiveMethodId" : "paymentMethodId");
    }

    private Object resolveMethod(
            EntryKind kind,
            MasterData masterData,
            String rawMethodId,
            Map<String, String> errors,
            String field) {
        String trimmed = trimToEmpty(rawMethodId);
        String requiredMessage = kind.isIncome() ? "受取方法を選択してください。" : "支払方法を選択してください。";
        if (trimmed.isBlank()) {
            errors.putIfAbsent(field, requiredMessage);
            return null;
        }
        Long methodId = parseLong(trimmed);
        if (methodId == null) {
            errors.putIfAbsent(field, requiredMessage);
            return null;
        }
        if (kind.isIncome()) {
            ReceiveMethod method = masterData.receiveMethodsById.get(methodId);
            if (method == null || !method.isActive()) {
                errors.putIfAbsent(field, requiredMessage);
                return null;
            }
            return method;
        }
        PaymentMethod method = masterData.paymentMethodsById.get(methodId);
        if (method == null || !method.isActive()) {
            errors.putIfAbsent(field, requiredMessage);
            return null;
        }
        return method;
    }

    private PaymentMethod resolvePaymentMethod(
            MasterData masterData,
            String rawMethodId,
            Map<String, String> errors,
            String fieldName) {
        Object method = resolveMethod(EntryKind.EXPENSE, masterData, rawMethodId, errors, fieldName);
        return method instanceof PaymentMethod paymentMethod ? paymentMethod : null;
    }

    private LocalDate resolveDate(String rawDate, Map<String, String> errors, EntryKind kind) {
        return resolveDate(rawDate, errors, kind, "date");
    }

    private LocalDate resolveDate(String rawDate, Map<String, String> errors, EntryKind kind, String fieldName) {
        String label = kind.isIncome() ? "収益発生日を選択してください。" : "支出発生日を選択してください。";
        String value = trimToEmpty(rawDate);
        if (value.isBlank()) {
            errors.putIfAbsent(fieldName, label);
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(value, ISO_DATE);
            if (parsed.isAfter(LocalDate.now(TOKYO))) {
                errors.putIfAbsent(fieldName, "登録日より未来の日付は選択できません。");
                return null;
            }
            return parsed;
        } catch (DateTimeParseException ex) {
            errors.putIfAbsent(fieldName, "存在する日付を選択してください。");
            return null;
        }
    }

    private BigDecimal resolveAmount(String rawAmount, Map<String, String> errors) {
        return resolveAmount(rawAmount, errors, "amount");
    }

    private BigDecimal resolveAmount(String rawAmount, Map<String, String> errors, String fieldName) {
        String value = trimToEmpty(rawAmount);
        if (value.isBlank()) {
            errors.putIfAbsent(fieldName, "金額を入力してください。");
            return null;
        }
        if (!AMOUNT_PATTERN.matcher(value).matches()) {
            errors.putIfAbsent(fieldName, "金額は数値のみで入力してください。");
            return null;
        }
        BigDecimal parsed = new BigDecimal(value);
        if (parsed.compareTo(BigDecimal.ONE) < 0) {
            errors.putIfAbsent(fieldName, "金額は1円以上で入力してください。");
            return null;
        }
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            errors.putIfAbsent(fieldName, "金額は9999999999999円以下で入力してください。");
            return null;
        }
        return parsed.setScale(2);
    }

    private String resolveDetails(String rawDetails, Map<String, String> errors, EntryKind kind) {
        return resolveDetails(rawDetails, errors, kind, "details");
    }

    private String resolveDetails(String rawDetails, Map<String, String> errors, EntryKind kind, String fieldName) {
        String value = rawDetails == null ? "" : rawDetails.trim();
        String required = kind.isIncome() ? "収益詳細を入力してください。" : "支出詳細を入力してください。";
        String lengthMessage = kind.isIncome() ? "収益詳細は1〜255文字で入力してください。" : "支出詳細は1〜255文字で入力してください。";
        String forbiddenMessage = kind.isIncome() ? "収益詳細に使用できない文字が含まれています。" : "支出詳細に使用できない文字が含まれています。";
        if (value.isEmpty()) {
            errors.putIfAbsent(fieldName, required);
            return null;
        }
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints < 1 || codePoints > 255) {
            errors.putIfAbsent(fieldName, lengthMessage);
            return null;
        }
        if (FORBIDDEN_DETAILS.matcher(value).find()) {
            errors.putIfAbsent(fieldName, forbiddenMessage);
            return null;
        }
        return value;
    }

    private BigDecimal calculateDeductibleAmount(
            BigDecimal amount,
            boolean excludeFromReturn,
            boolean homeApportionment,
            BigDecimal rate) {
        if (amount == null) {
            return ZERO.setScale(2);
        }
        if (excludeFromReturn) {
            return ZERO.setScale(2);
        }
        if (!homeApportionment) {
            return amount.setScale(2);
        }
        return amount.multiply(rate)
                .divide(new BigDecimal("100"), 0, RoundingMode.DOWN)
                .setScale(2);
    }

    private BusinessProfile ensureCatalogs(Long appUserId) {
        BusinessProfile profile = businessProfileRepository.findByAppUserIdForUpdate(appUserId)
                .orElseThrow(() -> new EntryNotFoundException("Business profile not found"));
        ensureReceiveMethods();
        ensurePaymentMethods();
        ensureIncomeCatalogs(profile);
        ensureExpenseCatalogs(profile);
        return profile;
    }

    private void ensureIncomeCatalogs(BusinessProfile profile) {
        List<IncomeCategory> existingCategories = incomeCategoryRepository.findByBusinessProfileIdOrderByIdAsc(profile.getId());
        Map<String, IncomeCategory> byCode = new LinkedHashMap<>();
        existingCategories.forEach(category -> byCode.put(category.getCategoryCode(), category));
        for (CatalogSeed seed : INCOME_CATEGORY_SEEDS) {
            IncomeCategory category = byCode.get(seed.code());
            if (category == null) {
                category = new IncomeCategory();
                category.setBusinessProfile(profile);
                category.setCategoryCode(seed.code());
                category.setCategoryName(seed.name());
                category.setActive(true);
                category = incomeCategoryRepository.save(category);
                byCode.put(seed.code(), category);
            }
        }
        List<IncomeItem> existingItems = incomeItemRepository.findByIncomeCategoryBusinessProfileIdOrderByIdAsc(profile.getId());
        Map<String, IncomeItem> itemByComposite = new LinkedHashMap<>();
        existingItems.forEach(item -> itemByComposite.put(item.getIncomeCategory().getCategoryCode() + ":" + item.getItemCode(), item));
        for (CatalogSeed seed : INCOME_CATEGORY_SEEDS) {
            IncomeCategory category = byCode.get(seed.code());
            for (CatalogItemSeed itemSeed : seed.items()) {
                String key = seed.code() + ":" + itemSeed.code();
                if (itemByComposite.containsKey(key)) {
                    continue;
                }
                IncomeItem item = new IncomeItem();
                item.setIncomeCategory(category);
                item.setItemCode(itemSeed.code());
                item.setItemName(itemSeed.name());
                item.setActive(true);
                incomeItemRepository.save(item);
            }
        }
    }

    private void ensureExpenseCatalogs(BusinessProfile profile) {
        List<ExpenseCategory> existingCategories = expenseCategoryRepository.findByBusinessProfileIdOrderByIdAsc(profile.getId());
        Map<String, ExpenseCategory> byCode = new LinkedHashMap<>();
        existingCategories.forEach(category -> byCode.put(category.getCategoryCode(), category));
        for (CatalogSeed seed : EXPENSE_CATEGORY_SEEDS) {
            ExpenseCategory category = byCode.get(seed.code());
            if (category == null) {
                category = new ExpenseCategory();
                category.setBusinessProfile(profile);
                category.setCategoryCode(seed.code());
                category.setCategoryName(seed.name());
                category.setActive(true);
                category = expenseCategoryRepository.save(category);
                byCode.put(seed.code(), category);
            }
        }
        List<ExpenseItem> existingItems = expenseItemRepository.findByExpenseCategoryBusinessProfileIdOrderByIdAsc(profile.getId());
        Map<String, ExpenseItem> itemByComposite = new LinkedHashMap<>();
        existingItems.forEach(item -> itemByComposite.put(item.getExpenseCategory().getCategoryCode() + ":" + item.getItemCode(), item));
        for (CatalogSeed seed : EXPENSE_CATEGORY_SEEDS) {
            ExpenseCategory category = byCode.get(seed.code());
            for (CatalogItemSeed itemSeed : seed.items()) {
                String key = seed.code() + ":" + itemSeed.code();
                if (itemByComposite.containsKey(key)) {
                    continue;
                }
                ExpenseItem item = new ExpenseItem();
                item.setExpenseCategory(category);
                item.setItemCode(itemSeed.code());
                item.setItemName(itemSeed.name());
                item.setActive(true);
                expenseItemRepository.save(item);
            }
        }
    }

    private void ensureReceiveMethods() {
        Map<String, ReceiveMethod> methodsByCode = new LinkedHashMap<>();
        receiveMethodRepository.findAllByOrderByIdAsc().forEach(method -> methodsByCode.put(method.getMethodCode(), method));
        for (MethodSeed seed : METHOD_SEEDS) {
            if (methodsByCode.containsKey(seed.code())) {
                continue;
            }
            ReceiveMethod method = new ReceiveMethod();
            method.setMethodCode(seed.code());
            method.setMethodName(seed.name());
            method.setActive(true);
            receiveMethodRepository.save(method);
        }
    }

    private void ensurePaymentMethods() {
        Map<String, PaymentMethod> methodsByCode = new LinkedHashMap<>();
        paymentMethodRepository.findAllByOrderByIdAsc().forEach(method -> methodsByCode.put(method.getMethodCode(), method));
        for (MethodSeed seed : METHOD_SEEDS) {
            if (methodsByCode.containsKey(seed.code())) {
                continue;
            }
            PaymentMethod method = new PaymentMethod();
            method.setMethodCode(seed.code());
            method.setMethodName(seed.name());
            method.setActive(true);
            paymentMethodRepository.save(method);
        }
    }

    private MasterData loadMasterData(EntryKind kind, Long businessProfileId) {
        MasterData data = new MasterData();
        if (kind.isIncome()) {
            List<IncomeCategory> categories = incomeCategoryRepository.findByBusinessProfileIdOrderByIdAsc(businessProfileId).stream()
                    .filter(IncomeCategory::isActive)
                    .sorted(Comparator.comparing(IncomeCategory::getId))
                    .toList();
            List<IncomeItem> items = incomeItemRepository.findByIncomeCategoryBusinessProfileIdOrderByIdAsc(businessProfileId).stream()
                    .filter(IncomeItem::isActive)
                    .sorted(Comparator.comparing(IncomeItem::getId))
                    .toList();
            List<ReceiveMethod> methods = receiveMethodRepository.findAllByOrderByIdAsc().stream()
                    .filter(ReceiveMethod::isActive)
                    .toList();
            for (IncomeCategory category : categories) {
                data.categoriesByCode.put(category.getCategoryCode(),
                        new CategorySelection(category.getCategoryCode(), category.getCategoryName()));
                data.categoryOptions.add(new EntryOption(category.getCategoryCode(), category.getCategoryName(), null));
            }
            for (IncomeItem item : items) {
                data.incomeItemsById.put(item.getId(), item);
                data.itemOptions.add(new EntryOption(
                        String.valueOf(item.getId()),
                        item.getItemName(),
                        item.getIncomeCategory().getCategoryCode()));
            }
            for (ReceiveMethod method : methods) {
                data.receiveMethodsById.put(method.getId(), method);
                data.methodOptions.add(new EntryOption(String.valueOf(method.getId()), method.getMethodName(), null));
                if ("cash".equals(method.getMethodCode())) {
                    data.defaultMethodId = String.valueOf(method.getId());
                }
            }
        } else {
            List<ExpenseCategory> categories = expenseCategoryRepository.findByBusinessProfileIdOrderByIdAsc(businessProfileId).stream()
                    .filter(ExpenseCategory::isActive)
                    .sorted(Comparator.comparing(ExpenseCategory::getId))
                    .toList();
            List<ExpenseItem> items = expenseItemRepository.findByExpenseCategoryBusinessProfileIdOrderByIdAsc(businessProfileId).stream()
                    .filter(ExpenseItem::isActive)
                    .sorted(Comparator.comparing(ExpenseItem::getId))
                    .toList();
            List<PaymentMethod> methods = paymentMethodRepository.findAllByOrderByIdAsc().stream()
                    .filter(PaymentMethod::isActive)
                    .toList();
            for (ExpenseCategory category : categories) {
                data.categoriesByCode.put(category.getCategoryCode(),
                        new CategorySelection(category.getCategoryCode(), category.getCategoryName()));
                data.categoryOptions.add(new EntryOption(category.getCategoryCode(), category.getCategoryName(), null));
            }
            for (ExpenseItem item : items) {
                data.expenseItemsById.put(item.getId(), item);
                data.itemOptions.add(new EntryOption(
                        String.valueOf(item.getId()),
                        item.getItemName(),
                        item.getExpenseCategory().getCategoryCode()));
            }
            for (PaymentMethod method : methods) {
                data.paymentMethodsById.put(method.getId(), method);
                data.methodOptions.add(new EntryOption(String.valueOf(method.getId()), method.getMethodName(), null));
                if ("cash".equals(method.getMethodCode())) {
                    data.defaultMethodId = String.valueOf(method.getId());
                }
            }
        }
        if (data.defaultMethodId.isBlank() && !data.methodOptions.isEmpty()) {
            data.defaultMethodId = data.methodOptions.get(0).getId();
        }
        return data;
    }

    private Optional<BigDecimal> loadCurrentApportionmentRate(Long appUserId) {
        return userSettingRepository.findByAppUserId(appUserId).map(UserSetting::getHomeApportionmentRate);
    }

    private boolean isWithinFiscalYear(LocalDate transactionDate, FiscalYear fiscalYear) {
        return transactionDate != null
                && fiscalYear != null
                && !transactionDate.isBefore(fiscalYear.getPeriodStart())
                && !transactionDate.isAfter(fiscalYear.getPeriodEnd());
    }

    private String hashPayload(
            EntryKind kind,
            Long appUserId,
            Long editId,
            Long version,
            Long fiscalYearId,
            String typeCode,
            Long itemId,
            Long methodId,
            LocalDate transactionDate,
            BigDecimal amount,
            boolean excludeFromReturn,
            boolean homeApportionment,
            BigDecimal apportionmentRate,
            BigDecimal deductibleAmount,
            String details) {
        String payload = String.join("|",
                kind.getCode(),
                String.valueOf(appUserId),
                String.valueOf(editId),
                String.valueOf(version),
                String.valueOf(fiscalYearId),
                typeCode,
                String.valueOf(itemId),
                String.valueOf(methodId),
                formatDate(transactionDate),
                formatAmount(amount),
                String.valueOf(excludeFromReturn),
                String.valueOf(homeApportionment),
                formatAmount(apportionmentRate),
                formatAmount(deductibleAmount),
                details);
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String formatFiscalYear(FiscalYear fiscalYear) {
        if (fiscalYear == null) {
            return null;
        }
        return fiscalYear.getTaxYear() + "年 (" + formatDate(fiscalYear.getPeriodStart()) + "〜" + formatDate(fiscalYear.getPeriodEnd()) + ")";
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(ISO_DATE);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private Long normalizeVersion(Long version) {
        return version == null ? 0L : version;
    }

    private Long parseLong(String rawValue) {
        try {
            return Long.valueOf(rawValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean requiresExcludeFromBusinessPl(IncomeItem incomeItem) {
        if (incomeItem == null || incomeItem.getIncomeCategory() == null) {
            return false;
        }
        return CATEGORY_OTHER.equals(incomeItem.getIncomeCategory().getCategoryCode());
    }

    private boolean requiresExcludeFromBusinessPl(ExpenseItem expenseItem) {
        if (expenseItem == null || expenseItem.getExpenseCategory() == null) {
            return false;
        }
        return CATEGORY_OTHER.equals(expenseItem.getExpenseCategory().getCategoryCode());
    }

    private boolean isSocialInsuranceExpense(ExpenseItem expenseItem) {
        return expenseItem != null && EXPENSE_OTHER_SOCIAL_INSURANCE.equals(expenseItem.getItemCode());
    }

    private boolean containsSocialInsuranceDetail(String details) {
        for (String keyword : SOCIAL_INSURANCE_DETAIL_KEYWORDS) {
            if (details.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String resolveIncomeTransactionType(IncomeItem incomeItem) {
        if (incomeItem == null || incomeItem.getIncomeCategory() == null) {
            return TX_TYPE_INCOME;
        }
        if (!CATEGORY_OTHER.equals(incomeItem.getIncomeCategory().getCategoryCode())) {
            return TX_TYPE_INCOME;
        }
        if (INCOME_OTHER_ERRONEOUS_DEPOSIT_ADJUSTMENT.equals(incomeItem.getItemCode())) {
            return TX_TYPE_ADJUSTMENT;
        }
        return TX_TYPE_TRANSFER;
    }

    private String resolveExpenseTransactionType(ExpenseItem expenseItem) {
        if (expenseItem == null || expenseItem.getExpenseCategory() == null) {
            return TX_TYPE_OUTGO;
        }
        if (!CATEGORY_OTHER.equals(expenseItem.getExpenseCategory().getCategoryCode())) {
            return TX_TYPE_OUTGO;
        }
        if (EXPENSE_OTHER_ERRONEOUS_PAYMENT_REFUND.equals(expenseItem.getItemCode())) {
            return TX_TYPE_ADJUSTMENT;
        }
        return TX_TYPE_TRANSFER;
    }

    private String resolveIncomeSettlementStatus(ReceiveMethod receiveMethod) {
        return SETTLEMENT_STATUS_SETTLED;
    }

    private String resolveExpenseSettlementStatus(PaymentMethod paymentMethod) {
        if (paymentMethod == null || paymentMethod.getMethodCode() == null) {
            return SETTLEMENT_STATUS_SETTLED;
        }
        if ("card".equals(paymentMethod.getMethodCode())) {
            return SETTLEMENT_STATUS_UNSETTLED;
        }
        return SETTLEMENT_STATUS_SETTLED;
    }

    private void syncIncomeJournalEntry(IncomeEntry entry, AccountingTransaction transaction) {
        JournalEntry journalEntry = journalEntryRepository.findByAccountingTransactionId(transaction.getId())
                .orElseGet(JournalEntry::new);
        journalEntry.setFiscalYear(entry.getFiscalYear());
        journalEntry.setAccountingTransaction(transaction);
        journalEntry.setSourceType(SOURCE_TYPE_ACCOUNTING_TRANSACTION);
        journalEntry.setSourceId(transaction.getId());
        journalEntry.setEntryDate(entry.getTransactionDate());
        journalEntry.setSummary(entry.getDetails());
        journalEntry.setStatus(STATUS_DRAFT);

        BigDecimal amount = safeAmount(entry.getAmount());
        String debitAccount = resolveIncomeDebitAccount(entry.getReceiveMethod());
        String creditAccount = resolveIncomeCreditAccount(entry);
        if (entry.isExcludeFromReturn() && !CATEGORY_OTHER.equals(resolveIncomeCategoryCode(entry))) {
            creditAccount = "事業主借";
        }

        List<JournalLine> lines = new ArrayList<>();
        lines.add(buildJournalLine(1, "D", debitAccount, amount, entry.getDetails()));
        lines.add(buildJournalLine(2, "C", creditAccount, amount, entry.getDetails()));
        journalEntry.setLines(lines);
        journalEntryRepository.save(journalEntry);
    }

    private void syncExpenseJournalEntry(ExpenseEntry entry, AccountingTransaction transaction) {
        JournalEntry journalEntry = journalEntryRepository.findByAccountingTransactionId(transaction.getId())
                .orElseGet(JournalEntry::new);
        journalEntry.setFiscalYear(entry.getFiscalYear());
        journalEntry.setAccountingTransaction(transaction);
        journalEntry.setSourceType(SOURCE_TYPE_ACCOUNTING_TRANSACTION);
        journalEntry.setSourceId(transaction.getId());
        journalEntry.setEntryDate(entry.getTransactionDate());
        journalEntry.setSummary(entry.getDetails());
        journalEntry.setStatus(STATUS_DRAFT);

        BigDecimal amount = safeAmount(entry.getAmount());
        BigDecimal deductibleAmount = safeAmount(entry.getDeductibleAmount());
        BigDecimal ownerDrawAmount = amount.subtract(deductibleAmount);
        if (ownerDrawAmount.signum() < 0) {
            ownerDrawAmount = ZERO.setScale(2);
        }

        String debitAccount = resolveExpenseDebitAccount(entry);
        String creditAccount = resolveExpenseCreditAccount(entry.getPaymentMethod());

        List<JournalLine> lines = new ArrayList<>();
        int lineNo = 1;
        if (entry.isExcludeFromReturn()) {
            lines.add(buildJournalLine(lineNo++, "D", "事業主貸", amount, entry.getDetails()));
        } else if (entry.isHomeApportionmentApplied() && ownerDrawAmount.signum() > 0) {
            lines.add(buildJournalLine(lineNo++, "D", debitAccount, deductibleAmount, entry.getDetails()));
            lines.add(buildJournalLine(lineNo++, "D", "事業主貸", ownerDrawAmount, "家事按分の私用部分"));
        } else {
            lines.add(buildJournalLine(lineNo++, "D", debitAccount, amount, entry.getDetails()));
        }
        lines.add(buildJournalLine(lineNo, "C", creditAccount, amount, entry.getDetails()));
        journalEntry.setLines(lines);
        journalEntryRepository.save(journalEntry);
    }

    private JournalLine buildJournalLine(int lineNo, String debitCredit, String accountName, BigDecimal amount, String memo) {
        JournalLine line = new JournalLine();
        line.setLineNo(lineNo);
        line.setDebitCredit(debitCredit);
        line.setAccountName(accountName);
        line.setAmount(safeAmount(amount));
        line.setMemo(trimToEmpty(memo));
        return line;
    }

    private void syncTaxDeductionRecordForExpense(AccountingTransaction transaction, ExpenseEntry entry) {
        if (transaction == null || transaction.getId() == null) {
            return;
        }
        Optional<TaxDeductionRecord> existing = taxDeductionRecordRepository.findBySourceTransactionId(transaction.getId());
        if (isSocialInsuranceExpense(entry.getExpenseItem())) {
            TaxDeductionRecord record = existing.orElseGet(TaxDeductionRecord::new);
            record.setFiscalYear(entry.getFiscalYear());
            record.setSourceTransaction(transaction);
            record.setDeductionType(resolveSocialInsuranceDeductionType(entry.getDetails()));
            record.setPaidDate(entry.getTransactionDate());
            record.setPayer("本人");
            record.setInsuredPersonOrBeneficiary("本人");
            record.setPaidAmount(safeAmount(entry.getAmount()));
            record.setEligibleAmount(null);
            record.setCalculatedDeductionAmount(null);
            record.setStatus(DEDUCTION_STATUS_CANDIDATE);
            record.setNotes(trimToEmpty(entry.getDetails()));
            taxDeductionRecordRepository.save(record);
            return;
        }
        if (existing.isPresent()) {
            TaxDeductionRecord record = existing.get();
            record.setStatus(DEDUCTION_STATUS_INELIGIBLE);
            record.setNotes("取引分類が変更されたため控除候補から除外");
            taxDeductionRecordRepository.save(record);
        }
    }

    private void syncTaxDeductionRecordForIncome(AccountingTransaction transaction, IncomeEntry entry) {
        if (transaction == null || transaction.getId() == null) {
            return;
        }
        Optional<TaxDeductionRecord> existing = taxDeductionRecordRepository.findBySourceTransactionId(transaction.getId());
        if (existing.isPresent()) {
            TaxDeductionRecord record = existing.get();
            record.setStatus(DEDUCTION_STATUS_INELIGIBLE);
            record.setNotes("収益取引のため控除候補から除外");
            taxDeductionRecordRepository.save(record);
        }
    }

    private String resolveIncomeDebitAccount(ReceiveMethod method) {
        if (method == null || method.getMethodCode() == null) {
            return "普通預金";
        }
        return switch (method.getMethodCode()) {
            case "cash" -> "現金";
            case "bank" -> "普通預金";
            case "card", "electronic" -> "未収入金";
            default -> "普通預金";
        };
    }

    private String resolveIncomeCreditAccount(IncomeEntry entry) {
        if (entry == null || entry.getIncomeItem() == null || entry.getIncomeItem().getIncomeCategory() == null) {
            return "売上高";
        }
        String categoryCode = entry.getIncomeItem().getIncomeCategory().getCategoryCode();
        if ("sales".equals(categoryCode)) {
            return "売上高";
        }
        if ("misc".equals(categoryCode)) {
            return "雑収入";
        }
        return switch (entry.getIncomeItem().getItemCode()) {
            case "owner-loan" -> "事業主借";
            case "loan-proceeds" -> "借入金";
            case "deposit-received" -> "預り金";
            case "reimbursement-collected" -> "立替金";
            case "erroneous-deposit-adjustment" -> "仮受金";
            default -> "諸口";
        };
    }

    private String resolveExpenseDebitAccount(ExpenseEntry entry) {
        if (entry == null || entry.getExpenseItem() == null || entry.getExpenseItem().getExpenseCategory() == null) {
            return "支払手数料";
        }
        String categoryCode = entry.getExpenseItem().getExpenseCategory().getCategoryCode();
        if ("expense".equals(categoryCode)) {
            return entry.getExpenseItem().getItemName();
        }
        if ("purchase".equals(categoryCode)) {
            return "仕入高";
        }
        return switch (entry.getExpenseItem().getItemCode()) {
            case "living-expenses", "entertainment-expenses", "income-resident-tax", "national-health-pension" -> "事業主貸";
            case "loan-principal-repayment" -> "借入金";
            case "fixed-asset-purchase" -> "工具器具備品";
            case "prepaid-expense" -> "前払費用";
            case "security-deposit" -> "敷金・保証金";
            case "reimbursement-paid" -> "立替金";
            case "deposit-paid" -> "預り金";
            case "erroneous-payment-refund" -> "仮払金";
            default -> "諸口";
        };
    }

    private String resolveExpenseCreditAccount(PaymentMethod method) {
        if (method == null || method.getMethodCode() == null) {
            return "普通預金";
        }
        return switch (method.getMethodCode()) {
            case "cash" -> "現金";
            case "card" -> "未払金";
            case "bank", "electronic" -> "普通預金";
            default -> "普通預金";
        };
    }

    private String resolveIncomeCategoryCode(IncomeEntry entry) {
        if (entry == null || entry.getIncomeItem() == null || entry.getIncomeItem().getIncomeCategory() == null) {
            return "";
        }
        return trimToEmpty(entry.getIncomeItem().getIncomeCategory().getCategoryCode());
    }

    private String resolveSocialInsuranceDeductionType(String details) {
        String normalized = trimToEmpty(details);
        if (normalized.contains("国民健康保険")) {
            return DEDUCTION_TYPE_SOCIAL_INSURANCE_NHI;
        }
        if (normalized.contains("国民年金")) {
            return DEDUCTION_TYPE_SOCIAL_INSURANCE_PENSION;
        }
        return DEDUCTION_TYPE_SOCIAL_INSURANCE;
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        if (amount == null) {
            return ZERO.setScale(2);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class MasterData {
        private final Map<String, CategorySelection> categoriesByCode = new LinkedHashMap<>();
        private final Map<Long, IncomeItem> incomeItemsById = new LinkedHashMap<>();
        private final Map<Long, ExpenseItem> expenseItemsById = new LinkedHashMap<>();
        private final Map<Long, ReceiveMethod> receiveMethodsById = new LinkedHashMap<>();
        private final Map<Long, PaymentMethod> paymentMethodsById = new LinkedHashMap<>();
        private final List<EntryOption> categoryOptions = new ArrayList<>();
        private final List<EntryOption> itemOptions = new ArrayList<>();
        private final List<EntryOption> methodOptions = new ArrayList<>();
        private String defaultMethodId = "";
    }

    private record CategorySelection(String code, String label) {
    }

    private record CatalogSeed(String code, String name, List<CatalogItemSeed> items) {
    }

    private record CatalogItemSeed(String code, String name) {
    }

    private record MethodSeed(String code, String name) {
    }
}
