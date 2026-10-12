package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    List<ExpenseCategory> findByBusinessProfileIdOrderByIdAsc(Long businessProfileId);

    Optional<ExpenseCategory> findByBusinessProfileIdAndCategoryCode(Long businessProfileId, String categoryCode);
}
