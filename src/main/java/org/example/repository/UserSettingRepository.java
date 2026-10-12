package org.example.repository;

import java.util.Optional;

import org.example.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByAppUserId(Long appUserId);
}
