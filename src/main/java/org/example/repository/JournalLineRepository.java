package org.example.repository;

import org.example.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
}

