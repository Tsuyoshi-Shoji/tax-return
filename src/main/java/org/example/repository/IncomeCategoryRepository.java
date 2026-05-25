package org.example.repository;

import org.example.entity.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Long> {

    List<IncomeCategory> findAllByStatusOrderByDisplayOrderAsc(String status);
}

