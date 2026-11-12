package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    long countByFiscalYearIdAndStatus(Long fiscalYearId, String status);

    @Query("""
            select e from JournalEntry e
            where e.fiscalYear.id = :yearId
              and e.fiscalYear.businessProfile.appUser.id = :userId
              and (:status is null or e.status = :status
                   or (:status = 'reviewed' and e.status = 'finalized'))
            order by e.entryDate, e.id
            """)
    Page<JournalEntry> findReviewPage(@Param("yearId") Long yearId, @Param("userId") Long userId,
            @Param("status") String status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from JournalEntry e
            where e.id = :id and e.fiscalYear.id = :yearId
              and e.fiscalYear.businessProfile.appUser.id = :userId
            """)
    Optional<JournalEntry> findOwnedForReview(@Param("id") Long id, @Param("yearId") Long yearId,
            @Param("userId") Long userId);

    Optional<JournalEntry> findByAccountingTransactionId(Long accountingTransactionId);

    Optional<JournalEntry> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<JournalEntry> findByFiscalYearIdAndSourceType(Long fiscalYearId, String sourceType);

    @Query("""
            select e
            from JournalEntry e
            left join fetch e.lines l
            where e.fiscalYear.id = :fiscalYearId
            order by e.entryDate asc, e.id asc, l.lineNo asc
            """)
    List<JournalEntry> findAllWithLinesByFiscalYearId(@Param("fiscalYearId") Long fiscalYearId);
}

