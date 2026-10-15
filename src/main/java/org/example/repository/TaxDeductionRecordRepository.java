package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.TaxDeductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxDeductionRecordRepository extends JpaRepository<TaxDeductionRecord, Long> {
    Optional<TaxDeductionRecord> findBySourceTransactionId(Long sourceTransactionId);

    List<TaxDeductionRecord> findByFiscalYearIdOrderByIdAsc(Long fiscalYearId);
}

