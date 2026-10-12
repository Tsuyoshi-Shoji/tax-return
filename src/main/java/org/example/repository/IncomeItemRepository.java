package org.example.repository;

import java.util.List;

import org.example.entity.IncomeItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeItemRepository extends JpaRepository<IncomeItem, Long> {
    List<IncomeItem> findByIncomeCategoryBusinessProfileIdOrderByIdAsc(Long businessProfileId);
}
