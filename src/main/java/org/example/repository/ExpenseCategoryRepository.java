package org.example.repository;

import org.example.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findAllByStatusOrderByDisplayOrderAsc(String status);

    Optional<ExpenseCategory> findByExpenseTypeAndStatus(String expenseType, String status);
}

