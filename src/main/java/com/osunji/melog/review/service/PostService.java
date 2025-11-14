package com.osunji.melog.review.service;
import com.osunji.melog.global.util.JWTUtil;

import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.harmony.entity.HarmonyRoomPosts;
import com.osunji.melog.harmony.repository.HarmonyRoomPostsRepository;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.review.repository.BookmarkRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.repository.CommentRepository;
import com.osunji.melog.review.mapper.PostMapper;
import com.osunji.melog.review.dto.request.PostRequest;
import com.osunji.melog.review.dto.response.PostResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.user.repository.UserRepository;
import com.osunji.melog.user.repository.FollowRepository;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
	private final HarmonyRoomPostsRepository harmonyRoomPostsRepository;
	private final FollowRepository followRepository;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final BookmarkRepository bookmarkRepository;
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
			// 6. 좋아요/북마크 여부 체크
			boolean isLike = false;
			boolean isBookmark = false;
			if (userId != null) {
				isLike = post.isLikedBy(userRepository.findById(userId).orElse(null));
				isBookmark = bookmarkRepository.existsByUserIdAndPostId(userId, postId);
			}
			// 7. DTO 변환
			PostResponse.Single responseData = postMapper.toSingle(post, bestComment, commentCount,isLike, isBookmark);

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

	/** 내가 이 게시글 좋아요 했는지 안 했는지*/
	@Transactional(readOnly = true)
	public boolean isPostLikedByUser(String postId, String authHeader) {
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		UUID postUUID = UUID.fromString(postId);

		// 먼저 일반 게시글에서 조회
		Optional<Post> feedPostOpt = postRepository.findById(postUUID);
		if (feedPostOpt.isPresent()) {
			return feedPostOpt.get().getLikes().stream()
				.anyMatch(user -> user.getId().equals(userId));
		}
		// 아니면 하모니룸 게시글에서 조회
		Optional<HarmonyRoomPosts> harmonyPostOpt = harmonyRoomPostsRepository.findById(postUUID);
		if (harmonyPostOpt.isPresent()) {
			return harmonyPostOpt.get().getLikes().stream()
				.anyMatch(like -> like.getUser().getId().equals(userId));
		}

		throw new IllegalArgumentException("해당 ID에 해당하는 게시글이 없습니다.");
	}


	//---------------피드 조회-----------------//

	/** 인기 피드 GET (API 19번) - 좋아요 순 */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.FeedList> getPopularPosts(String authHeader) {
		try {
			System.out.println("🔥 ===== 인기 피드 조회 시작 (좋아요 순) =====");

			// 1. 토큰에서 userId 추출 (선택적)
			UUID userId = null;
			try {
				userId = authHelper.authHelperAsUUID(authHeader);
				System.out.println("✅ 로그인 사용자: " + userId);
			} catch (Exception e) {
				System.out.println("ℹ️ 비로그인 사용자로 처리");
			}

			// 2. ✅ 전체 게시글 조회 후 좋아요 순 정렬
			System.out.println("📋 인기 게시글 조회 시작...");

			List<Post> allPosts = postRepository.findAll();
			System.out.println("  - 전체 게시글 수: " + allPosts.size());

			// 좋아요 수 기준으로 정렬
			List<Post> popularPosts = allPosts.stream()
				.filter(post -> post.getUser() != null) // user가 null인 게시글 제외
				.sorted((p1, p2) -> {
					int likes1 = (p1.getLikes() != null) ? p1.getLikes().size() : 0;
					int likes2 = (p2.getLikes() != null) ? p2.getLikes().size() : 0;

					System.out.println("    게시글 " + p1.getId() + ": " + likes1 + "개 좋아요");
					System.out.println("    게시글 " + p2.getId() + ": " + likes2 + "개 좋아요");

					return Integer.compare(likes2, likes1); // 좋아요 많은 순 (내림차순)
				})
				.limit(50)
				.collect(Collectors.toList());

			System.out.println("  - 인기 게시글 (좋아요 순) " + popularPosts.size() + "개 선별 완료");

			// 3. DTO 변환
			System.out.println("📋 DTO 변환 시작...");
			List<FilterPostResponse.FeedPostData> feedPostList = new ArrayList<>();

			for (Post post : popularPosts) {
				try {
					int likeCount = (post.getLikes() != null) ? post.getLikes().size() : 0;
					System.out.println("  변환 중: " + post.getId() + " - " + post.getTitle() + " (좋아요 " + likeCount + "개)");

					// ✅ 안전한 댓글 조회
					Optional<PostComment> bestCommentOpt = Optional.empty();
					int commentCount = 0;

					try {
						bestCommentOpt = commentRepository.findBestComment(post.getId());
						commentCount = commentRepository.countCommentByPostId(post.getId());
					} catch (Exception e) {
						System.out.println("    ⚠️ 댓글 조회 실패: " + e.getMessage());
					}

					PostComment bestComment = bestCommentOpt.orElse(null);
					boolean isLike = false;
					boolean isBookmark = false;
					if(userId != null) {
						isLike = postRepository.existsLikeByUserIdAndPostId(userId, post.getId());
						isBookmark = bookmarkRepository.existsByUserIdAndPostId(userId, post.getId());
					}
					FilterPostResponse.FeedPostData feedData = postMapper.toFeedPostData(post, bestComment, commentCount, isLike, isBookmark);

					feedPostList.add(feedData);

					System.out.println("    ✅ 변환 완료");

				} catch (Exception e) {
					System.out.println("    ❌ DTO 변환 오류: " + e.getMessage());
					e.printStackTrace();
					// 실패한 게시글은 스킵하고 계속 진행
				}
			}

			System.out.println("✅ DTO 변환 완료: " + feedPostList.size() + "개");

			FilterPostResponse.FeedList feedList = FilterPostResponse.FeedList.builder()
				.results(feedPostList)
				.build();

			System.out.println("🎉 ===== 인기 피드 조회 성공 (좋아요 순) =====");
			return ApiMessage.success(200, "인기 피드 조회 성공", feedList);

		} catch (Exception e) {
			System.out.println("💥 인기 피드 조회 오류: " + e.getMessage());
			e.printStackTrace();
			return ApiMessage.fail(500, "인기 피드 조회 실패: " + e.getMessage());
		}
	}

	/** 팔로우 피드 GET (API 20번)  */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.FeedList> getFollowPosts(String authHeader) {
		try {
			System.out.println("🔥 ===== 팔로우 피드 조회 시작 =====");

			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			System.out.println("✅ 사용자 ID 추출: " + userId);

			// 2. 팔로잉 유저 ID 리스트 조회
			List<UUID> followingUserIds = followRepository.findFolloweeIds(userId);
			System.out.println("📋 팔로잉 사용자 수: " + followingUserIds.size());
			System.out.println("  - 팔로잉 ID 목록: " + followingUserIds);

			if (followingUserIds.isEmpty()) {
				System.out.println("⚠️ 팔로잉 사용자가 없음 - 빈 결과 반환");
				return ApiMessage.success(200, "팔로우하는 사용자가 없습니다",
					FilterPostResponse.FeedList.builder()
						.results(Collections.emptyList())
						.build());
			}
			System.out.println("📋 게시글 조회 시작 (전체 조회 후 필터링 방법)...");

			List<Post> allPosts = postRepository.findAll();
			System.out.println("  - 전체 게시글 수: " + allPosts.size());

			// 각 게시글 정보 출력 (디버깅)
			for (Post post : allPosts) {
				if (post.getUser() != null) {
					System.out.println("    게시글: " + post.getId() + " - 작성자: " + post.getUser().getId() + " - 제목: " + post.getTitle());
				} else {
					System.out.println("    게시글: " + post.getId() + " - 작성자: null - 제목: " + post.getTitle());
				}
			}

			// 팔로잉하는 사용자의 게시글만 필터링
			List<Post> followingPosts = allPosts.stream()
				.filter(post -> {
					if (post.getUser() == null) {
						System.out.println("    ⚠️ 게시글 " + post.getId() + "의 user가 null");
						return false;
					}
					boolean isFollowing = followingUserIds.contains(post.getUser().getId());
					System.out.println("    게시글 " + post.getId() + " 작성자 " + post.getUser().getId() + " 팔로잉 여부: " + isFollowing);
					return isFollowing;
				})
				.sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt())) // 최신순
				.limit(50)
				.toList();

			System.out.println("  - 팔로잉 사용자 게시글 수: " + followingPosts.size());

			// 각 팔로잉 사용자별 게시글 수 출력
			for (UUID followingId : followingUserIds) {
				long count = followingPosts.stream()
					.filter(p -> p.getUser() != null && p.getUser().getId().equals(followingId))
					.count();
				System.out.println("    - 사용자 " + followingId + ": " + count + "개 게시글");

				// 해당 사용자의 게시글 제목들 출력
				followingPosts.stream()
					.filter(p -> p.getUser() != null && p.getUser().getId().equals(followingId))
					.forEach(p -> System.out.println("      * " + p.getTitle()));
			}

			// 4. DTO 변환
			System.out.println("📋 DTO 변환 시작...");
			List<FilterPostResponse.FeedPostData> feedPostList = new ArrayList<>();

			for (Post post : followingPosts) {
				boolean isLike = false;
				boolean isBookmark = false;
				if (userId != null) {
					isLike = postRepository.existsLikeByUserIdAndPostId(userId, post.getId());
					isBookmark = bookmarkRepository.existsByUserIdAndPostId(userId, post.getId());
				}
				try {
					System.out.println("  변환 중: " + post.getId() + " - " + post.getTitle());

					// ✅ 안전한 댓글 조회
					Optional<PostComment> bestCommentOpt = Optional.empty();
					int commentCount = 0;

					try {
						bestCommentOpt = commentRepository.findBestComment(post.getId());
						commentCount = commentRepository.countCommentByPostId(post.getId());
					} catch (Exception e) {
						System.out.println("    ⚠️ 댓글 조회 실패: " + e.getMessage());
					}

					PostComment bestComment = bestCommentOpt.orElse(null);
					FilterPostResponse.FeedPostData feedData = postMapper.toFeedPostData(post, bestComment, commentCount, isLike, isBookmark);
					feedPostList.add(feedData);

					System.out.println("    ✅ 변환 완료");

				} catch (Exception e) {
					System.out.println("    ❌ DTO 변환 오류: " + e.getMessage());
					e.printStackTrace();
					// 실패한 게시글은 스킵하고 계속 진행
				}
			}

			System.out.println("✅ DTO 변환 완료: " + feedPostList.size() + "개");

			FilterPostResponse.FeedList feedList = FilterPostResponse.FeedList.builder()
				.results(feedPostList)
				.build();

			System.out.println("🎉 ===== 팔로우 피드 조회 성공 =====");
			return ApiMessage.success(200, "팔로우 피드 조회 성공", feedList);

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

    /** 특정 유저의 '미디어가 포함된' 모든 게시글(피드형) GET  */
    @Transactional(readOnly = true)
    public ApiMessage<FilterPostResponse.FeedList> getUserMediaFeed(String userIdStr,
                                                                    @Nullable String currentUserIdStr) {
        try {
            // 1) 파라미터 파싱
            UUID userId = UUID.fromString(userIdStr);
            UUID currentUserId = null;
            if (currentUserIdStr != null && !currentUserIdStr.isBlank()) {
                currentUserId = UUID.fromString(currentUserIdStr);
            }

            // 2) 미디어 포함 게시글 조회
            List<Post> posts = postRepository.findUserMediaPostsWithAuthor(userId, currentUserId);

            // 3) DTO 변환 (댓글 수/베스트 댓글 포함)
            List<FilterPostResponse.FeedPostData> feedPostList = new ArrayList<>();

            for (Post post : posts) {
                try {
                    Optional<PostComment> bestCommentOpt = Optional.empty();
                    int commentCount = 0;

                    try {
                        bestCommentOpt = commentRepository.findBestComment(post.getId());
                        commentCount = commentRepository.countCommentByPostId(post.getId());
                    } catch (Exception e) {
                        // 댓글 조회 실패해도 나머지 렌더링은 진행
                    }

                    PostComment bestComment = bestCommentOpt.orElse(null);

                    // 기존에 쓰던 매퍼 그대로 사용 (likeCount/createdAgo/hiddenUser 등 내부 규칙 일관 유지)
                    FilterPostResponse.FeedPostData feedData =
                            postMapper.toFeedPostData(post, bestComment, commentCount);

                    feedPostList.add(feedData);

                } catch (Exception e) {
                    // 문제 생긴 게시물은 스킵하고 계속
                }
            }

            FilterPostResponse.FeedList body = FilterPostResponse.FeedList.builder()
                    .results(feedPostList)
                    .build();

            return ApiMessage.success(200, "사용자 미디어 게시글(댓글 포함) 조회 성공", body);

        } catch (IllegalArgumentException e) {
            // UUID 파싱 실패 등
            return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다.");
        } catch (Exception e) {
            return ApiMessage.fail(500, "사용자 미디어 게시글 조회 실패: " + e.getMessage());
        }
    }

    /** 특정 유저의 '전체' 게시글(피드형) GET */
    @Transactional(readOnly = true)
    public ApiMessage<FilterPostResponse.FeedList> getUserFeed(String userIdStr,
                                                               @Nullable String currentUserIdStr) {
        try {
            // 1) 파라미터 파싱
            UUID userId = UUID.fromString(userIdStr);
            UUID currentUserId = null;
            if (currentUserIdStr != null && !currentUserIdStr.isBlank()) {
                currentUserId = UUID.fromString(currentUserIdStr);
            }

            // 2) 전체 게시글 조회 (미디어 조건 제거)
            List<Post> posts = postRepository.findUserPostsWithAuthor(userId, currentUserId);

            // 3) DTO 변환 (댓글 수/베스트 댓글 포함) — 기존 매퍼 재사용
            List<FilterPostResponse.FeedPostData> feedPostList = new ArrayList<>();
            for (Post post : posts) {
                try {
                    Optional<PostComment> bestCommentOpt = Optional.empty();
                    int commentCount = 0;

                    try {
                        bestCommentOpt = commentRepository.findBestComment(post.getId());
                        commentCount = commentRepository.countCommentByPostId(post.getId());
                    } catch (Exception ignore) {
                        // 댓글 조회 실패해도 개별 게시물 렌더링은 진행
                    }

                    PostComment bestComment = bestCommentOpt.orElse(null);
                    FilterPostResponse.FeedPostData feedData =
                            postMapper.toFeedPostData(post, bestComment, commentCount);

                    feedPostList.add(feedData);
                } catch (Exception ignore) {
                    // 문제 발생한 게시물은 스킵
                }
            }

            FilterPostResponse.FeedList body = FilterPostResponse.FeedList.builder()
                    .results(feedPostList)
                    .build();

            return ApiMessage.success(200, "사용자 전체 게시글(댓글 포함) 조회 성공", body);

        } catch (IllegalArgumentException e) {
            // UUID 파싱 실패 등
            return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다.");
        } catch (Exception e) {
            return ApiMessage.fail(500, "사용자 전체 게시글 조회 실패: " + e.getMessage());
        }
    }



	private String formatCreatedAgo(LocalDateTime createdAt) {
		if (createdAt == null) return "";

		long hours = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
		if (hours < 1) return "방금 전";
		else if (hours < 24) return hours + "시간 전";

		long days = ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
		if (days == 1) return "하루 전";
		if (days <= 30) return days + "일 전";

		long months = ChronoUnit.MONTHS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
		if (months == 1) return "한 달 전";
		return months + "달 전";
	}

}
