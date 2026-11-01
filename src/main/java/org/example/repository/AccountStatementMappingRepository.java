package org.example.repository;

import java.util.List;

import org.example.entity.AccountStatementMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatementMappingRepository extends JpaRepository<AccountStatementMapping, Long> {
    List<AccountStatementMapping> findByFormCode(String formCode);
}
