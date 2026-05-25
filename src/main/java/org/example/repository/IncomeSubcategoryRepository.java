package org.example.repository;

import org.example.entity.IncomeSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncomeSubcategoryRepository extends JpaRepository<IncomeSubcategory, Long> {

    List<IncomeSubcategory> findAllByIncomeCategoryIdAndStatusOrderByDisplayOrderAsc(Long incomeCategoryId, String status);

    Optional<IncomeSubcategory> findByIdAndIncomeCategoryIdAndStatus(Long id, Long incomeCategoryId, String status);
}

