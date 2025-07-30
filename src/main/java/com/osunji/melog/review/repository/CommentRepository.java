package com.osunji.melog.review.repository;

import com.osunji.melog.review.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<PostComment, String> {

	//---------------기본 CRUD-----------------//
	//findById     -> PostComment 테이블에서 id 컬럼으로 검색
	//findAll      -> PostComment 테이블의 모든 값 조회
	//save         -> id가 중복되면 update/없으면 insert
	//delete       -> PostComment 객체 삭제
	//deleteById   -> Id로 댓글 삭제
	//existsById   -> 해당 아이디가 존재하는지 조회
	//count        -> PostComment 테이블 전체 개수 조회

	//---------------특정 게시글의 모든 댓글 조회 -----------------//
	/** 특정 게시글의 모든 댓글 조회 - 계층 구조로 (부모댓글 + 자식댓글) */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.post.id = :postId " +
		"ORDER BY c.createdAt ASC")
	List<PostComment> findPostCommentAll(@Param("postId") String postId);

	//---------------베스트 댓글 조회-----------------//
	/** 특정 게시글의 베스트 댓글 - 좋아요 수가 가장 많은 댓글 */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.post.id = :postId " +
		"ORDER BY SIZE(c.likedUsers) DESC")
	Optional<PostComment> findBestComment(@Param("postId") String postId);

	//---------------댓글 작성 관련-----------------//
	/** 댓글 ID로 조회 + 작성자 정보 포함 (권한 체크용) */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user WHERE c.id = :commentId")
	Optional<PostComment> findCommentbyId(@Param("commentId") String commentId);

	//---------------댓글 좋아요 관련-----------------//
	/** 특정 댓글의 좋아요 수 조회 */
	@Query("SELECT SIZE(c.likedUsers) FROM PostComment c WHERE c.id = :commentId")
	int countCommentLike(@Param("commentId") String commentId);

	/** 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인 */
	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
		"FROM PostComment c JOIN c.likedUsers u " +
		"WHERE c.id = :commentId AND u.id = :userId")
	boolean isCommentLikebyId(@Param("commentId") String commentId, @Param("userId") String userId);

	//---------------통계 관련-----------------//
	/** 특정 게시글의 총 댓글 수 조회 */
	@Query("SELECT COUNT(c) FROM PostComment c WHERE c.post.id = :postId")
	int countCommentByPostId(@Param("postId") String postId);

	/** 특정 사용자가 작성한 댓글 수 조회 */
	@Query("SELECT COUNT(c) FROM PostComment c WHERE c.user.id = :userId")
	int countCommentByuserId(@Param("userId") String userId);

	//---------------부모-자식 댓글 관련-----------------//
	/** 특정 댓글의 대댓글들 조회 */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.parentComment.id = :parentCommentId " +
		"ORDER BY c.createdAt ASC")
	List<PostComment> findChildComments(@Param("parentCommentId") String parentCommentId);

	/** 최상위 댓글들만 조회 (parentComment가 null인 것들) */
	@Query("SELECT c FROM PostComment c JOIN FETCH c.user " +
		"WHERE c.post.id = :postId AND c.parentComment IS NULL " +
		"ORDER BY c.createdAt ASC")
	List<PostComment> findRootCommentsByPostId(@Param("postId") String postId);
}
