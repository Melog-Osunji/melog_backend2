package com.osunji.melog.review.service;

//global
import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;

// entity
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;

// repo
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.repository.CommentRepository;
//mapper
import com.osunji.melog.review.mapper.CommentMapper;


//dto
import com.osunji.melog.review.dto.request.CommentRequest;
import com.osunji.melog.review.dto.response.CommentResponse;

//user
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final AuthHelper authHelper;
	private final CommentMapper commentMapper;


	/**   특정 게시글의 모든 댓글Get (API 16번)   path(postId)       */
	@Transactional(readOnly = true)
	public ApiMessage<CommentResponse.All> getAllComments(String postId) {
		// 1. 게시글 존재 검사
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		// 2. 해당 게시글의 루트(top-level) 댓글만 조회 (부모가 없는 것들)
		List<PostComment> comments = commentRepository.findRootCommentsByPostId(postId);

		// 3. DTO 변환 (대댓글은 매퍼에서 재귀로 처리)
		List<CommentResponse.CommentData> commentDataList = comments.stream()
			.map(commentMapper::toCommentData)
			.toList();

		CommentResponse.All response = CommentResponse.All.builder()
			.comments(commentDataList)
			.build();

		return ApiMessage.success(200, "게시글 모든 댓글 조회 성공", response);
	}




	/**   특정 게시글의 베스트 댓글Get (API 17번)   path(postId)       */
	@Transactional(readOnly = true)
	public ApiMessage<CommentResponse.Best> getBestComment(String postId) {
		// 1. 베스트 댓글 조회 (좋아요수 가장 많은 기준)
		Optional<PostComment> bestOpt = commentRepository.findBestComment(postId);
		// 베댓없을시 예외 처리
		if (bestOpt.isEmpty()) {
			return ApiMessage.success(200, "베스트 댓글이 없습니다.", null);
		}
		// 2. entity를 매퍼로 dto로 변환 후 반환
		CommentResponse.Best bestDto = commentMapper.toBestComment(bestOpt.get());
		return ApiMessage.success(200, "베스트 댓글 조회 성공", bestDto);
	}




	/**   댓글 Delete (API 26번)   path(postId/commentId), header(token)    */
	public ApiMessage<Void> deleteComment(String postId, String commentId, String authHeader) {
		// 1. 토큰에서 userId 추출
		String userId = null;
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (Exception e) {
			return ApiMessage.fail(401, e.getMessage());
		}

		// 2. 게시글 존재 확인
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		// 3. 댓글 존재 확인, 작성자 권한 체크용 댓글 조회
		PostComment comment = commentRepository.findCommentbyId(commentId)
			.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

		// 4. 작성자 권한 체크
		if (!comment.getUser().getId().equals(userId)) {
			return ApiMessage.fail(403, "삭제 권한이 없습니다.");
		}

		// 5. 댓글 삭제
		commentRepository.delete(comment);

		return ApiMessage.success(200, "댓글이 성공적으로 삭제되었습니다.", null);
	}


	/**      28. 댓글 작성 path(postId), header(token), body(content/responseTo)     */
	public ApiMessage<Void> createComment(String postId, CommentRequest.Create request, String authHeader) {
		// 1. 토큰에서 userId 추출
		String userId = null;
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (Exception e) {
			return ApiMessage.fail(401, e.getMessage());
		}

		// 2. 유저 존재 확인
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
		// 3. 게시글 존재 확인
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		// 4. 대댓글이면 responseTo가 있음, 부모댓글이 존재하는지 확인
		PostComment parent = null;
		if (request.getResponseTo() != null) {
			parent = commentRepository.findById(request.getResponseTo())
				.orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
		}
		PostComment comment = commentMapper.toEntity(request, user, post, parent);
		commentRepository.save(comment);

		return ApiMessage.success(200, "댓글이 성공적으로 작성되었습니다.", null);
	}

	/**      29. 댓글 좋아요 생성/취소 (path: postId/commentId, header: token)   */
	public ApiMessage<Void> likeOrUnlikeComment(String postId, String commentId, String authHeader) {
		// 1. 토큰에서 userId 추출
		String userId;
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (IllegalStateException | IllegalArgumentException e) {
			return ApiMessage.fail(401, e.getMessage());
		}
		// 2. 유저 존재 확인
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
		// 3. 댓글 존재 확인
		PostComment comment = commentRepository.findById(commentId)
			.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
		// 4. 라이크 유저 있으면 삭제 아니면 생성
		if (comment.getLikedUsers().contains(user)) {
			comment.getLikedUsers().remove(user);
		} else {
			comment.getLikedUsers().add(user);
		}

		commentRepository.save(comment);

		return ApiMessage.success(200, "댓글 좋아요/취소 완료", null);
	}

}
