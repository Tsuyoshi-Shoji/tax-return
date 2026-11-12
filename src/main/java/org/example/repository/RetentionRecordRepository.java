package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.RetentionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RetentionRecordRepository extends JpaRepository<RetentionRecord, Long> {
    List<RetentionRecord> findByFiscalYearIdOrderByRetentionDeadlineAscIdAsc(Long fiscalYearId);

    @Query("select r from RetentionRecord r join fetch r.fiscalYear fy join fy.businessProfile bp "
            + "where r.id = :id and bp.appUser.id = :appUserId")
    Optional<RetentionRecord> findOwned(@Param("id") Long id, @Param("appUserId") Long appUserId);
}
