package com.osunji.melog.harmony.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.osunji.melog.harmony.entity.HarmonyPostBookmark;
import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.entity.HarmonyRoomBookmark;
import com.osunji.melog.user.domain.User;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface HarmonyRoomBookmarkRepository extends JpaRepository<HarmonyRoomBookmark, UUID> {

	/**
	 * 사용자와 하모니룸으로 북마크 존재 여부 확인
	 */
	boolean existsByUserAndHarmonyRoom(User user, HarmonyRoom harmonyRoom);

	/**
	 * 사용자와 하모니룸으로 북마크 조회
	 */
	Optional<HarmonyRoomBookmark> findByUserAndHarmonyRoom(User user, HarmonyRoom harmonyRoom);
	@Query("SELECT b FROM HarmonyPostBookmark b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
	List<HarmonyPostBookmark> findBookmarkAllByUserId(UUID userId);

	/**
	 * 특정 하모니룸의 북마크 수 조회
	 */
	Long countByHarmonyRoom(HarmonyRoom harmonyRoom);
    @Query("SELECT hb FROM HarmonyRoomBookmark hb WHERE hb.user.id = :userId AND hb.harmonyRoom.id = :harmonyRoomId")
	Optional<HarmonyRoomBookmark> findByUserIdAndHarmonyRoomId(@Param("userId") UUID userId,
		@Param("harmonyRoomId") UUID harmonyRoomId);

	@Query("SELECT hrb FROM HarmonyRoomBookmark hrb " +
		"JOIN FETCH hrb.harmonyRoom " +  // ✅ 하모니룸 정보도 함께 가져오기
		"WHERE hrb.user.id = :userId " +
		"ORDER BY hrb.bookmarkedAt DESC")
	List<HarmonyRoomBookmark> findByUserId(@Param("userId") UUID userId);
	/**
	 * 하모니룸별 모든 북마크 조회 (삭제용)
	 */
	@Query("SELECT hrb FROM HarmonyRoomBookmark hrb WHERE hrb.harmonyRoom.id = :harmonyRoomId")
	List<HarmonyRoomBookmark> findByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);

	/**
	 * 하모니룸별 모든 북마크 삭제
	 */
	@Modifying
	@Query("DELETE FROM HarmonyRoomBookmark hrb WHERE hrb.harmonyRoom.id = :harmonyRoomId")
	void deleteByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);
	@Query("SELECT COUNT(hb) FROM HarmonyRoomBookmark hb WHERE hb.harmonyRoom.id = :harmonyRoomId")
	Long countByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);
	boolean existsByHarmonyRoom_IdAndUser_Id(UUID harmonyRoomId, UUID userId);


}
