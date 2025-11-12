package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyPostComment;
import com.osunji.melog.harmony.entity.HarmonyRoomPosts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HarmonyCommentRepository extends JpaRepository<HarmonyPostComment, UUID> {

	//---------------특정 하모니룸 게시글의 모든 댓글 조회-----------------//
	/**
	 * 특정 하모니룸 게시글의 모든 댓글 조회 - 계층 구조로 (부모댓글 + 자식댓글)
	 */
	@Query("SELECT c FROM HarmonyPostComment c JOIN FETCH c.user " +
		"WHERE c.harmonyPost.id = :harmonyPostId " +
		"ORDER BY c.createdAt ASC")
	List<HarmonyPostComment> findHarmonyPostCommentAll(@Param("harmonyPostId") UUID harmonyPostId);

	//---------------베스트 댓글 조회-----------------//
	/**
	 * 특정 하모니룸 게시글의 베스트 댓글 - 좋아요 수가 가장 많은 댓글
	 */
	@Query("SELECT c FROM HarmonyPostComment c " +
		"WHERE c.harmonyPost.id = :harmonyPostId " +
		"ORDER BY SIZE(c.likedUsers) DESC, c.createdAt ASC")
	List<HarmonyPostComment> findBestComments(@Param("harmonyPostId") UUID harmonyPostId);

	/**
	 * 베스트 댓글 단일 조회
	 */
	default Optional<HarmonyPostComment> findBestComment(UUID harmonyPostId) {
		List<HarmonyPostComment> comments = findBestComments(harmonyPostId);
		return comments.isEmpty() ? Optional.empty() : Optional.of(comments.get(0));
	}

	//---------------댓글 작성 관련-----------------//
	/**
	 * 댓글 ID로 조회 + 작성자 정보 포함 (권한 체크용)
	 */
	@Query("SELECT c FROM HarmonyPostComment c JOIN FETCH c.user WHERE c.id = :commentId")
	Optional<HarmonyPostComment> findHarmonyCommentById(@Param("commentId") UUID commentId);

	//---------------댓글 좋아요 관련-----------------//
	/**
	 * 특정 댓글의 좋아요 수 조회
	 */
	@Query("SELECT SIZE(c.likedUsers) FROM HarmonyPostComment c WHERE c.id = :commentId")
	int countHarmonyCommentLike(@Param("commentId") UUID commentId);

	/**
	 * 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인
	 */
	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
		"FROM HarmonyPostComment c JOIN c.likedUsers u " +
		"WHERE c.id = :commentId AND u.id = :userId")
	boolean isHarmonyCommentLikedBy(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

	//---------------통계 관련-----------------//
	/**
	 * 특정 하모니룸 게시글의 총 댓글 수 조회
	 */
	@Query("SELECT COUNT(c) FROM HarmonyPostComment c WHERE c.harmonyPost.id = :harmonyPostId")
	int countCommentByHarmonyPostId(@Param("harmonyPostId") UUID harmonyPostId);

	/**
	 * 특정 사용자가 작성한 하모니룸 댓글 수 조회
	 */
	@Query("SELECT COUNT(c) FROM HarmonyPostComment c WHERE c.user.id = :userId")
	int countHarmonyCommentByUserId(@Param("userId") UUID userId);

	//---------------부모-자식 댓글 관련-----------------//
	/**
	 * 특정 댓글의 대댓글들 조회
	 */
	@Query("SELECT c FROM HarmonyPostComment c JOIN FETCH c.user " +
		"WHERE c.parentComment.id = :parentCommentId " +
		"ORDER BY c.createdAt ASC")
	List<HarmonyPostComment> findChildComments(@Param("parentCommentId") UUID parentCommentId);

	/**
	 * 최상위 댓글들만 조회 (parentComment가 null인 것들)
	 */
	@Query("SELECT c FROM HarmonyPostComment c JOIN FETCH c.user " +
		"WHERE c.harmonyPost.id = :harmonyPostId AND c.parentComment IS NULL " +
		"ORDER BY c.createdAt ASC")
	List<HarmonyPostComment> findRootCommentsByHarmonyPostId(@Param("harmonyPostId") UUID harmonyPostId);

	/**
	 * 특정 댓글과 모든 대댓글 조회 (재귀)
	 */
	@Query("SELECT c FROM HarmonyPostComment c JOIN FETCH c.user " +
		"WHERE c.id = :commentId OR c.parentComment.id = :commentId " +
		"ORDER BY c.createdAt ASC")
	List<HarmonyPostComment> findHarmonyCommentWithReplies(@Param("commentId") UUID commentId);

	/**
	 *특정 하모니룸의 모든 게시글에서 댓글 조회
	 */
	@Query("SELECT c FROM HarmonyPostComment c JOIN FETCH c.user " +
		"WHERE c.harmonyPost.harmonyRoom.id = :harmonyRoomId " +
		"ORDER BY c.createdAt DESC")
	List<HarmonyPostComment> findCommentsByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);
	/**
	 * 여러 게시글의 베스트 댓글을 한 번에 조회 (배치 처리)
	 */
	@Query("SELECT c FROM HarmonyPostComment c " +
		"JOIN FETCH c.user " +
		"WHERE c.harmonyPost.id IN :postIds " +
		"AND c.id IN (" +
		"    SELECT c2.id FROM HarmonyPostComment c2 " +
		"    WHERE c2.harmonyPost.id = c.harmonyPost.id " +
		"    ORDER BY SIZE(c2.likedUsers) DESC, c2.createdAt ASC " +
		"    LIMIT 1" +
		")")
	List<HarmonyPostComment> findBestCommentsForMultiplePosts(@Param("postIds") List<UUID> postIds);

	/**
	 * 더 간단한 버전 (JPQL LIMIT이 지원되지 않는 경우)
	 */
	@Query("SELECT c FROM HarmonyPostComment c " +
		"JOIN FETCH c.user " +
		"WHERE c.harmonyPost.id IN :postIds " +
		"ORDER BY c.harmonyPost.id, SIZE(c.likedUsers) DESC, c.createdAt ASC")
	List<HarmonyPostComment> findAllCommentsForPosts(@Param("postIds") List<UUID> postIds);


	// 특정 게시글 최상위 댓글 조회(대댓글 제외)
	@Query("SELECT c FROM HarmonyPostComment c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt DESC")
	List<HarmonyPostComment> findRootCommentsByPostId(@Param("postId") UUID postId);


}