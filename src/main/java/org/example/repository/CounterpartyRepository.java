package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {
    Optional<Counterparty> findByBusinessProfileIdAndCounterpartyName(Long businessProfileId, String counterpartyName);

    List<Counterparty> findByBusinessProfileIdOrderByCounterpartyNameAsc(Long businessProfileId);
}
