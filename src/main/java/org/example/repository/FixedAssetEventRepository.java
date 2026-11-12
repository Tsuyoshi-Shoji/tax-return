package org.example.repository;

import java.util.List;
import java.util.Optional;

import org.example.entity.FixedAssetEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FixedAssetEventRepository extends JpaRepository<FixedAssetEvent, Long> {
    List<FixedAssetEvent> findByFixedAssetIdOrderByEffectiveDateAscIdAsc(Long fixedAssetId);
    Optional<FixedAssetEvent> findByIdAndFixedAssetBusinessProfileId(Long id, Long businessProfileId);
    @Query("select e from FixedAssetEvent e where e.fixedAsset.businessProfile.id = :profileId order by e.effectiveDate, e.id")
    List<FixedAssetEvent> findByBusinessProfileIdOrderByEffectiveDateAscIdAsc(@Param("profileId") Long profileId);
}
