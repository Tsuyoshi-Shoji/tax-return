package org.example.repository;

import java.math.BigDecimal;
import java.util.List;

import org.example.entity.TransactionSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionSettlementRepository extends JpaRepository<TransactionSettlement, Long> {
    @Modifying
    @Query("delete from TransactionSettlement ts where ts.settlementAdjustment.id = :adjustmentId")
    void deleteBySettlementAdjustmentId(@Param("adjustmentId") Long adjustmentId);

    @Query("select coalesce(sum(ts.settledAmount), 0) from TransactionSettlement ts where ts.sourceTransaction.id = :transactionId")
    BigDecimal sumSettledAmountBySourceTransactionId(@Param("transactionId") Long transactionId);

    @Query("select max(ts.settlementAdjustment.id) from TransactionSettlement ts where ts.sourceTransaction.id = :transactionId")
    Long findLatestSettlementAdjustmentIdBySourceTransactionId(@Param("transactionId") Long transactionId);

    @Query("select ts from TransactionSettlement ts where ts.sourceTransaction.fiscalYear.id = :fiscalYearId")
    List<TransactionSettlement> findAllByFiscalYearId(@Param("fiscalYearId") Long fiscalYearId);
}

