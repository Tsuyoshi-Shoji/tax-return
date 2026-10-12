package org.example.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.entity.ExpenseEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseEntryRepository extends JpaRepository<ExpenseEntry, Long> {

    @Query("""
            select coalesce(sum(e.amount), 0)
            from ExpenseEntry e
            where e.fiscalYear.id = :fiscalYearId
              and e.deleted = false
              and e.transactionDate between :startDate and :endDate
            """)
    BigDecimal sumAllAmountInRange(
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(case when e.deductibleAmount is not null then e.deductibleAmount else e.amount end), 0)
            from ExpenseEntry e
            where e.fiscalYear.id = :fiscalYearId
              and e.deleted = false
              and e.excludeFromReturn = false
              and e.transactionDate between :startDate and :endDate
            """)
    BigDecimal sumBusinessAmountInRange(
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select e
            from ExpenseEntry e
            join fetch e.fiscalYear fy
            join fetch fy.businessProfile bp
            join bp.appUser appUser
            left join fetch e.expenseItem item
            left join fetch item.expenseCategory category
            left join fetch e.paymentMethod paymentMethod
            left join fetch e.accountingTransaction accountingTransaction
            where e.id = :entryId
              and e.deleted = false
              and appUser.id = :appUserId
            """)
    Optional<ExpenseEntry> findOwnedById(
            @Param("entryId") Long entryId,
            @Param("appUserId") Long appUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from ExpenseEntry e
            join fetch e.fiscalYear fy
            join fetch fy.businessProfile bp
            join bp.appUser appUser
            left join fetch e.expenseItem item
            left join fetch item.expenseCategory category
            left join fetch e.paymentMethod paymentMethod
            left join fetch e.accountingTransaction accountingTransaction
            where e.id = :entryId
              and e.deleted = false
              and appUser.id = :appUserId
            """)
    Optional<ExpenseEntry> findOwnedByIdForUpdate(
            @Param("entryId") Long entryId,
            @Param("appUserId") Long appUserId);

    @Query("""
            select fy.id
            from ExpenseEntry e
            join e.fiscalYear fy
            join fy.businessProfile bp
            join bp.appUser appUser
            where e.id = :entryId
              and e.deleted = false
              and appUser.id = :appUserId
            """)
    Optional<Long> findOwnedFiscalYearId(
            @Param("entryId") Long entryId,
            @Param("appUserId") Long appUserId);

    @Query("""
            select e
            from ExpenseEntry e
            join fetch e.fiscalYear fy
            join fetch fy.businessProfile bp
            join bp.appUser appUser
            left join fetch e.expenseItem item
            left join fetch item.expenseCategory category
            where fy.id = :fiscalYearId
              and e.deleted = false
              and appUser.id = :appUserId
            order by e.transactionDate desc, e.id desc
            """)
    List<ExpenseEntry> findAllOwnedByFiscalYear(
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("appUserId") Long appUserId);
}