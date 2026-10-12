package org.example.features.entry.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
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
import org.example.entity.PaymentMethod;
import org.example.entity.ReceiveMethod;
import org.example.entity.UserSetting;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryKind;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.entry.EntryOption;
import org.example.features.entry.EntryPageData;
import org.example.features.entry.form.EntryForm;
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
import org.example.repository.PaymentMethodRepository;
import org.example.repository.ReceiveMethodRepository;
import org.example.repository.UserSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntryServiceImpl implements EntryService {
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999");
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("100000000");
    private static final BigDecimal ZERO = new BigDecimal("0");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[0-9]{1,13}$");
    private static final Pattern FORBIDDEN_DETAILS = Pattern.compile("[<>\"'`\\\\]");
    private static final String STATUS_OPEN = "open";
    private static final String STATUS_DRAFT = "draft";
    private static final List<CatalogSeed> INCOME_CATEGORY_SEEDS = List.of(
            new CatalogSeed("sales", "売上", List.of(
                    new CatalogItemSeed("product", "商品売上"),
                    new CatalogItemSeed("service", "サービス売上"))),
            new CatalogSeed("misc", "雑収入", List.of(
                    new CatalogItemSeed("interest", "受取利息"),
                    new CatalogItemSeed("commission", "手数料収入"))),
            new CatalogSeed("other", "その他", List.of(
                    new CatalogItemSeed("other-income", "その他収益"))));
    private static final List<CatalogSeed> EXPENSE_CATEGORY_SEEDS = List.of(
            new CatalogSeed("expense", "経費", List.of(
                    new CatalogItemSeed("supplies", "消耗品費"),
                    new CatalogItemSeed("communication", "通信費"),
                    new CatalogItemSeed("travel", "旅費交通費"),
                    new CatalogItemSeed("utilities", "水道光熱費"))),
            new CatalogSeed("purchase", "仕入", List.of(
                    new CatalogItemSeed("goods", "商品仕入"),
                    new CatalogItemSeed("materials", "材料仕入"))),
            new CatalogSeed("other", "その他", List.of(
                    new CatalogItemSeed("other-expense", "その他支出"))));
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
            PaymentMethodRepository paymentMethodRepository) {
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

        validateLengths(kind, sanitizedForm, fieldErrors);

        String typeCode = trimToEmpty(kind.readType(sanitizedForm));
        CategorySelection categorySelection = resolveCategory(kind, masterData, typeCode, fieldErrors);
        Object itemSelection = resolveItem(kind, masterData, categorySelection, sanitizedForm.getCategory(), fieldErrors);
        LocalDate transactionDate = resolveDate(sanitizedForm.getDate(), fieldErrors, kind);
        BigDecimal amount = resolveAmount(sanitizedForm.getAmount(), fieldErrors);
        Object methodSelection = resolveMethod(kind, masterData, kind.readMethodId(sanitizedForm), fieldErrors);
        String details = resolveDetails(sanitizedForm.getDetails(), fieldErrors, kind);

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
            deductibleAmount = calculateDeductibleAmount(amount, sanitizedForm.isExcludeFromReturn(),
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
                sanitizedForm.isExcludeFromReturn(),
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
                sanitizedForm.isExcludeFromReturn(),
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
        if (command.getKind().isIncome()) {
            return saveIncome(command);
        }
        return saveExpense(command);
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
        transaction.setTransactionType("income");
        transaction.setSummary(command.getDetails());
        transaction.setTotalAmount(command.getAmount());
        accountingTransactionRepository.save(transaction);

        entry.setIncomeItem(command.getIncomeItem());
        entry.setReceiveMethod(command.getReceiveMethod());
        entry.setTransactionDate(command.getTransactionDate());
        entry.setAmount(command.getAmount());
        entry.setExcludeFromReturn(command.isExcludeFromReturn());
        entry.setDetails(command.getDetails());
        entry.setAccountingTransaction(transaction);
        incomeEntryRepository.save(entry);
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
        transaction.setTransactionType("expense");
        transaction.setSummary(command.getDetails());
        transaction.setTotalAmount(command.getAmount());
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
        return form;
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
        checkMaxLength(form.getCsrfToken(), 200, "csrfToken", "フォームの送信情報が無効です。画面を開き直してください。", errors);
        checkMaxLength(form.getSubmissionToken(), 200, "submissionToken", "フォームの送信情報が無効です。画面を開き直してください。", errors);
        checkMaxLength(form.getConfirmationToken(), 200, "confirmationToken", "確認トークンが不正です。再度確認してください。", errors);
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
        String fieldName = kind.isIncome() ? "incomeType" : "expenseType";
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
        String trimmed = trimToEmpty(rawItemId);
        if (trimmed.isBlank()) {
            errors.putIfAbsent("category", "項目を選択してください。");
            return null;
        }
        Long itemId = parseLong(trimmed);
        if (itemId == null) {
            errors.putIfAbsent("category", "選択された項目がマスタに存在しません。");
            return null;
        }
        if (kind.isIncome()) {
            IncomeItem item = masterData.incomeItemsById.get(itemId);
            if (item == null || categorySelection == null
                    || !categorySelection.code().equals(item.getIncomeCategory().getCategoryCode())
                    || !item.isActive()) {
                errors.putIfAbsent("category", "選択された項目がマスタに存在しません。");
                return null;
            }
            return item;
        }
        ExpenseItem item = masterData.expenseItemsById.get(itemId);
        if (item == null || categorySelection == null
                || !categorySelection.code().equals(item.getExpenseCategory().getCategoryCode())
                || !item.isActive()) {
            errors.putIfAbsent("category", "選択された項目がマスタに存在しません。");
            return null;
        }
        return item;
    }

    private Object resolveMethod(EntryKind kind, MasterData masterData, String rawMethodId, Map<String, String> errors) {
        String trimmed = trimToEmpty(rawMethodId);
        String field = kind.isIncome() ? "receiveMethodId" : "paymentMethodId";
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

    private LocalDate resolveDate(String rawDate, Map<String, String> errors, EntryKind kind) {
        String label = kind.isIncome() ? "収益発生日を選択してください。" : "支出発生日を選択してください。";
        String value = trimToEmpty(rawDate);
        if (value.isBlank()) {
            errors.putIfAbsent("date", label);
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(value, ISO_DATE);
            if (parsed.isAfter(LocalDate.now(TOKYO))) {
                errors.putIfAbsent("date", "登録日より未来の日付は選択できません。");
                return null;
            }
            return parsed;
        } catch (DateTimeParseException ex) {
            errors.putIfAbsent("date", "存在する日付を選択してください。");
            return null;
        }
    }

    private BigDecimal resolveAmount(String rawAmount, Map<String, String> errors) {
        String value = trimToEmpty(rawAmount);
        if (value.isBlank()) {
            errors.putIfAbsent("amount", "金額を入力してください。");
            return null;
        }
        if (!AMOUNT_PATTERN.matcher(value).matches()) {
            errors.putIfAbsent("amount", "金額は数値のみで入力してください。");
            return null;
        }
        BigDecimal parsed = new BigDecimal(value);
        if (parsed.compareTo(BigDecimal.ONE) < 0) {
            errors.putIfAbsent("amount", "金額は1円以上で入力してください。");
            return null;
        }
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            errors.putIfAbsent("amount", "金額は9999999999999円以下で入力してください。");
            return null;
        }
        return parsed.setScale(2);
    }

    private String resolveDetails(String rawDetails, Map<String, String> errors, EntryKind kind) {
        String value = rawDetails == null ? "" : rawDetails.trim();
        String required = kind.isIncome() ? "収益詳細を入力してください。" : "支出詳細を入力してください。";
        String lengthMessage = kind.isIncome() ? "収益詳細は1〜255文字で入力してください。" : "支出詳細は1〜255文字で入力してください。";
        if (value.isEmpty()) {
            errors.putIfAbsent("details", required);
            return null;
        }
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints < 1 || codePoints > 255) {
            errors.putIfAbsent("details", lengthMessage);
            return null;
        }
        if (FORBIDDEN_DETAILS.matcher(value).find()) {
            errors.putIfAbsent("details", "詳細に使用できない文字が含まれています。");
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
