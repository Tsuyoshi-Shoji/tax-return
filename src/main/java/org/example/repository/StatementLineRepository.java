package org.example.repository;

import java.util.List;

import org.example.entity.StatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementLineRepository extends JpaRepository<StatementLine, Long> {
    List<StatementLine> findByFormCodeOrderByDisplayOrderAscIdAsc(String formCode);
}
