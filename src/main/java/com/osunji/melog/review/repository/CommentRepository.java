package com.osunji.melog.review.repository;

import com.osunji.melog.review.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<PostComment, UUID> {  // ✅ String → UUID

	//---------------특정 게시글의 모든 댓글 조회-----------------//
	/** 특정 게시글의 모든 댓글 조회 - 계층 구조로 (부모댓글 + 자식댓글) */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.post.id = :postId " +
		"ORDER BY c.createdAt ASC")
	List<PostComment> findPostCommentAll(@Param("postId") UUID postId);  // ✅ String → UUID

	//---------------베스트 댓글 조회-----------------//
	/** 특정 게시글의 베스트 댓글 - 좋아요 수가 가장 많은 댓글 */
	@Query("SELECT c FROM PostComment c " +
		"WHERE c.post.id = :postId " +
		"ORDER BY SIZE(c.likedUsers) DESC, c.createdAt ASC")
	List<PostComment> findBestComments(@Param("postId") UUID postId);

	// ✅ 단일 결과 대신 리스트로 받아서 첫 번째 선택
	default Optional<PostComment> findBestComment(UUID postId) {
		List<PostComment> comments = findBestComments(postId);
		return comments.isEmpty() ? Optional.empty() : Optional.of(comments.get(0));
	}

	//---------------댓글 작성 관련-----------------//
	/** 댓글 ID로 조회 + 작성자 정보 포함 (권한 체크용) */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user WHERE c.id = :commentId")
	Optional<PostComment> findCommentbyId(@Param("commentId") UUID commentId);  // ✅ String → UUID

	//---------------댓글 좋아요 관련-----------------//
	/** 특정 댓글의 좋아요 수 조회 */
	@Query("SELECT SIZE(c.likedUsers) FROM PostComment c WHERE c.id = :commentId")
	int countCommentLike(@Param("commentId") UUID commentId);  // ✅ String → UUID

	/** 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인 */
	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
		"FROM PostComment c JOIN c.likedUsers u " +
		"WHERE c.id = :commentId AND u.id = :userId")
	boolean isCommentLikebyId(@Param("commentId") UUID commentId, @Param("userId") UUID userId);  // ✅ String → UUID

	//---------------통계 관련-----------------//
	/** 특정 게시글의 총 댓글 수 조회 */
	@Query("SELECT COUNT(c) FROM PostComment c WHERE c.post.id = :postId")
	int countCommentByPostId(@Param("postId") UUID postId);  // ✅ String → UUID

	/** 특정 사용자가 작성한 댓글 수 조회 */
	@Query("SELECT COUNT(c) FROM PostComment c WHERE c.user.id = :userId")
	int countCommentByuserId(@Param("userId") UUID userId);  // ✅ String → UUID

	//---------------부모-자식 댓글 관련-----------------//
	/** 특정 댓글의 대댓글들 조회 */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.parentComment.id = :parentCommentId " +
		"ORDER BY c.createdAt ASC")
	List<PostComment> findChildComments(@Param("parentCommentId") UUID parentCommentId);  // ✅ String → UUID

	/** 최상위 댓글들만 조회 (parentComment가 null인 것들) */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.post.id = :postId AND c.parentComment IS NULL " +
		"ORDER BY c.createdAt ASC")
	List<PostComment> findRootCommentsByPostId(@Param("postId") UUID postId);  // ✅ String → UUID

	//---------------추가 유용한 메서드-----------------//
	/** 특정 댓글과 모든 대댓글 조회 (재귀) */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.id = :commentId OR c.parentComment.id = :commentId " +
		"ORDER BY c.createdAt ASC")
	List<PostComment> findCommentWithReplies(@Param("commentId") UUID commentId);
}
