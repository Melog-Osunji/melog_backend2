package com.osunji.melog.harmony.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.osunji.melog.harmony.entity.HarmonyPostBookmark;
import java.util.UUID;

@Repository
public interface HarmonyRoomPostBookmarkRepository extends JpaRepository<HarmonyPostBookmark, UUID> {
	boolean existsByHarmonyPost_IdAndUser_Id(UUID harmonyPostId, UUID userId);
}
