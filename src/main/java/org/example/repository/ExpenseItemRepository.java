package org.example.repository;

import java.util.List;

import org.example.entity.ExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
    List<ExpenseItem> findByExpenseCategoryBusinessProfileIdOrderByIdAsc(Long businessProfileId);
}
