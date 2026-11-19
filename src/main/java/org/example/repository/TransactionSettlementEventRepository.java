package org.example.repository;

import java.math.BigDecimal;
import java.util.List;

import org.example.entity.TransactionSettlementEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionSettlementEventRepository extends JpaRepository<TransactionSettlementEvent, Long> {
    List<TransactionSettlementEvent> findByFiscalYearIdAndIdempotencyKey(Long fiscalYearId, String idempotencyKey);

    @Query("""
            select coalesce(sum(e.settledAmount), 0)
            from TransactionSettlementEvent e
            where e.sourceTransaction.id = :transactionId
            """)
    BigDecimal sumSettledAmountBySourceTransactionId(@Param("transactionId") Long transactionId);

    @Query("""
            select max(e.settlementTransaction.id)
            from TransactionSettlementEvent e
            where e.sourceTransaction.id = :transactionId
            """)
    Long findLatestSettlementTransactionIdBySourceTransactionId(@Param("transactionId") Long transactionId);
}

