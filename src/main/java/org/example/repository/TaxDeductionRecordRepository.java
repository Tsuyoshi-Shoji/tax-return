package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.TaxDeductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxDeductionRecordRepository extends JpaRepository<TaxDeductionRecord, Long> {
    Optional<TaxDeductionRecord> findBySourceTransactionId(Long sourceTransactionId);

    @Modifying
    @Query("delete from TaxDeductionRecord r where r.sourceTransaction.id = :transactionId")
    void deleteBySourceTransactionId(@Param("transactionId") Long transactionId);

    List<TaxDeductionRecord> findByFiscalYearIdOrderByIdAsc(Long fiscalYearId);

    Optional<TaxDeductionRecord> findByIdAndFiscalYearId(Long id, Long fiscalYearId);
}

