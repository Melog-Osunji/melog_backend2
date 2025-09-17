package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HarmonyRoomRepository extends JpaRepository<HarmonyRoom, UUID> {

	/**
	 * 소유자로 하모니룸 조회
	 */
	List<HarmonyRoom> findByOwnerOrderByNameAsc(User owner);

	/**
	 * 하모니룸 이름으로 검색
	 */
	@Query("SELECT h FROM HarmonyRoom h WHERE " +
		"LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
		"ORDER BY h.createdAt DESC")
	List<HarmonyRoom> findByNameContaining(@Param("keyword") String keyword);

	/**
	 * 공개 하모니룸 조회 (추천용)
	 */
	@Query("SELECT h FROM HarmonyRoom h WHERE h.isPrivate = false ORDER BY h.bookMarkNum DESC, h.createdAt DESC")
	List<HarmonyRoom> findPublicHarmonyRoomsForRecommend();

	/**
	 * 북마크 순 랭킹 조회
	 */
	@Query("SELECT COUNT(h) + 1 FROM HarmonyRoom h WHERE h.bookMarkNum > :bookMarkNum")
	Long findRankingByBookMarkNum(@Param("bookMarkNum") Integer bookMarkNum);
}
