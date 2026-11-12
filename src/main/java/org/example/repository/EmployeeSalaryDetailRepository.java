package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.EmployeeSalaryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeSalaryDetailRepository extends JpaRepository<EmployeeSalaryDetail, Long> {
    List<EmployeeSalaryDetail> findByFiscalYearIdOrderBySalaryKindAscPersonNameAscIdAsc(Long fiscalYearId);
    Optional<EmployeeSalaryDetail> findByIdAndFiscalYearId(Long id, Long fiscalYearId);
}
