package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.TaxDependent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxDependentRepository extends JpaRepository<TaxDependent, Long> {
    List<TaxDependent> findByFiscalYearIdOrderByBirthDateAscIdAsc(Long fiscalYearId);

    Optional<TaxDependent> findByIdAndFiscalYearId(Long id, Long fiscalYearId);
}
