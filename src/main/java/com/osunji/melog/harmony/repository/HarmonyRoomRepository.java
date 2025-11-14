package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.entity.HarmonyRoomMembers;
import com.osunji.melog.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
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
	 * ✅ 실제 북마크 수 기준 랭킹 조회 (기존 메서드 대체)
	 */
	@Query("SELECT COUNT(hr) + 1 FROM HarmonyRoom hr " +
		"WHERE (SELECT COUNT(hrb) FROM HarmonyRoomBookmark hrb WHERE hrb.harmonyRoom = hr) > :bookmarkCount")
	Long findRankingByActualBookMarkCount(@Param("bookmarkCount") Long bookmarkCount);

	/**
	 * ✅ 하모니룸별 실제 북마크 수와 함께 조회 (성능 최적화용)
	 */
	@Query("SELECT hr.id, hr.name, COUNT(hrb) as bookmarkCount " +
		"FROM HarmonyRoom hr " +
		"LEFT JOIN HarmonyRoomBookmark hrb ON hrb.harmonyRoom = hr " +
		"GROUP BY hr.id, hr.name " +
		"ORDER BY COUNT(hrb) DESC")
	List<Object[]> findHarmonyRoomsWithBookmarkCount();

	/**
	 * ✅ 북마크 수 상위 하모니룸 조회 (인기순)
	 */
	@Query("SELECT hr FROM HarmonyRoom hr " +
		"LEFT JOIN HarmonyRoomBookmark hrb ON hrb.harmonyRoom = hr " +
		"WHERE hr.isPrivate = false " +
		"GROUP BY hr " +
		"ORDER BY COUNT(hrb) DESC")
	List<HarmonyRoom> findTopHarmonyRoomsByBookmarkCount(Pageable pageable);
	/**
	 * 북마크 순 랭킹 조회
	 */
	@Query("SELECT COUNT(h) + 1 FROM HarmonyRoom h WHERE h.bookMarkNum > :bookMarkNum")
	Long findRankingByBookMarkNum(@Param("bookMarkNum") Integer bookMarkNum);

	List<HarmonyRoom> findByOwner_Id(UUID ownerId);

	/**
	 * ✅ 북마크 수 기준 랭킹 조회
	 */
	@Query("SELECT COUNT(h) + 1 FROM HarmonyRoom h WHERE h.bookMarkNum > :bookmarkCount")
	Long findRankingByBookMarkCount(@Param("bookmarkCount") Long bookmarkCount);


	@Query("""
    SELECT h FROM HarmonyRoom h
    WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(h.intro) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
	List<HarmonyRoom> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
        select hr
        from HarmonyRoom hr
        where hr.owner.id = :userId
           or exists (
                select 1
                from HarmonyRoomMembers m
                where m.harmonyRoom = hr
                  and m.user.id = :userId
           )
        order by hr.createdAt desc
        """)
    List<HarmonyRoom> findAllJoinedOrOwned(@Param("userId") UUID userId);


    // 내가 어떤 룸에 어떤 역할로 속해 있는지 전부 조회
    @Query("""
    select m
    from HarmonyRoomMembers m
    join m.harmonyRoom hr
    where m.user.id = :userId
    """)
    List<HarmonyRoomMembers> findByUserIdWithRoom(@Param("userId") UUID userId);





}
