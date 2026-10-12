package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    List<FiscalYear> findByBusinessProfileIdOrderByTaxYearDesc(Long businessProfileId);

    Optional<FiscalYear> findByIdAndBusinessProfileId(Long id, Long businessProfileId);

    @Query("""
            select fy
            from FiscalYear fy
            join fy.businessProfile bp
            join bp.appUser appUser
            where appUser.id = :appUserId
              and fy.status = 'open'
              and fy.closedAt is null
              and :targetDate between fy.periodStart and fy.periodEnd
            order by fy.periodStart desc, fy.id desc
            """)
    List<FiscalYear> findOpenByAppUserIdAndDate(
            @Param("appUserId") Long appUserId,
            @Param("targetDate") java.time.LocalDate targetDate);

    @Query("""
            select fy
            from FiscalYear fy
            join fy.businessProfile bp
            join bp.appUser appUser
            where fy.id = :fiscalYearId
              and appUser.id = :appUserId
            """)
    Optional<FiscalYear> findOwnedById(
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("appUserId") Long appUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select fy
            from FiscalYear fy
            join fy.businessProfile bp
            join bp.appUser appUser
            where fy.id = :fiscalYearId
              and appUser.id = :appUserId
            """)
    Optional<FiscalYear> findOwnedByIdForUpdate(
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("appUserId") Long appUserId);
}
