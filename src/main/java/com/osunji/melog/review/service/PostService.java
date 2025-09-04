package com.osunji.melog.review.service;

//global
import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;

// entity
import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;

// repo

import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.repository.CommentRepository;
//mapper
import com.osunji.melog.review.mapper.CommentMapper;
import com.osunji.melog.review.mapper.PostMapper;


//dto
import com.osunji.melog.review.dto.request.PostRequest;
import com.osunji.melog.review.dto.response.PostResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;

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
public class PostService {
	private final JWTUtil jwtUtil;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final AuthHelper authHelper;
	private final PostMapper postMapper;
	private final CommentMapper commentMapper;

	// 성공시 global>dto>ApiMessage 형식으로 답변

	//---------------게시글 CRUD-----------------//
	/**      게시글 CREATE (API 14번)     	    */
	public ApiMessage createPost(PostRequest.Create request, String authHeader) {

		// if header주입시 userID추출 공통
		String userId;
		// 1. 헤더에서 토큰을 추출하고, userId 추출
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (IllegalStateException e) {
			// 인증 실패: 토큰 X, 만료 등
			return ApiMessage.fail(401, e.getMessage());
		} catch (IllegalArgumentException e) {
			// 잘못된 요청 정보
			return ApiMessage.fail(400, e.getMessage());
		}
		// 2. user 엔티티 조회 (userId)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		// 3. post 엔티티 조회
		Post post = new Post(user, request.getTitle(), request.getContent(),
			request.getMediaType(), request.getMediaUrl(), request.getTags());
		// 4. save
		postRepository.save(post);
		return ApiMessage.success(200, "게시글이 성공적으로 생성되었습니다.", null);

	}

	/**      게시글 Get (API 15번)   path(postID)header(token)  	    */
	@Transactional(readOnly = true)
	public ApiMessage<PostResponse.Single> getPost(String postId, String authHeader) {
		String userId = null;

		// 1. 헤더에서 토큰을 추출하고, userId 추출
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (IllegalStateException e) {
			// 인증 실패: 토큰 X, 만료 등
			return ApiMessage.fail(401, e.getMessage());
		} catch (IllegalArgumentException e) {
			// 잘못된 요청 정보
			return ApiMessage.fail(400, e.getMessage());
		}

		// 2. post 엔티티 조회
		Post post = postRepository.findByIdWithUser(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		// 3. 숨김 처리된 게시글 여부(userId가 null이면 체크 생략)
		String finalUserId = userId;
		if (userId != null &&
			post.getHiddenUser().stream().anyMatch(u -> u.getId().equals(finalUserId))) {
			return ApiMessage.fail(403, "숨김 처리된 게시글입니다.");
		}
		// 5. 베스트 댓글 조회
		Optional<PostComment> bestCommentOpt = commentRepository.findBestComment(postId);
		PostComment bestComment = bestCommentOpt.orElse(null);

		// 댓글 개수 조회
		int commentCount = commentRepository.countCommentByPostId(postId);

		// PostMapper 사용
		PostResponse.Single responseData = postMapper.toSingle(post, bestComment, commentCount);

		return ApiMessage.success(200, "게시글 조회 성공", responseData);
	}

	/**      게시글 Update (API 30번)   path(postID)header(token)  	    */
	public ApiMessage updatePost(String postId, PostRequest.Update request, String authHeader) {
		String userId = null;

		// 1. 헤더에서 토큰을 추출하고, userId 추출
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (IllegalStateException e) {
			// 인증 실패: 토큰 X, 만료 등
			return ApiMessage.fail(401, e.getMessage());
		} catch (IllegalArgumentException e) {
			// 잘못된 요청 정보
			return ApiMessage.fail(400, e.getMessage());
		}

		// 2. post 엔티티 조회
		Post post = postRepository.findByIdWithUser(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		// 3. 작성자 권한 체크
		if (!post.getUser().getId().equals(userId)) {
			return ApiMessage.fail(403, "수정 권한이 없습니다.");
		}


		// 4. 업데이트 메서드 사용
		post.updatePost(
			request.getTitle(),
			request.getContent(),
			request.getMediaType(),
			request.getMediaUrl(),
			request.getTags()
		);

		return ApiMessage.success(200, "게시글이 성공적으로 수정되었습니다.", null);
	}

	/**      게시글 Delete (API 24번)   path(postID)header(token)  	    */
	public ApiMessage deletePost(String postId, String authHeader) {
		String userId = null;

		// 1. 헤더에서 토큰을 추출하고, userId 추출
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (IllegalStateException e) {
			// 인증 실패: 토큰 X, 만료 등
			return ApiMessage.fail(401, e.getMessage());
		} catch (IllegalArgumentException e) {
			// 잘못된 요청 정보
			return ApiMessage.fail(400, e.getMessage());
		}

		// 2. post 엔티티 조회
		Post post = postRepository.findByIdWithUser(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		// 3. 작성자 권한 체크
		if (!post.getUser().getId().equals(userId)) {
			return ApiMessage.fail(403, "수정 권한이 없습니다.");
		}

		postRepository.delete(post);
		return ApiMessage.success(200, "게시글이 성공적으로 삭제되었습니다.", null);
	}

	//---------------피드 조회-----------------//
	/**      인기피드Get (API 19번)   header(token)  	    */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.FeedList>  getPopularPosts(String authHeader) {
		String userId = null;

		// 1. 헤더에서 토큰을 추출하고, userId 추출
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (IllegalStateException e) {
			// 인증 실패: 토큰 X, 만료 등
			return ApiMessage.fail(401, e.getMessage());
		} catch (IllegalArgumentException e) {
			// 잘못된 요청 정보
			return ApiMessage.fail(400, e.getMessage());
		}

		// 2. postRepository의 findPopuplarPosts를 이용해서 좋아요많은순 내림차순 50개 출력
		List<Post> posts = postRepository.findPopularPosts(userId).stream().limit(50).toList();

		// 3. 각 게시글마다 베스트 댓글과 댓글 개수 조회 후 DTO 변환
		List<FilterPostResponse.FeedPostData> feedPostList = posts.stream()
			.map(post -> {
				// 베스트 댓글 조회 (없으면 null)
				Optional<PostComment> bestCommentOpt = commentRepository.findBestComment(post.getId());
				PostComment bestComment = bestCommentOpt.orElse(null);
				// 댓글 개수
				int commentCount = commentRepository.countCommentByPostId(post.getId());
				// 매퍼 사용해서 DTO 변환
				return postMapper.toFeedPostData(post, bestComment, commentCount);
			})
			.toList();

		// FeedList DTO로 감싸기
		FilterPostResponse.FeedList feedList = FilterPostResponse.FeedList.builder()
			.results(feedPostList)
			.build();

		return ApiMessage.success(200, "인기 피드 조회 성공", feedList);
	}



	// TODO: 추천시스템 구현 후 구현
	/**      추천피드Get (API 18번)   header(token)  	    */
	@Transactional(readOnly = true)
	public FilterPostResponse.FeedList getRecommendPosts(String authHeader) {
	// TODO: 추천시스템 구현 후 구현
		return null;
	}




	// todo:User service단에서 팔로우 조회 메서드 개발 후 사용
	/**      팔로우피드Get (API 20번)   header(token)  	    */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.FeedList> getFollowPosts(String authHeader) {
		String userId = null;

		// 1. 헤더에서 토큰을 추출하고, userId 추출
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		}

		// 2. 팔로잉 유저 ID 리스트 조회 (UserService 또는 별도 서비스에서 구현 필요)
		// todo:User service단에서 팔로우 조회 메서드 개발 후 사용
		List<String> followingUserIds = getFollowingUserIds(userId);

		// 3. 팔로우 중인 유저의 게시글 50개 조회 (최신순, hiddenUser 제외)
		List<Post> posts = postRepository.findFollowPosts(followingUserIds, userId)
			.stream()
			.limit(50)
			.toList();

		// 4. 각 게시글 별 베스트 댓글, 댓글 개수 조회 및 DTO 변환
		List<FilterPostResponse.FeedPostData> feedPostList = posts.stream()
			.map(post -> {
				Optional<PostComment> bestCommentOpt = commentRepository.findBestComment(post.getId());
				PostComment bestComment = bestCommentOpt.orElse(null);
				int commentCount = commentRepository.countCommentByPostId(post.getId());
				return postMapper.toFeedPostData(post, bestComment, commentCount);
			})
			.toList();

		// 5. FeedList DTO로  응답 생성
		FilterPostResponse.FeedList feedList = FilterPostResponse.FeedList.builder()
			.results(feedPostList)
			.build();

		return ApiMessage.success(200, "팔로우 피드 조회 성공", feedList);
	}
	private List<String> getFollowingUserIds(String userId) {
		// TODO: UserService 에서 import로 가져오고 이건 지우기
		return List.of(); // 임시로 빈리스트로 초기화
	}




	// TODO: 관련높은게시글을 찾는 메서드 구현 후 구현(현재팔로우)
	/**      관련높은게시글Get (API 21번)   header(token)  	    */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.FeedList> getSimilarPosts(String authHeader) {
		return ApiMessage.fail(200,"로직미구현");
	}



	/**      특정유저 모든게시글 Get (API 22번)   header(token)  	    */
	@Transactional(readOnly = true)
	public ApiMessage<FilterPostResponse.UserPostList>  getUserPosts(String userId) {
		return ApiMessage.fail(200,"로직미구현");
	}



}
