package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByAccountingTransactionId(Long accountingTransactionId);

    Optional<JournalEntry> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    @Query("""
            select e
            from JournalEntry e
            left join fetch e.lines l
            where e.fiscalYear.id = :fiscalYearId
            order by e.entryDate asc, e.id asc, l.lineNo asc
            """)
    List<JournalEntry> findAllWithLinesByFiscalYearId(@Param("fiscalYearId") Long fiscalYearId);
}

