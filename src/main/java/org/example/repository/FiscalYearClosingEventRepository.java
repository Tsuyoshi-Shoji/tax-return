package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.FiscalYearClosingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalYearClosingEventRepository extends JpaRepository<FiscalYearClosingEvent, Long> {
    List<FiscalYearClosingEvent> findByFiscalYearIdOrderByIdAsc(Long fiscalYearId);

    Optional<FiscalYearClosingEvent> findFirstByFiscalYearIdAndEventTypeOrderByCloseVersionDesc(
            Long fiscalYearId, String eventType);
}
