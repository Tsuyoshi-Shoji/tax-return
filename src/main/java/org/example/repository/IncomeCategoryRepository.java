package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Long> {
    List<IncomeCategory> findByBusinessProfileIdOrderByIdAsc(Long businessProfileId);

    Optional<IncomeCategory> findByBusinessProfileIdAndCategoryCode(Long businessProfileId, String categoryCode);
}
