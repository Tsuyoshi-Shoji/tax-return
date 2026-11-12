package org.example.repository;

import java.util.Optional;

import org.example.entity.EvidenceDriveConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceDriveConnectionRepository extends JpaRepository<EvidenceDriveConnection, Long> {
    Optional<EvidenceDriveConnection> findByAppUserId(Long appUserId);
}
