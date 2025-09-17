package com.osunji.melog.review.repository;

import com.osunji.melog.review.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

	//---------------특정 게시글 조회용-----------------//
	/** 기본 postID로 조회 + 유저 정보 추가 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :postId")
	Optional<Post> findByIdWithUser(@Param("postId") UUID postId);

	//---------------피드 필터링 관련 조회-----------------//
	/** 인기피드 - 좋아요 수 기준 내림차순 + hiddenUsers 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE :userId IS NULL OR :userId NOT MEMBER OF p.hiddenUsers " +
		"ORDER BY SIZE(p.likes) DESC")
	List<Post> findPopularPosts(@Param("userId") UUID userId);

	/** 최신순 게시글 조회 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE :userId IS NULL OR :userId NOT MEMBER OF p.hiddenUsers " +
		"ORDER BY p.createdAt DESC")
	List<Post> findRecentPosts(@Param("userId") UUID userId);

	/** 팔로우피드 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user u " +
		"WHERE u.id IN :followingUserIds " +
		"AND (:currentUserId IS NULL OR :currentUserId NOT MEMBER OF p.hiddenUsers) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findFollowPosts(@Param("followingUserIds") List<UUID> followingUserIds,
		@Param("currentUserId") UUID currentUserId);

	/** 특정 유저의 모든 게시글 조회 + hiddenUsers 제외 */
	@Query("SELECT p FROM Post p WHERE p.user.id = :userId " +
		"AND (:currentUserId IS NULL OR :currentUserId NOT MEMBER OF p.hiddenUsers) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId,
		@Param("currentUserId") UUID currentUserId);

	//---------------미디어 관련-----------------//
	/** 인기 미디어 조회 + hiddenUsers 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE p.mediaUrl IS NOT NULL " +
		"AND (:userId IS NULL OR :userId NOT MEMBER OF p.hiddenUsers) " +
		"ORDER BY SIZE(p.likes) DESC")
	List<Post> findPopularMedia(@Param("userId") UUID userId);

	//---------------ID 리스트로 조회 (Elasticsearch 연동용)-----------------//
	/** ID 리스트로 여러 게시글 조회 */
	List<Post> findAllByIdIn(List<UUID> ids);

	//---------------추가 유용한 메서드-----------------//

	/** 특정 사용자가 작성한 게시글 수 조회 */
	@Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId")
	Long countByUserId(@Param("userId") UUID userId);

	/** 제목, 내용으로만 검색 (태그 검색 제외) */
	@Query("SELECT p FROM Post p WHERE " +
		"LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
		"LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findByTitleContainingOrContentContaining(
		@Param("keyword") String keyword);

	/** YouTube 미디어 조회 - 좋아요 순 정렬 */
	@Query("SELECT p FROM Post p WHERE " +
		"p.mediaType = 'youtube' " +
		"ORDER BY SIZE(p.likes) DESC, p.createdAt DESC")
	List<Post> findPopularMedia(Pageable pageable);

	/** YouTube 미디어만 모두 조회 */
	@Query("SELECT p FROM Post p WHERE p.mediaType = 'youtube'")
	List<Post> findAllYoutubeMedia();

	/**
	 * 사용자의 모든 게시글 조회 (일반 + 하모니룸 게시글 포함)
	 */
	@Query("SELECT p FROM Post p WHERE p.user.id = :userId " +
		"AND (:currentUserId IS NULL OR :currentUserId NOT MEMBER OF p.hiddenUsers) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findAllPostsByUserIdIncludingHarmony(@Param("userId") UUID userId,
		@Param("currentUserId") UUID currentUserId);

}