package com.osunji.melog.user.repository;

import com.osunji.melog.user.domain.UserProfileMusic;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileMusicRepository extends JpaRepository<UserProfileMusic, UUID> {

    boolean existsByUserId(UUID userId);
    Optional<UserProfileMusic> findByUserIdAndId(UUID userId, UUID youtubeVideoId);
    Optional<UserProfileMusic> findByUserId(UUID userId);

}
