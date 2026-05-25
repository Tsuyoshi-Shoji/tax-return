package org.example.features.home.service.impl;

import org.example.entity.Expense;
import org.example.entity.Income;
import org.example.entity.User;
import org.example.features.home.service.HomeService;
import org.example.repository.ExpenseRepository;
import org.example.repository.IncomeRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class HomeServiceImpl implements HomeService {

    private static final Long DEFAULT_USER_ID = 1L;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public HomeServiceImpl(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String getWelcomeMessage() {
        Optional<User> currentUserOptional = findCurrentUser();
        if (currentUserOptional.isEmpty()) {
            return "ようこそ。";
        }
        User currentUser = currentUserOptional.get();
        String name = currentUser.getUsername();
        return (name == null || name.isBlank() ? currentUser.getEmail() : name) + " さん、ようこそ。";
    }

    @Override
    public String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentTransactions(int limit) {
        Optional<User> currentUserOptional = findCurrentUser();
        if (currentUserOptional.isEmpty()) {
            return List.of();
        }
        User currentUser = currentUserOptional.get();
        List<Income> incomes = incomeRepository.findByUserIdAndDeletedFlagFalseOrderByCreatedAtDesc(currentUser.getId());
        List<Expense> expenses = expenseRepository.findByUserIdAndDeletedFlagFalseOrderByCreatedAtDesc(currentUser.getId());

        return Stream.concat(
                        incomes.stream().map(this::toIncomeTransaction),
                        expenses.stream().map(this::toExpenseTransaction))
                .sorted((left, right) -> ((LocalDate) right.get("sortDate")).compareTo((LocalDate) left.get("sortDate")))
                .limit(limit)
                .peek(transaction -> transaction.remove("sortDate"))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAnnualProfitLoss() {
        return getAnnualProfitLoss(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAnnualProfitLoss(boolean includeTaxExcluded) {
        LocalDate today = LocalDate.now();
        return getProfitLossBetween(LocalDate.of(today.getYear(), Month.JANUARY, 1), today, includeTaxExcluded);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyProfitLoss() {
        return getMonthlyProfitLoss(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyProfitLoss(boolean includeTaxExcluded) {
        LocalDate today = LocalDate.now();
        return getProfitLossBetween(today.withDayOfMonth(1), today, includeTaxExcluded);
    }

    @Override
    public long getDaysUntilTaxDeadline() {
        LocalDate today = LocalDate.now();
        LocalDate deadline = LocalDate.of(today.getYear(), 3, 15);
        if (today.isAfter(deadline)) {
            deadline = deadline.plusYears(1);
        }
        deadline = moveToNextBusinessDay(deadline);
        return java.time.temporal.ChronoUnit.DAYS.between(today, deadline);
    }

    @Override
    public boolean isApproachingDeadline() {
        return getDaysUntilTaxDeadline() <= 90;
    }

    private Map<String, Object> getProfitLossBetween(LocalDate fromDate, LocalDate toDate, boolean includeTaxExcluded) {
        Optional<User> currentUserOptional = findCurrentUser();
        if (currentUserOptional.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("income", BigDecimal.ZERO);
            empty.put("expense", BigDecimal.ZERO);
            empty.put("profit", BigDecimal.ZERO);
            return empty;
        }

        User currentUser = currentUserOptional.get();
        BigDecimal income = includeTaxExcluded
                ? nvl(incomeRepository.sumAmountByUserIdAndPeriodIncludingAllData(currentUser.getId(), fromDate, toDate))
                : nvl(incomeRepository.sumAmountByUserIdAndPeriod(currentUser.getId(), fromDate, toDate));
        BigDecimal expense = nvl(expenseRepository.sumAmountByUserIdAndPeriod(currentUser.getId(), fromDate, toDate));
        Map<String, Object> result = new HashMap<>();
        result.put("income", income);
        result.put("expense", expense);
        result.put("profit", income.subtract(expense));
        return result;
    }

    private Optional<User> findCurrentUser() {
        return userRepository.findById(DEFAULT_USER_ID);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate moveToNextBusinessDay(LocalDate date) {
        LocalDate adjusted = date;
        while (adjusted.getDayOfWeek().getValue() >= 6) {
            adjusted = adjusted.plusDays(1);
        }
        return adjusted;
    }

    private Map<String, Object> toIncomeTransaction(Income income) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("sortDate", income.getIncomeDate());
        transaction.put("date", income.getIncomeDate());
        transaction.put("type", "収益");
        transaction.put("category", income.getIncomeSubcategory() == null ? income.getIncomeCategory().getCategoryName() : income.getIncomeSubcategory().getSubcategoryName());
        transaction.put("amount", income.getAmount());
        return transaction;
    }

    private Map<String, Object> toExpenseTransaction(Expense expense) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("sortDate", expense.getExpenseDate());
        transaction.put("date", expense.getExpenseDate());
        transaction.put("type", "支出");
        transaction.put("category", expense.getExpenseSubcategory() == null ? expense.getExpenseCategory().getCategoryName() : expense.getExpenseSubcategory().getSubcategoryName());
        transaction.put("amount", expense.getAmount());
        return transaction;
    }
}
