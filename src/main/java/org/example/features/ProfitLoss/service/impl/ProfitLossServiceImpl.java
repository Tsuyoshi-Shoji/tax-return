package org.example.features.profitloss.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.example.entity.BusinessProfile;
import org.example.entity.ExpenseEntry;
import org.example.entity.ExpenseItem;
import org.example.entity.FiscalYear;
import org.example.entity.IncomeEntry;
import org.example.entity.IncomeItem;
import org.example.entity.AccountingTransaction;
import org.example.features.entry.EntryConflictException;
import org.example.features.entry.EntryNotFoundException;
import org.example.features.home.dto.FiscalYearOptionDto;
import org.example.features.profitloss.dto.ProfitLossDetailDto;
import org.example.features.profitloss.dto.ProfitLossItemOption;
import org.example.features.profitloss.dto.ProfitLossListRow;
import org.example.features.profitloss.dto.ProfitLossPageData;
import org.example.features.profitloss.form.ProfitLossSearchForm;
import org.example.features.profitloss.service.ProfitLossService;
import org.example.repository.BusinessProfileRepository;
import org.example.repository.ExpenseEntryRepository;
import org.example.repository.ExpenseItemRepository;
import org.example.repository.FiscalYearRepository;
import org.example.repository.IncomeEntryRepository;
import org.example.repository.IncomeItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfitLossServiceImpl implements ProfitLossService {
    private static final int PAGE_SIZE = 10;
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final Pattern ITEM_VALUE_PATTERN = Pattern.compile("^(income|expense):[0-9]+$");

    private final BusinessProfileRepository businessProfileRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final IncomeItemRepository incomeItemRepository;
    private final ExpenseItemRepository expenseItemRepository;

    public ProfitLossServiceImpl(
            BusinessProfileRepository businessProfileRepository,
            FiscalYearRepository fiscalYearRepository,
            IncomeEntryRepository incomeEntryRepository,
            ExpenseEntryRepository expenseEntryRepository,
            IncomeItemRepository incomeItemRepository,
            ExpenseItemRepository expenseItemRepository) {
        this.businessProfileRepository = businessProfileRepository;
        this.fiscalYearRepository = fiscalYearRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.expenseEntryRepository = expenseEntryRepository;
        this.incomeItemRepository = incomeItemRepository;
        this.expenseItemRepository = expenseItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitLossPageData loadPage(Long appUserId, ProfitLossSearchForm requestForm) {
        boolean datesNotRequested = requestForm == null
                || (requestForm.getDateFrom() == null && requestForm.getDateTo() == null);
        ProfitLossSearchForm form = normalizeForm(requestForm);
        List<String> errors= new ArrayList<>();
        ParsedFilter parsed = parseFilter(form, errors);
        form.setItems(new ArrayList<>(parsed.itemValues));
        form.setItem(parsed.itemValues.stream().findFirst().orElse(null));

        BusinessProfile profile = businessProfileRepository.findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Business profile not found."));
        List<FiscalYear> fiscalYears = fiscalYearRepository.findByBusinessProfileIdOrderByTaxYearDesc(profile.getId());
        List<FiscalYearOptionDto> fiscalYearOptions = fiscalYears.stream()
                .map(f -> new FiscalYearOptionDto(f.getId(), f.getTaxYear() + "年"))
                .toList();
        if (fiscalYears.isEmpty()) {
            return new ProfitLossPageData(
                    form,
                    errors,
                    fiscalYearOptions,
                    null,
                    "",
                    form.getTypes().contains("income"),
                    form.getTypes().contains("expense"),
                    List.of(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of(),
                    List.of(),
                    0,
                    0,
                    1,
                    1,
                    1,
                    1,
                    List.of(1),
                    List.of(1));
        }

        FiscalYear selectedFiscalYear = resolveFiscalYear(fiscalYears);
        if (datesNotRequested && errors.isEmpty()
                && selectedFiscalYear.getPeriodStart() != null && selectedFiscalYear.getPeriodEnd() != null) {
            parsed.dateFrom = selectedFiscalYear.getPeriodStart();
            parsed.dateTo = selectedFiscalYear.getPeriodEnd();
            form.setDateFrom(parsed.dateFrom.toString());
            form.setDateTo(parsed.dateTo.toString());
        }
        List<ProfitLossItemOption> itemOptions = loadItemOptions(profile.getId());

        List<IncomeEntry> incomes = incomeEntryRepository.findAllOwnedByFiscalYear(selectedFiscalYear.getId(), appUserId).stream()
                .filter(entry -> matchIncome(entry, parsed))
                .toList();
        List<ExpenseEntry> expenses = expenseEntryRepository.findAllOwnedByFiscalYear(selectedFiscalYear.getId(), appUserId).stream()
                .filter(entry -> matchExpense(entry, parsed))
                .toList();

        BigDecimal totalIncome = incomes.stream()
                .filter(entry -> !entry.isExcludeFromReturn())
                .map(IncomeEntry::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.stream()
                .map(this::businessExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOwnerDraw = expenses.stream()
                .map(this::ownerDraw)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = totalIncome.subtract(totalExpense);

        List<ProfitLossListRow> incomeRows = incomes.stream()
                .sorted(byDateDescForIncome())
                .map(this::mapIncomeRow)
                .toList();
        List<ProfitLossListRow> expenseRows = expenses.stream()
                .sorted(byDateDescForExpense())
                .map(this::mapExpenseRow)
                .toList();

        PagingResult<ProfitLossListRow> incomePaging = paginate(incomeRows, form.getIncomePage());
        PagingResult<ProfitLossListRow> expensePaging = paginate(expenseRows, form.getExpensePage());
        form.setIncomePage(incomePaging.currentPage);
        form.setExpensePage(expensePaging.currentPage);

        return new ProfitLossPageData(
                form,
                errors,
                fiscalYearOptions,
                selectedFiscalYear.getId(),
                selectedFiscalYear.getTaxYear() + "年",
                parsed.includeIncome,
                parsed.includeExpense,
                itemOptions,
                totalProfit,
                totalIncome,
                totalExpense,
                totalIncome,
                totalExpense,
                totalOwnerDraw,
                incomePaging.rows,
                expensePaging.rows,
                incomeRows.size(),
                expenseRows.size(),
                incomePaging.currentPage,
                incomePaging.totalPages,
                expensePaging.currentPage,
                expensePaging.totalPages,
                pageNumbers(incomePaging.currentPage, incomePaging.totalPages),
                pageNumbers(expensePaging.currentPage, expensePaging.totalPages));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitLossDetailDto loadDetail(Long appUserId, String recordType, Long id) {
        if ("income".equals(recordType)) {
            IncomeEntry e = incomeEntryRepository.findOwnedById(id, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            return new ProfitLossDetailDto(
                    "income", e.getId(), "収益",
                    e.getIncomeItem() == null ? "未分類" : e.getIncomeItem().getItemName(),
                    e.getTransactionDate(), e.getAmount(),
                    "受取方法", e.getReceiveMethod() == null ? "" : e.getReceiveMethod().getMethodName(),
                    e.isExcludeFromReturn(), false, null, null, e.getDetails(),
                    lockMessage(e.getFiscalYear(), e.getAccountingTransaction()));
        }
        if ("expense".equals(recordType)) {
            ExpenseEntry e = expenseEntryRepository.findOwnedById(id, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            return new ProfitLossDetailDto(
                    "expense", e.getId(), "支出",
                    e.getExpenseItem() == null ? "未分類" : e.getExpenseItem().getItemName(),
                    e.getTransactionDate(), e.getAmount(),
                    "支払方法", e.getPaymentMethod() == null ? "" : e.getPaymentMethod().getMethodName(),
                    e.isExcludeFromReturn(), e.isHomeApportionmentApplied(),
                    e.getHomeApportionmentRate(), e.getDeductibleAmount(), e.getDetails(),
                    lockMessage(e.getFiscalYear(), e.getAccountingTransaction()));
        }
        throw new EntryNotFoundException("Unknown record type");
    }

    @Override
    @Transactional
    public void delete(Long appUserId, String recordType, Long id) {
        if ("income".equals(recordType)) {
            Long fiscalYearId = incomeEntryRepository.findOwnedFiscalYearId(id, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            fiscalYearRepository.findOwnedByIdForUpdate(fiscalYearId, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
            IncomeEntry e = incomeEntryRepository.findOwnedByIdForUpdate(id, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            requireDeletable(lockMessage(e.getFiscalYear(), e.getAccountingTransaction()));
            e.markDeleted();
            incomeEntryRepository.save(e);
            return;
        }
        if ("expense".equals(recordType)) {
            Long fiscalYearId = expenseEntryRepository.findOwnedFiscalYearId(id, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            fiscalYearRepository.findOwnedByIdForUpdate(fiscalYearId, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Fiscal year not found"));
            ExpenseEntry e = expenseEntryRepository.findOwnedByIdForUpdate(id, appUserId)
                    .orElseThrow(() -> new EntryNotFoundException("Entry not found"));
            requireDeletable(lockMessage(e.getFiscalYear(), e.getAccountingTransaction()));
            e.markDeleted();
            expenseEntryRepository.save(e);
            return;
        }
        throw new EntryNotFoundException("Unknown record type");
    }

    private void requireDeletable(String lockMessage) {
        if (lockMessage != null) {
            throw new EntryConflictException(lockMessage);
        }
    }

    private String lockMessage(FiscalYear fiscalYear, AccountingTransaction transaction) {
        if (fiscalYear == null || !"open".equalsIgnoreCase(fiscalYear.getStatus()) || fiscalYear.getClosedAt() != null) {
            return "年度が締められているため編集・削除できません。";
        }
        if (transaction != null && !"draft".equalsIgnoreCase(transaction.getStatus())) {
            return "確定済みデータは編集・削除できません。";
        }
        return null;
    }

    private ProfitLossSearchForm normalizeForm(ProfitLossSearchForm requestForm) {
        ProfitLossSearchForm normalized = requestForm == null ? new ProfitLossSearchForm() : requestForm;
        if (normalized.getTypes() == null) {
            normalized.setTypes(List.of());
        }
        Set<String> normalizedTypes = normalized.getTypes().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> v.equals("income") || v.equals("expense"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedTypes.isEmpty()) {
            normalizedTypes.add("income");
            normalizedTypes.add("expense");
        }
        normalized.setTypes(new ArrayList<>(normalizedTypes));
        if (!"hide".equals(normalized.getTaxTarget())) {
            normalized.setTaxTarget("show");
        }
        normalized.setIncomePage(safePage(normalized.getIncomePage()));
        normalized.setExpensePage(safePage(normalized.getExpensePage()));
        normalized.setDateFrom(trimToNull(normalized.getDateFrom()));
        normalized.setDateTo(trimToNull(normalized.getDateTo()));
        normalized.setAmountFrom(trimToNull(normalized.getAmountFrom()));
        normalized.setAmountTo(trimToNull(normalized.getAmountTo()));
        normalized.setItem(trimToNull(normalized.getItem()));
        Set<String> normalizedItems = new LinkedHashSet<>();
        if (normalized.getItems() != null) {
            normalized.getItems().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(v -> ITEM_VALUE_PATTERN.matcher(v).matches())
                    .forEach(normalizedItems::add);
        }
        if (normalized.getItem() != null && ITEM_VALUE_PATTERN.matcher(normalized.getItem()).matches()) {
            normalizedItems.add(normalized.getItem());
        }
        normalized.setItems(new ArrayList<>(normalizedItems));
        normalized.setItem(normalizedItems.stream().findFirst().orElse(null));
        return normalized;
    }

    private ParsedFilter parseFilter(ProfitLossSearchForm form, List<String> errors) {
        LocalDate dateFrom = parseDate(form.getDateFrom(), "発生日Fromには存在する日付を指定してください。", errors);
        LocalDate dateTo = parseDate(form.getDateTo(), "発生日Toには存在する日付を指定してください。", errors);
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            errors.add("発生日Toは発生日From以降の日付を指定してください。");
        }
        BigDecimal amountFrom = parseAmount(form.getAmountFrom(), "金額Fromは数値で入力してください。", errors);
        BigDecimal amountTo = parseAmount(form.getAmountTo(), "金額Toは数値で入力してください。", errors);
        if (amountFrom != null && amountTo != null && amountTo.compareTo(amountFrom) < 0) {
            errors.add("金額Toは金額From以上を指定してください。");
        }
        ParsedFilter parsed = new ParsedFilter();
        parsed.dateFrom = dateFrom;
        parsed.dateTo = dateTo;
        parsed.amountFrom = amountFrom;
        parsed.amountTo = amountTo;
        parsed.includeIncome = form.getTypes().contains("income");
        parsed.includeExpense = form.getTypes().contains("expense");
        parsed.hideExcluded = "hide".equals(form.getTaxTarget());
        parsed.item = form.getItem();
        Set<String> itemValues = new LinkedHashSet<>();
        if (form.getItems() != null) {
            form.getItems().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(v -> ITEM_VALUE_PATTERN.matcher(v).matches())
                    .forEach(itemValues::add);
        }
        if (parsed.item != null && ITEM_VALUE_PATTERN.matcher(parsed.item).matches()) {
            itemValues.add(parsed.item);
        }
        itemValues.removeIf(v -> (v.startsWith("income:") && !parsed.includeIncome) || (v.startsWith("expense:") && !parsed.includeExpense));
        parsed.itemValues = itemValues;
        return parsed;
    }

    private LocalDate parseDate(String text, String errorMessage, List<String> errors) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            errors.add(errorMessage);
            return null;
        }
    }

    private BigDecimal parseAmount(String text, String errorMessage, List<String> errors) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (!DIGITS.matcher(text).matches()) {
            errors.add(errorMessage);
            return null;
        }
        return new BigDecimal(text);
    }

    private List<ProfitLossItemOption> loadItemOptions(Long businessProfileId) {
        List<ProfitLossItemOption> options = new ArrayList<>();
        incomeItemRepository.findByIncomeCategoryBusinessProfileIdOrderByIdAsc(businessProfileId).stream()
                .filter(IncomeItem::isActive)
                .forEach(item -> options.add(new ProfitLossItemOption(
                        "income:" + item.getId(),
                        item.getItemName(),
                        "income")));
        expenseItemRepository.findByExpenseCategoryBusinessProfileIdOrderByIdAsc(businessProfileId).stream()
                .filter(ExpenseItem::isActive)
                .forEach(item -> options.add(new ProfitLossItemOption(
                        "expense:" + item.getId(),
                        item.getItemName(),
                        "expense")));
        return options;
    }

    private FiscalYear resolveFiscalYear(List<FiscalYear> fiscalYears) {
        return fiscalYears.get(0);
    }

    private boolean matchIncome(IncomeEntry entry, ParsedFilter parsed) {
        if (!parsed.includeIncome) {
            return false;
        }
        if (!matchCommon(parsed, entry.getTransactionDate(), entry.getAmount(), entry.isExcludeFromReturn())) {
            return false;
        }
        if (parsed.itemValues.isEmpty()) {
            return true;
        }
        if (entry.getIncomeItem() == null) {
            return false;
        }
        return parsed.itemValues.contains("income:" + entry.getIncomeItem().getId());
    }

    private boolean matchExpense(ExpenseEntry entry, ParsedFilter parsed) {
        if (!parsed.includeExpense) {
            return false;
        }
        if (!matchCommon(parsed, entry.getTransactionDate(), entry.getAmount(), entry.isExcludeFromReturn())) {
            return false;
        }
        if (parsed.itemValues.isEmpty()) {
            return true;
        }
        if (entry.getExpenseItem() == null) {
            return false;
        }
        return parsed.itemValues.contains("expense:" + entry.getExpenseItem().getId());
    }

    private boolean matchCommon(ParsedFilter parsed, LocalDate date, BigDecimal amount, boolean excludeFromReturn) {
        if (date == null || amount == null) {
            return false;
        }
        if (parsed.dateFrom != null && date.isBefore(parsed.dateFrom)) {
            return false;
        }
        if (parsed.dateTo != null && date.isAfter(parsed.dateTo)) {
            return false;
        }
        if (parsed.amountFrom != null && amount.compareTo(parsed.amountFrom) < 0) {
            return false;
        }
        if (parsed.amountTo != null && amount.compareTo(parsed.amountTo) > 0) {
            return false;
        }
        if (parsed.hideExcluded && excludeFromReturn) {
            return false;
        }
        return true;
    }

    private Comparator<IncomeEntry> byDateDescForIncome() {
        return Comparator.comparing(IncomeEntry::getTransactionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(IncomeEntry::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<ExpenseEntry> byDateDescForExpense() {
        return Comparator.comparing(ExpenseEntry::getTransactionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExpenseEntry::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    // Excluded entries are outside business accounting; apportioned entries count only the deductible share.
    private BigDecimal businessExpense(ExpenseEntry entry) {
        if (entry.isExcludeFromReturn() || entry.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return entry.getDeductibleAmount() != null ? entry.getDeductibleAmount() : entry.getAmount();
    }

    // The private-use share of an apportioned expense is booked as owner's drawings (事業主貸).
    private BigDecimal ownerDraw(ExpenseEntry entry) {
        if (entry.isExcludeFromReturn() || !entry.isHomeApportionmentApplied() || entry.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return entry.getAmount().subtract(businessExpense(entry)).max(BigDecimal.ZERO);
    }

    private ProfitLossListRow mapIncomeRow(IncomeEntry entry) {
        return new ProfitLossListRow(
                entry.getId(),
                entry.getTransactionDate(),
                "収益",
                entry.getIncomeItem() == null ? "未分類" : entry.getIncomeItem().getItemName(),
                entry.getDetails(),
                entry.getAmount(),
                !entry.isExcludeFromReturn(),
                "income");
    }

    private ProfitLossListRow mapExpenseRow(ExpenseEntry entry) {
        return new ProfitLossListRow(
                entry.getId(),
                entry.getTransactionDate(),
                "支出",
                entry.getExpenseItem() == null ? "未分類" : entry.getExpenseItem().getItemName(),
                entry.getDetails(),
                entry.getAmount(),
                !entry.isExcludeFromReturn(),
                "expense");
    }

    private <T> PagingResult<T> paginate(List<T> rows, Integer requestedPage) {
        int totalPages = Math.max(1, (int) Math.ceil(rows.size() / (double) PAGE_SIZE));
        int currentPage = Math.min(Math.max(1, requestedPage == null ? 1 : requestedPage), totalPages);
        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(rows.size(), fromIndex + PAGE_SIZE);
        List<T> pageRows = fromIndex >= rows.size() ? List.of() : rows.subList(fromIndex, toIndex);
        return new PagingResult<>(pageRows, currentPage, totalPages);
    }

    private List<Integer> pageNumbers(int currentPage, int totalPages) {
        int start = Math.max(1, currentPage - 1);
        int end = Math.min(totalPages, start + 2);
        if (end - start < 2) {
            start = Math.max(1, end - 2);
        }
        List<Integer> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            pages.add(i);
        }
        return pages;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int safePage(Integer raw) {
        return raw == null || raw < 1 ? 1 : raw;
    }

    private static final class ParsedFilter {
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private BigDecimal amountFrom;
        private BigDecimal amountTo;
        private boolean includeIncome;
        private boolean includeExpense;
        private boolean hideExcluded;
        private String item;
        private Set<String> itemValues = Set.of();
    }

    private static final class PagingResult<T> {
        private final List<T> rows;
        private final int currentPage;
        private final int totalPages;

        private PagingResult(List<T> rows, int currentPage, int totalPages) {
            this.rows = rows;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
        }
    }
}
