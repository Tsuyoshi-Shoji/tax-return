package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.AccountingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AccountingTransactionRepository extends JpaRepository<AccountingTransaction, Long> {
    List<AccountingTransaction> findTop5ByFiscalYearIdAndStatusOrderByTransactionDateDescIdDesc(
            Long fiscalYearId,
            String status);

    List<AccountingTransaction> findTop5ByFiscalYearIdOrderByTransactionDateDescIdDesc(Long fiscalYearId);

    long countByFiscalYearIdAndStatus(Long fiscalYearId, String status);

    @Query("""
            select tx
            from AccountingTransaction tx
            join tx.fiscalYear fy
            join fy.businessProfile bp
            join bp.appUser appUser
            where tx.id = :transactionId
              and appUser.id = :appUserId
            """)
    Optional<AccountingTransaction> findOwnedById(
            @Param("transactionId") Long transactionId,
            @Param("appUserId") Long appUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select tx
            from AccountingTransaction tx
            join tx.fiscalYear fy
            join fy.businessProfile bp
            join bp.appUser appUser
            where tx.id = :transactionId
              and appUser.id = :appUserId
            """)
    Optional<AccountingTransaction> findOwnedByIdForUpdate(
            @Param("transactionId") Long transactionId,
            @Param("appUserId") Long appUserId);
}
