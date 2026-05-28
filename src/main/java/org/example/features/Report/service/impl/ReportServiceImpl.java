package org.example.features.report.service.impl;

import org.example.entity.Expense;
import org.example.entity.Income;
import org.example.entity.User;
import org.example.features.auth.AuthUserContext;
import org.example.features.report.dto.CalendarEvent;
import org.example.features.report.dto.ItemAmountRow;
import org.example.features.report.dto.MonthlyAmountRow;
import org.example.features.report.dto.ReportDetailView;
import org.example.features.report.dto.ReportView;
import org.example.features.report.service.ReportService;
import org.example.repository.ExpenseRepository;
import org.example.repository.IncomeRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class ReportServiceImpl implements ReportService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportView getYearlyReport(int year) {
        return getYearlyReport(year, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportView getYearlyReport(int year, boolean includeTaxExcluded) {
        Optional<User> currentUserOptional = findDefaultUser();
        if (currentUserOptional.isEmpty()) {
            int currentYear = LocalDate.now().getYear();
            List<Integer> selectableYears = IntStream.rangeClosed(currentYear - 6, currentYear + 1)
                    .boxed()
                    .sorted(Comparator.reverseOrder())
                    .toList();
            return new ReportView(year, selectableYears, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        User currentUser = currentUserOptional.get();
        List<Income> incomes = filterIncomes(
                incomeRepository.findByUserIdAndDeletedFlagFalseOrderByIncomeDateDescCreatedAtDesc(currentUser.getId()),
                includeTaxExcluded);
        List<Expense> expenses = filterExpenses(
                expenseRepository.findByUserIdAndDeletedFlagFalseOrderByExpenseDateDescCreatedAtDesc(currentUser.getId()),
                includeTaxExcluded);

        List<MonthlyAmountRow> monthlyRows = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (int month = 1; month <= 12; month++) {
            final int targetMonth = month;
            BigDecimal incomeAmount = incomes.stream()
                    .filter(income -> income.getIncomeDate().getYear() == year && income.getIncomeDate().getMonthValue() == targetMonth)
                    .map(Income::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expenseAmount = expenses.stream()
                    .filter(expense -> expense.getExpenseDate().getYear() == year && expense.getExpenseDate().getMonthValue() == targetMonth)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalIncome = totalIncome.add(incomeAmount);
            totalExpense = totalExpense.add(expenseAmount);
            monthlyRows.add(new MonthlyAmountRow(targetMonth, incomeAmount, expenseAmount));
        }

        int currentYear = LocalDate.now().getYear();
        List<Integer> selectableYears = IntStream.rangeClosed(currentYear - 6, currentYear + 1)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();

        return new ReportView(year, selectableYears, monthlyRows, totalIncome, totalExpense, totalIncome.subtract(totalExpense));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailView getMonthlyReport(int year, int month) {
        return getMonthlyReport(year, month, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailView getMonthlyReport(int year, int month, boolean includeTaxExcluded) {
        Optional<User> currentUserOptional = findDefaultUser();
        if (currentUserOptional.isEmpty()) {
            throw new IllegalArgumentException("対象データが存在しません。");
        }
        User currentUser = currentUserOptional.get();
        List<Income> incomes = filterIncomes(
                incomeRepository.findByUserIdAndDeletedFlagFalseOrderByIncomeDateDescCreatedAtDesc(currentUser.getId()),
                includeTaxExcluded).stream()
                .filter(income -> income.getIncomeDate().getYear() == year && income.getIncomeDate().getMonthValue() == month)
                .toList();
        List<Expense> expenses = filterExpenses(
                expenseRepository.findByUserIdAndDeletedFlagFalseOrderByExpenseDateDescCreatedAtDesc(currentUser.getId()),
                includeTaxExcluded).stream()
                .filter(expense -> expense.getExpenseDate().getYear() == year && expense.getExpenseDate().getMonthValue() == month)
                .toList();

        BigDecimal totalIncome = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.stream()
                .map(expense -> resolveExpenseAmountForDetail(expense, includeTaxExcluded))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> incomeMap = new LinkedHashMap<>();
        for (Income income : incomes) {
            String key = income.getIncomeSubcategory() == null ? income.getIncomeCategory().getCategoryName() : income.getIncomeSubcategory().getSubcategoryName();
            incomeMap.merge(key, income.getAmount(), BigDecimal::add);
        }

        Map<String, BigDecimal> expenseMap = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            String key = expense.getExpenseSubcategory() == null ? expense.getExpenseCategory().getCategoryName() : expense.getExpenseSubcategory().getSubcategoryName();
            expenseMap.merge(key, resolveExpenseAmountForDetail(expense, includeTaxExcluded), BigDecimal::add);
        }

        List<ItemAmountRow> incomeBreakdown = incomeMap.entrySet().stream().map(entry -> new ItemAmountRow(entry.getKey(), entry.getValue())).toList();
        List<ItemAmountRow> expenseBreakdown = expenseMap.entrySet().stream().map(entry -> new ItemAmountRow(entry.getKey(), entry.getValue())).toList();

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        for (Income income : incomes) {
            calendarEvents.add(new CalendarEvent(income.getIncomeDate().getDayOfMonth(), "income"));
        }
        for (Expense expense : expenses) {
            calendarEvents.add(new CalendarEvent(expense.getExpenseDate().getDayOfMonth(), "expense"));
        }

        YearMonth targetMonth = YearMonth.of(year, month);
        if (targetMonth.lengthOfMonth() == 0) {
            throw new IllegalArgumentException("対象月が不正です。");
        }

        return new ReportDetailView(
                year,
                month,
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                incomeBreakdown,
                expenseBreakdown,
                calendarEvents);
    }

    private Optional<User> findDefaultUser() {
        Long userId = AuthUserContext.getCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    private List<Income> filterIncomes(List<Income> incomes, boolean includeTaxExcluded) {
        if (includeTaxExcluded) {
            return incomes;
        }
        return incomes.stream()
                .filter(this::isProfitLossTargetIncome)
                .toList();
    }

    private List<Expense> filterExpenses(List<Expense> expenses, boolean includeTaxExcluded) {
        if (includeTaxExcluded) {
            return expenses;
        }
        return expenses.stream()
                .filter(this::isProfitLossTargetExpense)
                .toList();
    }

    private boolean isProfitLossTargetIncome(Income income) {
        return !Boolean.FALSE.equals(income.getBusinessTartget());
    }

    private boolean isProfitLossTargetExpense(Expense expense) {
        return !Boolean.FALSE.equals(expense.getBusinessTarget());
    }

    private BigDecimal resolveExpenseAmountForDetail(Expense expense, boolean includeTaxExcluded) {
        if (includeTaxExcluded) {
            return expense.getAmount();
        }
        return expense.getDeductibleAmount() == null ? BigDecimal.ZERO : expense.getDeductibleAmount();
    }
}
