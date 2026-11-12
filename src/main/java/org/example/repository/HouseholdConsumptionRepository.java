package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.HouseholdConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdConsumptionRepository extends JpaRepository<HouseholdConsumption, Long> {
    List<HouseholdConsumption> findByFiscalYearIdOrderByTransactionDateAscIdAsc(Long fiscalYearId);
    Optional<HouseholdConsumption> findByIdAndFiscalYearId(Long id, Long fiscalYearId);
}
