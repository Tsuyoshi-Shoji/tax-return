package org.example.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.example.entity.EvidenceFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, Long> {
    Optional<EvidenceFile> findByRetentionRecordId(Long retentionRecordId);

    List<EvidenceFile> findByRetentionRecordIdIn(Collection<Long> retentionRecordIds);
}
