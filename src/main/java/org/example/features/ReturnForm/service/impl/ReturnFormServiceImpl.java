package org.example.features.returnform.service.impl;

import org.example.entity.Expense;
import org.example.entity.Income;
import org.example.entity.User;
import org.example.features.auth.AuthUserContext;
import org.example.features.returnform.dto.ReturnFormView;
import org.example.features.returnform.service.ReturnFormService;
import org.example.repository.ExpenseRepository;
import org.example.repository.IncomeRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReturnFormServiceImpl implements ReturnFormService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ReturnFormServiceImpl(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnFormView getCurrentYearSummary() {
        int year = LocalDate.now().getYear();
        Optional<User> currentUserOptional = findCurrentUser();
        if (currentUserOptional.isEmpty()) {
            return new ReturnFormView(year, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        User currentUser = currentUserOptional.get();

        List<Income> incomes = incomeRepository.findByUserIdAndDeletedFlagFalseOrderByIncomeDateDescCreatedAtDesc(currentUser.getId()).stream()
                .filter(this::isProfitLossTargetIncome)
                .filter(income -> income.getIncomeDate().getYear() == year)
                .toList();
        List<Expense> expenses = expenseRepository.findByUserIdAndDeletedFlagFalseOrderByExpenseDateDescCreatedAtDesc(currentUser.getId()).stream()
                .filter(this::isProfitLossTargetExpense)
                .filter(expense -> expense.getExpenseDate().getYear() == year)
                .toList();

        BigDecimal totalIncome = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deductibleExpense = expenses.stream()
                .map(expense -> expense.getDeductibleAmount() == null ? BigDecimal.ZERO : expense.getDeductibleAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nonDeductibleExpense = totalExpense.subtract(deductibleExpense);

        return new ReturnFormView(
                year,
                totalIncome,
                deductibleExpense,
                totalIncome.subtract(deductibleExpense),
                nonDeductibleExpense);
    }

    private boolean isProfitLossTargetIncome(Income income) {
        return !Boolean.FALSE.equals(income.getBusinessTartget());
    }

    private boolean isProfitLossTargetExpense(Expense expense) {
        return !Boolean.FALSE.equals(expense.getBusinessTarget());
    }

    private Optional<User> findCurrentUser() {
        Long userId = AuthUserContext.getCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }
}

