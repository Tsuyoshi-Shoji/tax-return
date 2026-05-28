package org.example.features.profitloss.service.impl;

import org.example.entity.Expense;
import org.example.entity.Income;
import org.example.entity.User;
import org.example.features.auth.AuthUserContext;
import org.example.features.profitloss.dto.ProfitLossDetailView;
import org.example.features.profitloss.dto.ProfitLossListRow;
import org.example.features.profitloss.dto.ProfitLossSearchResult;
import org.example.features.profitloss.form.ProfitLossSearchForm;
import org.example.features.profitloss.service.ProfitLossService;
import org.example.repository.ExpenseRepository;
import org.example.repository.IncomeRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfitLossServiceImpl implements ProfitLossService {

    private static final int PAGE_SIZE = 10;

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ProfitLossServiceImpl(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitLossSearchResult search(ProfitLossSearchForm form) {
        Optional<User> currentUserOptional = findCurrentUser();
        if (currentUserOptional.isEmpty()) {
            return new ProfitLossSearchResult(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of(),
                    List.of(),
                    1,
                    1,
                    1,
                    1);
        }
        User currentUser = currentUserOptional.get();
        List<Income> allIncomes = incomeRepository.findByUserIdAndDeletedFlagFalseOrderByIncomeDateDescCreatedAtDesc(currentUser.getId());
        List<Expense> allExpenses = expenseRepository.findByUserIdAndDeletedFlagFalseOrderByExpenseDateDescCreatedAtDesc(currentUser.getId());

        List<Income> incomeCandidates = form.isShowTaxExcluded()
                ? allIncomes
                : allIncomes.stream().filter(this::isProfitLossTargetIncome).toList();

        List<ProfitLossListRow> filteredIncomeRows = incomeCandidates.stream()
                .filter(income -> matchesIncome(income, form))
                .sorted(Comparator.comparing(Income::getIncomeDate, Comparator.reverseOrder()).thenComparing(Income::getId, Comparator.reverseOrder()))
                .map(this::toIncomeRow)
                .toList();

        List<Expense> expenseCandidates = form.isShowTaxExcluded()
                ? allExpenses
                : allExpenses.stream().filter(this::isProfitLossTargetExpense).toList();

        List<ProfitLossListRow> filteredExpenseRows = expenseCandidates.stream()
                .filter(expense -> matchesExpense(expense, form))
                .sorted(Comparator.comparing(Expense::getExpenseDate, Comparator.reverseOrder()).thenComparing(Expense::getId, Comparator.reverseOrder()))
                .map(this::toExpenseRow)
                .toList();

        BigDecimal totalIncome = filteredIncomeRows.stream().map(ProfitLossListRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = filteredExpenseRows.stream().map(ProfitLossListRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProfitLossSearchResult(
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                paginate(filteredIncomeRows, form.getIncomePage()),
                paginate(filteredExpenseRows, form.getExpensePage()),
                normalizePage(form.getIncomePage(), filteredIncomeRows.size()),
                totalPages(filteredIncomeRows.size()),
                normalizePage(form.getExpensePage(), filteredExpenseRows.size()),
                totalPages(filteredExpenseRows.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitLossDetailView getDetail(String recordType, Long id) {
        User currentUser = findCurrentUser().orElseThrow(() -> new IllegalStateException("Current user not found"));
        if ("income".equals(recordType)) {
            Income income = incomeRepository.findByIdAndUserIdAndDeletedFlagFalse(id, currentUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("対象の収益が見つかりません。"));
            return new ProfitLossDetailView(
                    income.getId(),
                    "income",
                    income.getIncomeDate(),
                    income.getIncomeCategory().getCategoryName(),
                    income.getIncomeSubcategory() == null ? income.getIncomeCategory().getCategoryName() : income.getIncomeSubcategory().getSubcategoryName(),
                    income.getAmount(),
                    income.getDescription());
        }

        Expense expense = expenseRepository.findByIdAndUserIdAndDeletedFlagFalse(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("対象の支出が見つかりません。"));
        return new ProfitLossDetailView(
                expense.getId(),
                "expense",
                expense.getExpenseDate(),
                expense.getExpenseCategory().getCategoryName(),
                expense.getExpenseSubcategory() == null ? expense.getExpenseCategory().getCategoryName() : expense.getExpenseSubcategory().getSubcategoryName(),
                expense.getAmount(),
                expense.getDescription());
    }

    @Override
    @Transactional
    public void delete(String recordType, Long id) {
        User currentUser = findCurrentUser().orElseThrow(() -> new IllegalStateException("Current user not found"));
        if ("income".equals(recordType)) {
            Income income = incomeRepository.findByIdAndUserIdAndDeletedFlagFalse(id, currentUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("対象の収益が見つかりません。"));
            income.setDeletedFlag(Boolean.TRUE);
            income.setDeletedAt(LocalDateTime.now());
            return;
        }

        Expense expense = expenseRepository.findByIdAndUserIdAndDeletedFlagFalse(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("対象の支出が見つかりません。"));
        expense.setDeletedFlag(Boolean.TRUE);
        expense.setDeletedAt(LocalDateTime.now());
    }

    private boolean matchesIncome(Income income, ProfitLossSearchForm form) {
        if (!matchesDateRange(income.getIncomeDate(), form.getDateFrom(), form.getDateTo())) {
            return false;
        }
        if (!matchesAmountRange(income.getAmount(), form.getAmountFrom(), form.getAmountTo())) {
            return false;
        }
        String type = incomeTypeValue(income);
        if (!matchesSelectedTypes(type, form.getTypes())) {
            return false;
        }
        return matchesSelectedItems(type, income.getIncomeSubcategory() == null ? income.getIncomeCategory().getCategoryName() : income.getIncomeSubcategory().getSubcategoryName(), form.getItems());
    }

    private boolean matchesExpense(Expense expense, ProfitLossSearchForm form) {
        if (!matchesDateRange(expense.getExpenseDate(), form.getDateFrom(), form.getDateTo())) {
            return false;
        }
        if (!matchesAmountRange(expense.getAmount(), form.getAmountFrom(), form.getAmountTo())) {
            return false;
        }
        String type = expenseTypeValue(expense);
        if (!matchesSelectedTypes(type, form.getTypes())) {
            return false;
        }
        return matchesSelectedItems(type, expense.getExpenseSubcategory() == null ? expense.getExpenseCategory().getCategoryName() : expense.getExpenseSubcategory().getSubcategoryName(), form.getItems());
    }

    private boolean matchesDateRange(LocalDate targetDate, String fromText, String toText) {
        if (fromText != null && !fromText.isBlank() && targetDate.isBefore(LocalDate.parse(fromText))) {
            return false;
        }
        return toText == null || toText.isBlank() || !targetDate.isAfter(LocalDate.parse(toText));
    }

    private boolean matchesAmountRange(BigDecimal amount, String fromText, String toText) {
        if (fromText != null && !fromText.isBlank() && amount.compareTo(new BigDecimal(fromText)) < 0) {
            return false;
        }
        return toText == null || toText.isBlank() || amount.compareTo(new BigDecimal(toText)) <= 0;
    }

    private boolean matchesSelectedTypes(String type, List<String> selectedTypes) {
        return selectedTypes == null || selectedTypes.isEmpty() || selectedTypes.contains(type);
    }

    private boolean matchesSelectedItems(String type, String item, String itemsValue) {
        if (itemsValue == null || itemsValue.isBlank()) {
            return true;
        }
        Set<String> selected = new HashSet<>();
        for (String token : itemsValue.split(",")) {
            if (!token.isBlank()) {
                selected.add(token.trim());
            }
        }
        return selected.contains(type + ":" + item);
    }

    private ProfitLossListRow toIncomeRow(Income income) {
        return new ProfitLossListRow(
                income.getId(),
                income.getIncomeDate(),
                income.getIncomeCategory().getCategoryName(),
                income.getIncomeSubcategory() == null ? income.getIncomeCategory().getCategoryName() : income.getIncomeSubcategory().getSubcategoryName(),
                income.getAmount());
    }

    private boolean isProfitLossTargetIncome(Income income) {
        return !Boolean.FALSE.equals(income.getBusinessTartget());
    }

    private boolean isProfitLossTargetExpense(Expense expense) {
        return !Boolean.FALSE.equals(expense.getBusinessTarget());
    }

    private ProfitLossListRow toExpenseRow(Expense expense) {
        return new ProfitLossListRow(
                expense.getId(),
                expense.getExpenseDate(),
                expense.getExpenseCategory().getCategoryName(),
                expense.getExpenseSubcategory() == null ? expense.getExpenseCategory().getCategoryName() : expense.getExpenseSubcategory().getSubcategoryName(),
                expense.getAmount());
    }

    private List<ProfitLossListRow> paginate(List<ProfitLossListRow> rows, int requestedPage) {
        int currentPage = normalizePage(requestedPage, rows.size());
        int fromIndex = Math.max(0, (currentPage - 1) * PAGE_SIZE);
        int toIndex = Math.min(rows.size(), fromIndex + PAGE_SIZE);
        if (fromIndex >= rows.size()) {
            return List.of();
        }
        return new ArrayList<>(rows.subList(fromIndex, toIndex));
    }

    private int totalPages(int totalSize) {
        return Math.max(1, (int) Math.ceil((double) totalSize / PAGE_SIZE));
    }

    private int normalizePage(int requestedPage, int totalSize) {
        return Math.min(Math.max(1, requestedPage), totalPages(totalSize));
    }

    private String incomeTypeValue(Income income) {
        Long categoryId = income.getIncomeCategory().getId();
        if (categoryId == 1L) {
            return "business";
        }
        if (categoryId == 2L) {
            return "investment";
        }
        if (categoryId == 3L) {
            return "temporary";
        }
        return "nontaxable";
    }

    private String expenseTypeValue(Expense expense) {
        return switch (expense.getExpenseCategory().getExpenseType()) {
            case "BUSINESS" -> "expense";
            case "PUBLIC" -> "public";
            default -> "private";
        };
    }

    private Optional<User> findCurrentUser() {
        Long userId = AuthUserContext.getCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }
}

