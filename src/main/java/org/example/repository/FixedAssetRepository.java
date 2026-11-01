package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {
    List<FixedAsset> findByBusinessProfileIdOrderByAcquisitionDateAscIdAsc(Long businessProfileId);

    Optional<FixedAsset> findByIdAndBusinessProfileId(Long id, Long businessProfileId);
}
