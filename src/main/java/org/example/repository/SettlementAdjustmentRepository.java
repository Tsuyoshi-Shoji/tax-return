package org.example.repository;

import java.util.Optional;

import org.example.entity.SettlementAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementAdjustmentRepository extends JpaRepository<SettlementAdjustment, Long> {
    Optional<SettlementAdjustment> findByFiscalYearId(Long fiscalYearId);
}
