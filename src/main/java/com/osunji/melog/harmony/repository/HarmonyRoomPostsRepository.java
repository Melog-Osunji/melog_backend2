package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.entity.HarmonyRoomPosts;

import org.springframework.data.jpa.repository.EntityGraph;
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
	 * 여러 하모니룸에서 미디어가 있는 게시글만 조회 (최신순)
	 */
	List<HarmonyRoomPosts> findByHarmonyRoomInAndMediaTypeIsNotNullOrderByCreatedAtDesc(List<HarmonyRoom> harmonyRooms);

	/**
	 * 특정 하모니룸에서 미디어가 있는 게시글만 조회
	 */
	List<HarmonyRoomPosts> findByHarmonyRoomAndMediaTypeIsNotNullOrderByCreatedAtDesc(HarmonyRoom harmonyRoom);
	// 연관관계 모두 fetch join
	@Query("SELECT p FROM HarmonyRoomPosts p " +
		"LEFT JOIN FETCH p.user " +
		"LEFT JOIN FETCH p.likes " +
		"LEFT JOIN FETCH p.comments " +
		"LEFT JOIN FETCH p.bookmarks " +
		"WHERE p.id = :id")
	Optional<HarmonyRoomPosts> findByIdWithAssociations(@Param("id") UUID id);

	// 좋아요 관계만 fetch join
	@Query("SELECT p FROM HarmonyRoomPosts p LEFT JOIN FETCH p.likes WHERE p.id = :id")
	Optional<HarmonyRoomPosts> findByIdWithLikes(@Param("id") UUID id);

	/**
	 * ✅ 특정 미디어 타입만 조회 (예: "youtube"만)
	 */
	List<HarmonyRoomPosts> findByHarmonyRoomInAndMediaTypeOrderByCreatedAtDesc(
		List<HarmonyRoom> harmonyRooms, String mediaType);
	/**
	 * 하모니룸으로 게시글 목록 조회
	 */
	Optional<HarmonyRoomPosts> findByHarmonyRoom(HarmonyRoom harmonyRoom);
	/**
	 * 특정 하모니룸의 게시글 조회 (최신순)
	 */
	List<HarmonyRoomPosts> findByHarmonyRoomOrderByCreatedAtDesc(HarmonyRoom harmonyRoom);

	/**
	 * 하모니룸 ID로 게시글 목록 조회
	 */
	@Query("SELECT h FROM HarmonyRoomPosts h WHERE h.harmonyRoom.id = :harmonyRoomId")
	Optional<HarmonyRoomPosts> findByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);

	/**
	 * ✅ 특정 하모니룸의 게시글 수 조회
	 */
	Long countByHarmonyRoom(HarmonyRoom harmonyRoom);
	/**
	 * 여러 하모니룸의 게시글 목록 조회
	 */
	@Query("SELECT h FROM HarmonyRoomPosts h WHERE h.harmonyRoom IN :harmonyRooms")
	List<HarmonyRoomPosts> findByHarmonyRoomIn(@Param("harmonyRooms") List<HarmonyRoom> harmonyRooms);
	/**
	 * ✅ 모든 연관 데이터와 함께 조회 (N+1 문제 해결)
	 */
	@Query("SELECT DISTINCT p FROM HarmonyRoomPosts p " +
		"LEFT JOIN FETCH p.user " +
		"LEFT JOIN FETCH p.likes l " +
		"LEFT JOIN FETCH l.user " +
		"LEFT JOIN FETCH p.bookmarks b " +
		"LEFT JOIN FETCH b.user " +
		"LEFT JOIN FETCH p.comments c " +
		"LEFT JOIN FETCH c.user " +
		"WHERE p.harmonyRoom = :harmonyRoom " +
		"ORDER BY p.createdAt DESC")
	List<HarmonyRoomPosts> findByHarmonyRoomWithAllAssociations(@Param("harmonyRoom") HarmonyRoom harmonyRoom);
	// 유저별 게시글 목록 조회 (최신순)
	@Query("SELECT p FROM HarmonyRoomPosts p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
	List<HarmonyRoomPosts> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

	// 하모니룸 ID로 게시글 목록 조회
	@Query("SELECT p FROM HarmonyRoomPosts p WHERE p.harmonyRoom.id = :harmonyId ORDER BY p.createdAt DESC")
	List<HarmonyRoomPosts> findByHarmonyRoomIdOrderByCreatedAtDesc(@Param("harmonyId") UUID harmonyId);

	// 특정 게시글 + 유저 조인 조회 (권한 체크 용)
	@Query("SELECT p FROM HarmonyRoomPosts p JOIN FETCH p.user WHERE p.id = :postId")
	Optional<HarmonyRoomPosts> findByIdWithUser(@Param("postId") UUID postId);
	@Query("SELECT p FROM HarmonyRoomPosts p WHERE p.user.id = :userId " +
		"AND (:currentUserId IS NULL OR :currentUserId NOT MEMBER OF p.hiddenUsers) " +
		"ORDER BY p.createdAt DESC")
	List<HarmonyRoomPosts> findByUserIdOrderByCreatedAtDesc(
		@Param("userId") UUID userId,
		@Param("currentUserId") UUID currentUserId);

}
