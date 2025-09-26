package com.osunji.melog.review.service;

import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.repository.CommentRepository;
import com.osunji.melog.review.mapper.CommentMapper;
import com.osunji.melog.review.dto.request.CommentRequest;
import com.osunji.melog.review.dto.response.CommentResponse;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final AuthHelper authHelper;
	private final CommentMapper commentMapper;

	/** 특정 게시글의 모든 댓글 GET (API 16번) */
	@Transactional(readOnly = true)
	public ApiMessage<CommentResponse.All> getAllComments(String postIdStr) {
		try {
			// 1. postId 변환 및 게시글 존재 검사
			UUID postId = UUID.fromString(postIdStr);
			postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			// 2. 최상위 댓글들 조회 (대댓글은 매퍼에서 재귀로 처리)
			List<PostComment> comments = commentRepository.findRootCommentsByPostId(postId);

			// 3. DTO 변환
			List<CommentResponse.CommentData> commentDataList = comments.stream()
				.map(commentMapper::toCommentData)
				.toList();

			CommentResponse.All response = CommentResponse.All.builder()
				.comments(commentDataList)
				.build();

			return ApiMessage.success(200, "게시글 모든 댓글 조회 성공", response);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 조회 실패: " + e.getMessage());
		}
	}

	/** 특정 게시글의 베스트 댓글 GET (API 17번) */
	@Transactional(readOnly = true)
	public ApiMessage<CommentResponse.Best> getBestComment(String postIdStr) {
		try {
			// 1. postId 변환 및 베스트 댓글 조회
			UUID postId = UUID.fromString(postIdStr);
			Optional<PostComment> bestOpt = commentRepository.findBestComment(postId);

			if (bestOpt.isEmpty()) {
				return ApiMessage.success(200, "베스트 댓글이 없습니다.", null);
			}

			// 2. DTO 변환 후 반환
			CommentResponse.Best bestDto = commentMapper.toBestComment(bestOpt.get());
			return ApiMessage.success(200, "베스트 댓글 조회 성공", bestDto);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "베스트 댓글 조회 실패: " + e.getMessage());
		}
	}

	/** 댓글 작성 (API 28번) */
	public ApiMessage createComment(String postIdStr, CommentRequest.Create request, String authHeader) {
		try {
			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 2. 사용자 및 게시글 조회
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			UUID postId = UUID.fromString(postIdStr);
			Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			// 3. 부모 댓글 확인 (대댓글인 경우)
			PostComment parentComment = null;
			if (request.getResponseTo() != null) {
				UUID parentId = UUID.fromString(request.getResponseTo());
				parentComment = commentRepository.findById(parentId)
					.orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
			}

			// 4. 댓글 생성 및 저장
			PostComment comment = (parentComment == null)
				? PostComment.createComment(user, post, request.getContent())
				: PostComment.createReply(user, post, request.getContent(), parentComment);

			commentRepository.save(comment);

			return ApiMessage.success(201, "댓글이 성공적으로 작성되었습니다.", null);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 작성 실패: " + e.getMessage());
		}
	}

	/** 댓글 삭제 (API 26번) */
	public ApiMessage deleteComment(String postIdStr, String commentIdStr, String authHeader) {
		try {
			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 2. 댓글 조회 및 작성자 권한 체크
			UUID commentId = UUID.fromString(commentIdStr);
			PostComment comment = commentRepository.findCommentbyId(commentId)
				.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

			if (!comment.getUser().getId().equals(userId)) {
				return ApiMessage.fail(403, "삭제 권한이 없습니다.");
			}

			// 3. 댓글 삭제
			commentRepository.delete(comment);

			return ApiMessage.success(200, "댓글이 성공적으로 삭제되었습니다.", null);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 삭제 실패: " + e.getMessage());
		}
	}

	/** 댓글 좋아요/취소 (API 29번) */
	public ApiMessage<Void> likeOrUnlikeComment(String postIdStr, String commentIdStr, String authHeader) {
		try {
			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 2. 사용자 및 댓글 조회
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			UUID commentId = UUID.fromString(commentIdStr);
			PostComment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

			// 3. 좋아요 토글
			if (comment.isLikedBy(user)) {
				comment.removeLike(user);
			} else {
				comment.addLike(user);
			}

			commentRepository.save(comment);

			return ApiMessage.success(200, "댓글 좋아요/취소 완료", null);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 좋아요 처리 실패: " + e.getMessage());
		}
	}
}
