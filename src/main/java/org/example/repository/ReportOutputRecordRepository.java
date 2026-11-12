package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.ReportOutputRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportOutputRecordRepository extends JpaRepository<ReportOutputRecord, Long> {
    List<ReportOutputRecord> findByFiscalYearIdOrderByGeneratedAtDescIdDesc(Long fiscalYearId, Pageable pageable);

    long countByFiscalYearId(Long fiscalYearId);

    @Query("select r from ReportOutputRecord r join fetch r.fiscalYear fy join fy.businessProfile bp "
            + "where r.id = :id and bp.appUser.id = :appUserId")
    Optional<ReportOutputRecord> findOwned(@Param("id") Long id, @Param("appUserId") Long appUserId);

    @Modifying
    @Query("update ReportOutputRecord r set r.recordStatus = 'SUPERSEDED', r.updatedAt = CURRENT_TIMESTAMP "
            + "where r.fiscalYear.id = :fiscalYearId and r.closeVersion = :closeVersion and r.recordStatus = 'CURRENT'")
    int supersedeVersion(@Param("fiscalYearId") Long fiscalYearId, @Param("closeVersion") Integer closeVersion);
}
