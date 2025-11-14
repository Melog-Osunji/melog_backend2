package com.osunji.melog.review.repository;

import com.osunji.melog.review.entity.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<PostBookmark, PostBookmark.PostBookmarkId> {

	//---------------특정 유저의 북마크 조회-----------------//
	/** 특정 사용자의 모든 북마크 게시글 조회 - postId, title, createdAt만 */
	@Query("SELECT pb FROM PostBookmark pb JOIN FETCH pb.post p " +
		"WHERE pb.user.id = :userId " +
		"ORDER BY pb.createdAt DESC")
	List<PostBookmark> findBookmarkAllByuserId(@Param("userId") UUID userId);  // ✅ String → UUID

	//---------------북마크 생성/삭제 관련-----------------//
	/** 특정 사용자의 특정 게시글 북마크 존재 엔티티반환 */
	@Query("SELECT pb FROM PostBookmark pb " +
		"WHERE pb.user.id = :userId AND pb.post.id = :postId")
	Optional<PostBookmark> isBookmarkByuserIdAndpostId(@Param("userId") UUID userId,  // ✅ String → UUID
		@Param("postId") UUID postId);  // ✅ String → UUID

	/** 북마크 삭제 - 사용자ID와 게시글ID로 */
	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM PostBookmark pb " +
		"WHERE pb.user.id = :userId AND pb.post.id = :postId")
	void deleteByUserIdAndPostId(@Param("userId") UUID userId,  // ✅ String → UUID
		@Param("postId") UUID postId);  // ✅ String → UUID

	/** 북마크 존재 여부 확인 */
	@Query("SELECT CASE WHEN COUNT(pb) > 0 THEN true ELSE false END " +
		"FROM PostBookmark pb " +
		"WHERE pb.user.id = :userId AND pb.post.id = :postId")
	boolean existsByUserIdAndPostId(@Param("userId") UUID userId,  // ✅ String → UUID
		@Param("postId") UUID postId);  // ✅ String → UUID

	//---------------통계 관련-----------------//
	/** 특정 게시글이 북마크된 총 횟수 조회 */
	@Query("SELECT COUNT(pb) FROM PostBookmark pb WHERE pb.post.id = :postId")
	int countBookmarkByPostId(@Param("postId") UUID postId);  // ✅ String → UUID

	//---------------북마크한 게시글 정보 포함 조회-----------------//
	/** 특정 사용자의 북마크와 게시글 상세 정보 함께 조회 */
	@Query("SELECT pb FROM PostBookmark pb " +
		"JOIN FETCH pb.post p " +
		"JOIN FETCH p.user " +
		"WHERE pb.user.id = :userId " +
		"ORDER BY pb.createdAt DESC")
	List<PostBookmark> findByUserIdWithPostDetails(@Param("userId") UUID userId);  // ✅ String → UUID

	//---------------인기 북마크 게시글 조회-----------------//
	/** 가장 많이 북마크된 게시글들 조회 (인기 게시글 참고용) */
	@Query("SELECT pb.post.id, COUNT(pb) as bookmarkCount " +
		"FROM PostBookmark pb " +
		"GROUP BY pb.post.id " +
		"ORDER BY COUNT(pb) DESC")
	List<Object[]> findMostBookmarkedPosts();

	//---------------추가 유용한 메서드-----------------//
	/** 특정 사용자의 북마크 개수 조회 */
	@Query("SELECT COUNT(pb) FROM PostBookmark pb WHERE pb.user.id = :userId")
	Long countByUserId(@Param("userId") UUID userId);






}
