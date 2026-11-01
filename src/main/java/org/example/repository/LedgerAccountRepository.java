package org.example.repository;

import java.util.List;

import org.example.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    List<LedgerAccount> findAllByOrderByDisplayOrderAscIdAsc();
}
