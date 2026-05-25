package org.example.repository;

import org.example.entity.ExpenseSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseSubcategoryRepository extends JpaRepository<ExpenseSubcategory, Long> {

    List<ExpenseSubcategory> findAllByExpenseCategoryIdAndStatusOrderByDisplayOrderAsc(Long expenseCategoryId, String status);

    Optional<ExpenseSubcategory> findByIdAndExpenseCategoryIdAndStatus(Long id, Long expenseCategoryId, String status);
}

