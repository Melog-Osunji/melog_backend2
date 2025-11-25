package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyPostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HarmonyPostLikeRepository extends JpaRepository<HarmonyPostLike, UUID> {

	boolean existsByHarmonyPost_IdAndUser_Id(UUID harmonyPostId, UUID userId);
}
