package com.osunji.melog.review.repository;

import com.osunji.melog.review.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

	//---------------기본 CRUD-----------------//
	//findById     -> Post테이블에서 id컬럼으로 검색
	//findAll      -> Post테이블의 모든 값 조회
	//findAllById  -> id에 해당하는 모든 게시글 조회
	//existById    ->해당 아이디가 존재하는지 조회
	//count        -> post테이블 전체 개수 조회

	//save         ->id가 중복되면 update/없으면 insert
	//saveAll      ->여러 post한번에 저장

	//delete        ->post 객체 삭제
	//deleteById    -> Id로 게시글 삭제
	//deleteAll     -> 주어진 게시글 여러개 삭제
	//deleteAllById ->아이디가진 게시글 모두 삭제
	//deleteAll()   ->전부 삭제

	//---------------특정 게시글 조회용-----------------//
	/** 기본 postID로 조회 + 유저 정보 추가(파라미터 = postId) - hiddenUser 체크는 Service에서 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :postId")
	Optional<Post> findByIdWithUser(@Param("postId") String postId);

	//---------------피드 필터링 관련 조회-----------------//
	/** 인기피드 - 좋아요 수 기준 내림차순 + hiddenUser 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE :userId IS NULL OR :userId NOT MEMBER OF p.hiddenUser " +
		"ORDER BY SIZE(p.like) DESC")
	List<Post> findPopularPosts(@Param("userId") String userId);

	/** 최신순 게시글 조회 (추천 시스템에서 사용할 기본 데이터) */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE :userId IS NULL OR :userId NOT MEMBER OF p.hiddenUser " +
		"ORDER BY p.createdAt DESC")
	List<Post> findRecentPosts(@Param("userId") String userId);

	/** 팔로우피드 - 팔로우 중인 사람들의 게시글 + hiddenUser 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user u " +
		"WHERE u.id IN :followingUserIds " +
		"AND (:currentUserId IS NULL OR :currentUserId NOT MEMBER OF p.hiddenUser) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findFollowPosts(@Param("followingUserIds") List<String> followingUserIds,
		@Param("currentUserId") String currentUserId);

	//---------------특정 유저 게시글 조회-----------------//
	/** 특정 유저의 모든 게시글 조회 + hiddenUser 제외 */
	@Query("SELECT p FROM Post p WHERE p.user.id = :userId " +
		"AND (:currentUserId IS NULL OR :currentUserId NOT MEMBER OF p.hiddenUser) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId,
		@Param("currentUserId") String currentUserId);

	//---------------검색 및 필터링-----------------//
	/** 제목이나 내용으로 게시글 검색 + 최신순 + hiddenUser 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
		"AND (:userId IS NULL OR :userId NOT MEMBER OF p.hiddenUser) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findByKeywordRecent(@Param("keyword") String keyword,
		@Param("userId") String userId);

	/** 제목/내용으로 검색 (인기순) + hiddenUser 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
		"AND (:userId IS NULL OR :userId NOT MEMBER OF p.hiddenUser) " +
		"ORDER BY SIZE(p.like) DESC")
	List<Post> findByKeywordPopular(@Param("keyword") String keyword,
		@Param("userId") String userId);

	/** 태그로 검색 + hiddenUser 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE p.tags LIKE %:tag% " +
		"AND (:userId IS NULL OR :userId NOT MEMBER OF p.hiddenUser) " +
		"ORDER BY p.createdAt DESC")
	List<Post> findByTag(@Param("tag") String tag,
		@Param("userId") String userId);

	//---------------통계 관련-----------------//
	/** 특정 게시글의 댓글 수 조회 */
	@Query("SELECT COUNT(c) FROM PostComment c WHERE c.post.id = :postId")
	int countComments(@Param("postId") String postId);

	//---------------미디어 관련-----------------//
	/** 인기 미디어 조회 (검색 결과용) + hiddenUser 제외 */
	@Query("SELECT p FROM Post p JOIN FETCH p.user " +
		"WHERE p.mediaLink IS NOT NULL " +
		"AND (:userId IS NULL OR :userId NOT MEMBER OF p.hiddenUser) " +
		"ORDER BY SIZE(p.like) DESC")
	List<Post> findPopularMedia(@Param("userId") String userId);
}