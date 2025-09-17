package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.entity.HarmonyRoomPosts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HarmonyRoomPostsRepository extends JpaRepository<HarmonyRoomPosts, UUID> {

	/**
	 * 하모니룸으로 게시글 목록 조회
	 */
	Optional<HarmonyRoomPosts> findByHarmonyRoom(HarmonyRoom harmonyRoom);

	/**
	 * 하모니룸 ID로 게시글 목록 조회
	 */
	@Query("SELECT h FROM HarmonyRoomPosts h WHERE h.harmonyRoom.id = :harmonyRoomId")
	Optional<HarmonyRoomPosts> findByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);

	/**
	 * 여러 하모니룸의 게시글 목록 조회
	 */
	@Query("SELECT h FROM HarmonyRoomPosts h WHERE h.harmonyRoom IN :harmonyRooms")
	List<HarmonyRoomPosts> findByHarmonyRoomIn(@Param("harmonyRooms") List<HarmonyRoom> harmonyRooms);
}
