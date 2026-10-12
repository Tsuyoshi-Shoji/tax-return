package org.example.repository;

import java.util.Optional;

import org.example.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    Optional<BusinessProfile> findByAppUserId(Long appUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bp
            from BusinessProfile bp
            join fetch bp.appUser appUser
            where appUser.id = :appUserId
            """)
    Optional<BusinessProfile> findByAppUserIdForUpdate(@Param("appUserId") Long appUserId);
}
