package org.example.repository;

import java.util.List;

import org.example.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    @Query("""
            select l
            from JournalLine l
            join fetch l.journalEntry e
            join fetch e.fiscalYear f
            where l.accountCode is null
              and f.status <> 'closed'
            """)
    List<JournalLine> findUncodedLinesInOpenFiscalYears();
}

