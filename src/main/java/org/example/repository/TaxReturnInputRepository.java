package org.example.repository;

import java.util.Optional;

import org.example.entity.TaxReturnInput;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxReturnInputRepository extends JpaRepository<TaxReturnInput, Long> {
    Optional<TaxReturnInput> findByFiscalYearId(Long fiscalYearId);
}
