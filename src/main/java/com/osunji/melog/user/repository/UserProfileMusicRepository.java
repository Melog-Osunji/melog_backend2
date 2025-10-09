package com.osunji.melog.user.repository;

import com.osunji.melog.user.domain.UserProfileMusic;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileMusicRepository extends JpaRepository<UserProfileMusic, UUID> {

    Optional<UserProfileMusic> findTopByUser_IdAndIsActiveTrueOrderBySelectedAtDesc(UUID userId);

    List<UserProfileMusic> findByUser_IdOrderBySelectedAtDesc(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserProfileMusic m set m.isActive=false where m.user.id=:userId and m.isActive=true")
    int deactivateAllActive(@Param("userId") UUID userId);
}
