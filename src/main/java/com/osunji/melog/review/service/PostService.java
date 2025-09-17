package com.osunji.melog.review.service;
import com.osunji.melog.global.util.JWTUtil;

import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.repository.CommentRepository;
import com.osunji.melog.review.mapper.PostMapper;
import com.osunji.melog.review.dto.request.PostRequest;
import com.osunji.melog.review.dto.response.PostResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final AuthHelper authHelper;
	private final PostMapper postMapper;
	private final JWTUtil jwtUtil;

	//---------------게시글 CRUD-----------------//
	/** 게시글 CREATE - UUID 반환하도록 수정 */
	public ApiMessage<String> createPost(PostRequest.Create request, String authHeader) {
		try {
			System.out.println("🔥 요청 데이터 확인:");
			System.out.println("  - 제목: '" + request.getTitle() + "'");
			System.out.println("  - 내용: '" + request.getContent() + "'");
			System.out.println("  - 미디어타입: '" + request.getMediaType() + "'");
			System.out.println("  - 미디어URL: '" + request.getMediaUrl() + "'");
			System.out.println("  - 태그: " + request.getTags());

			UUID userId = authHelper.authHelperAsUUID(authHeader);

			Optional<User> userOpt;
			try {
				userOpt = userRepository.findByUUID(userId);
			} catch (Exception e) {
				userOpt = userRepository.findByIdString(userId.toString());
			}

			User user = userOpt.orElseThrow(() ->
				new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			System.out.println("✅ 사용자 확인: " + user.getId());

			// Post 생성 시 디버깅
			System.out.println("📋 Post 생성 시작...");
			Post post = Post.create(user, request);
			System.out.println("✅ Post 생성 완료: " + post.getId());

			System.out.println("📋 Post 저장 시작...");
			Post savedPost = postRepository.save(post);
			System.out.println("✅ Post 저장 완료: " + savedPost.getId());

			// ✅ 게시글 ID를 String으로 변환
			String postIdStr;
			try {
				postIdStr = savedPost.getId().toString();
				System.out.println("✅ postId 변환 성공: " + postIdStr);
			} catch (Exception e) {
				System.out.println("❌ postId 변환 실패: " + e.getMessage());
				throw new RuntimeException("게시글 ID 변환 실패: " + e.getMessage());
			}

			System.out.println("📋 응답 생성 시작...");
			System.out.println("✅ 최종 게시글 ID: " + postIdStr);

			// ✅ 게시글 ID를 data로 반환 (HarmonyService에서 사용)
			return ApiMessage.success(201, "게시글 생성 성공", postIdStr);

		} catch (IllegalArgumentException e) {
			System.out.println("❌ UUID 변환 오류: " + e.getMessage());
			return ApiMessage.fail(400, "UUID 변환 오류: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("💥 예외 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
			e.printStackTrace();
			return ApiMessage.fail(500, "서버 오류: " + e.getMessage());
		}
	}



	/** 게시글 GET (API 15번) */
	@Transactional(readOnly = true)
	public ApiMessage<PostResponse.Single> getPost(String postIdStr, String authHeader) {
		try {
			// 1. postId String → UUID 변환
			UUID postId = UUID.fromString(postIdStr);

			// 2. 토큰에서 userId 추출 (선택적)
			UUID userId = null;
			try {
				userId = authHelper.authHelperAsUUID(authHeader);
			} catch (Exception e) {
				// 비로그인 사용자도 조회 가능하므로 무시
			}

			// 3. Post 엔티티 조회
			Post post = postRepository.findByIdWithUser(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			// 4. 숨김 처리된 게시글 체크
			if (userId != null && post.isHiddenBy(userRepository.findById(userId).orElse(null))) {
				return ApiMessage.fail(403, "숨김 처리된 게시글입니다.");
			}

			// 5. 베스트 댓글 및 댓글 개수 조회
			Optional<PostComment> bestCommentOpt = commentRepository.findBestComment(postId);
			PostComment bestComment = bestCommentOpt.orElse(null);
			int commentCount = commentRepository.countCommentByPostId(postId);

			// 6. DTO 변환
			PostResponse.Single responseData = postMapper.toSingle(post, bestComment, commentCount);

			return ApiMessage.success(200, "게시글 조회 성공", responseData);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, "잘못된 게시글 ID 형식입니다.");
		} catch (Exception e) {
			return ApiMessage.fail(500, "게시글 조회 실패: " + e.getMessage());
		}
	}

	/** 게시글 UPDATE (API 30번) */
	@Transactional
	public ApiMessage updatePost(String postIdStr, PostRequest.Update request, String authHeader) {
		try {
			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 2. postId 변환 및 조회
			UUID postId = UUID.fromString(postIdStr);
			Post post = postRepository.findByIdWithUser(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			// 3. 작성자 권한 체크
			if (!post.getUser().getId().equals(userId)) {
				return ApiMessage.fail(403, "수정 권한이 없습니다.");
			}

			// 4. 게시글 업데이트
			post.update(request);
			// postRepository.save(post); // @Transactional이므로 자동 저장

			return ApiMessage.success(200, "게시글이 성공적으로 수정되었습니다.", null);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "게시글 수정 실패: " + e.getMessage());
		}
	}

	/** 게시글 DELETE (API 24번) */
	@Transactional
	public ApiMessage deletePost(String postIdStr, String authHeader) {
		try {
			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 2. postId 변환 및 조회
			UUID postId = UUID.fromString(postIdStr);
			Post post = postRepository.findByIdWithUser(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			// 3. 작성자 권한 체크
			if (!post.getUser().getId().equals(userId)) {
				return ApiMessage.fail(403, "삭제 권한이 없습니다.");
			}

			// 4. 게시글 삭제
			postRepository.delete(post);

			return ApiMessage.success(200, "게시글이 성공적으로 삭제되었습니다.", null);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "게시글 삭제 실패: " + e.getMessage());
		}
	}

	/** 게시글 좋아요/취소 (API - 게시글 좋아요) */
	@Transactional
	public ApiMessage likeOrUnlikePost(String postIdStr, String authHeader) {
		try {
			System.out.println("❤️ 게시글 좋아요/취소 시작");

			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			System.out.println("  - 사용자 ID: " + userId);

			// 2. 사용자 조회
			User user;
			try {
				Optional<User> userOpt = userRepository.findByUUID(userId);
				if (userOpt.isEmpty()) {
					userOpt = userRepository.findByIdString(userId.toString());
				}
				user = userOpt.orElseThrow(() ->
					new IllegalArgumentException("사용자를 찾을 수 없습니다."));
			} catch (Exception e) {
				System.out.println("❌ 사용자 조회 실패: " + e.getMessage());
				throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
			}

			// 3. 게시글 조회
			UUID postId = UUID.fromString(postIdStr);
			Post post = postRepository.findByIdWithUser(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			System.out.println("  - 게시글 ID: " + postId);
			System.out.println("  - 현재 좋아요 수: " + post.getLikeCount());

			// 4. 좋아요 상태 확인 및 토글
			boolean wasLiked = post.isLikedBy(user);
			System.out.println("  - 기존 좋아요 여부: " + wasLiked);

			if (wasLiked) {
				// 좋아요 취소
				post.removeLike(user);
				System.out.println("  - 좋아요 취소됨");
			} else {
				// 좋아요 추가
				post.addLike(user);
				System.out.println("  - 좋아요 추가됨");
			}

			// 5. 저장
			postRepository.save(post);

			int newLikeCount = post.getLikeCount();
			System.out.println("  - 새로운 좋아요 수: " + newLikeCount);
			System.out.println("✅ 좋아요 처리 완료");

			// 6. 응답 메시지
			String action = wasLiked ? "취소" : "추가";
			String message = String.format("좋아요가 %s되었습니다. (현재 %d개)", action, newLikeCount);

			return ApiMessage.success(200, message, Map.of(
				"liked", !wasLiked,
				"likeCount", newLikeCount,
				"action", action
			));

		} catch (IllegalArgumentException e) {
			System.out.println("❌ 인자 오류: " + e.getMessage());
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			System.out.println("❌ 인증 오류: " + e.getMessage());
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			System.out.println("💥 좋아요 처리 오류: " + e.getMessage());
			e.printStackTrace();
			return ApiMessage.fail(500, "좋아요 처리 실패: " + e.getMessage());
		}
	}


	//---------------피드 조회-----------------//

	/** 인기 피드 GET (API 19번) */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.FeedList> getPopularPosts(String authHeader) {
		try {
			// 1. 토큰에서 userId 추출 (선택적)
			UUID userId = null;
			try {
				userId = authHelper.authHelperAsUUID(authHeader);
			} catch (Exception e) {
				// 비로그인 사용자도 조회 가능
			}

			// 2. 인기 게시글 50개 조회
			List<Post> posts = postRepository.findPopularPosts(userId)
				.stream().limit(50).toList();

			// 3. DTO 변환
			List<FilterPostResponse.FeedPostData> feedPostList = posts.stream()
				.map(post -> {
					Optional<PostComment> bestCommentOpt = commentRepository.findBestComment(post.getId());
					PostComment bestComment = bestCommentOpt.orElse(null);
					int commentCount = commentRepository.countCommentByPostId(post.getId());
					return postMapper.toFeedPostData(post, bestComment, commentCount);
				})
				.toList();

			FilterPostResponse.FeedList feedList = FilterPostResponse.FeedList.builder()
				.results(feedPostList)
				.build();

			return ApiMessage.success(200, "인기 피드 조회 성공", feedList);

		} catch (Exception e) {
			return ApiMessage.fail(500, "인기 피드 조회 실패: " + e.getMessage());
		}
	}

	/** 팔로우 피드 GET (API 20번) - 디버깅 버전 */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.FeedList> getFollowPosts(String authHeader) {
		try {
			System.out.println("🔥 ===== 팔로우 피드 조회 시작 =====");
			System.out.println("authHeader: " + (authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "null"));

			// 1. 토큰에서 userId 추출 (필수)
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			System.out.println("✅ 사용자 ID 추출: " + userId);

			// 2. 팔로잉 유저 ID 리스트 조회
			List<UUID> followingUserIds = getFollowingUserIds(userId);
			System.out.println("📋 팔로잉 사용자 수: " + followingUserIds.size());
			System.out.println("  - 팔로잉 ID 목록: " + followingUserIds);

			// ✅ 임시로 현재 사용자 자신의 게시글도 포함 (테스트용)
			if (followingUserIds.isEmpty()) {
				System.out.println("⚠️ 팔로잉 사용자가 없음 - 임시로 본인 포함");
				followingUserIds = List.of(userId);  // 본인 게시글이라도 표시
			}

			// 3. 팔로우 피드 조회
			System.out.println("📋 게시글 조회 시작...");
			List<Post> posts = postRepository.findFollowPosts(followingUserIds, userId)
				.stream().limit(50).toList();
			System.out.println("  - 조회된 게시글 수: " + posts.size());

			if (posts.isEmpty()) {
				System.out.println("⚠️ 조회된 게시글이 없음");
				// 전체 게시글 수 확인
				List<Post> allPosts = postRepository.findAll();
				System.out.println("  - DB 전체 게시글 수: " + allPosts.size());
			}

			// 4. DTO 변환
			System.out.println("📋 DTO 변환 시작...");
			List<FilterPostResponse.FeedPostData> feedPostList = posts.stream()
				.map(post -> {
					try {
						System.out.println("  변환 중: " + post.getId() + " - " + post.getTitle());
						Optional<PostComment> bestCommentOpt = commentRepository.findBestComment(post.getId());
						PostComment bestComment = bestCommentOpt.orElse(null);
						int commentCount = commentRepository.countCommentByPostId(post.getId());
						return postMapper.toFeedPostData(post, bestComment, commentCount);
					} catch (Exception e) {
						System.out.println("❌ DTO 변환 오류: " + e.getMessage());
						throw e;
					}
				})
				.toList();

			System.out.println("✅ DTO 변환 완료: " + feedPostList.size() + "개");

			FilterPostResponse.FeedList feedList = FilterPostResponse.FeedList.builder()
				.results(feedPostList)
				.build();

			System.out.println("🎉 ===== 팔로우 피드 조회 성공 =====");
			return ApiMessage.success(200, "팔로우 피드 조회 성공", feedList);

		} catch (IllegalStateException e) {
			System.out.println("❌ 인증 오류: " + e.getMessage());
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			System.out.println("💥 팔로우 피드 조회 오류: " + e.getMessage());
			e.printStackTrace();
			return ApiMessage.fail(500, "팔로우 피드 조회 실패: " + e.getMessage());
		}
	}

	/** 추천 피드 GET (API 18번) - TODO 구현 */
	@Transactional(readOnly = true)
	public FilterPostResponse.FeedList getRecommendPosts(String authHeader) {
		// TODO: 추천 시스템 구현 후 구현
		return FilterPostResponse.FeedList.builder()
			.results(List.of())
			.build();
	}

	/** 특정 유저 모든 게시글 GET (API 22번) */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.UserPostList> getUserPosts(String userIdStr) {
		try {
			UUID userId = UUID.fromString(userIdStr);
			List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, null);

			List<FilterPostResponse.UserPostData> userPostList = posts.stream()
				.map(postMapper::toUserPostData)
				.toList();

			FilterPostResponse.UserPostList response = FilterPostResponse.UserPostList.builder()
				.results(userPostList)
				.build();

			return ApiMessage.success(200, "사용자 게시글 조회 성공", response);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다.");
		} catch (Exception e) {
			return ApiMessage.fail(500, "사용자 게시글 조회 실패: " + e.getMessage());
		}
	}

	// 임시 메서드 - 추후 UserService에서 구현
	private List<UUID> getFollowingUserIds(UUID userId) {
		// TODO: UserService에서 팔로우 리스트 조회 로직 구현
		return List.of(); // 임시로 빈 리스트
	}


}
