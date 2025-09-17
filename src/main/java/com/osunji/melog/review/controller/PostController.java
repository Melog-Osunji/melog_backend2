package com.osunji.melog.review.controller;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.review.dto.request.CommentRequest;
import com.osunji.melog.review.dto.request.PostRequest;
import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.dto.response.CommentResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.review.dto.response.PostResponse;
import com.osunji.melog.review.service.BookmarkService;
import com.osunji.melog.review.service.CommentService;
import com.osunji.melog.review.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final CommentService commentService;
	private final BookmarkService bookmarkService;

	//---------------게시글 CRUD-----------------//

	/** 14. 게시글 작성 POST /api/posts */
	@PostMapping("/posts")
	public ResponseEntity<ApiMessage> createPost(
		@Valid @RequestBody PostRequest.Create request,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage response = postService.createPost(request, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 15. 특정 게시글 조회 GET /api/posts/{postID} */
	@GetMapping("/posts/{postId}")
	public ResponseEntity<ApiMessage<PostResponse.Single>> getPost(
		@PathVariable String postId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {

		ApiMessage<PostResponse.Single> response = postService.getPost(postId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 30. 게시글 수정 PATCH /api/posts/{postID} */
	@PatchMapping("/posts/{postId}")
	public ResponseEntity<ApiMessage> updatePost(
		@PathVariable String postId,
		@Valid @RequestBody PostRequest.Update request,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage response = postService.updatePost(postId, request, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 24. 게시글 삭제 DELETE /api/posts/{postID} */
	@DeleteMapping("/posts/{postId}")
	public ResponseEntity<ApiMessage> deletePost(
		@PathVariable String postId,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage response = postService.deletePost(postId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}
	/**
	 * 게시글 좋아요/취소
	 * POST /api/posts/{postId}/like
	 */
	@PostMapping("/posts/{postId}/like")
	public ApiMessage likePost(
		@PathVariable String postId,
		@RequestHeader("Authorization") String authHeader
	) {
		return postService.likeOrUnlikePost(postId, authHeader);
	}
	//---------------댓글 관련-----------------//

	/** 16. 특정 게시글 모든 댓글 조회 GET /api/posts/{postID}/comments */
	@GetMapping("/posts/{postId}/comments")
	public ResponseEntity<ApiMessage<CommentResponse.All>> getAllComments(@PathVariable String postId) {
		ApiMessage<CommentResponse.All> response = commentService.getAllComments(postId);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 17. 베스트 댓글 조회 GET /api/posts/{postID}/bestComment */
	@GetMapping("/posts/{postId}/bestComment")
	public ResponseEntity<ApiMessage<CommentResponse.Best>> getBestComment(@PathVariable String postId) {
		ApiMessage<CommentResponse.Best> response = commentService.getBestComment(postId);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 28. 댓글 작성 POST /api/posts/{postID}/comment */
	@PostMapping("/posts/{postId}/comment")
	public ResponseEntity<ApiMessage> createComment(
		@PathVariable String postId,
		@Valid @RequestBody CommentRequest.Create request,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage response = commentService.createComment(postId, request, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 26. 댓글 삭제 DELETE /api/posts/{postID}/comments/{commentID} */
	@DeleteMapping("/posts/{postId}/comments/{commentId}")
	public ResponseEntity<ApiMessage> deleteComment(
		@PathVariable String postId,
		@PathVariable String commentId,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage response = commentService.deleteComment(postId, commentId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 29. 댓글 좋아요/취소 PATCH /api/posts/{postID}/comments/{commentID} */
	@PatchMapping("/posts/{postId}/comments/{commentId}")
	public ResponseEntity<ApiMessage<Void>> toggleCommentLike(
		@PathVariable String postId,
		@PathVariable String commentId,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage<Void> response = commentService.likeOrUnlikeComment(postId, commentId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	//---------------북마크 관련-----------------//

	/** 27. 북마크 생성 POST /api/posts/{postID}/bookmark */
	@PostMapping("/posts/{postId}/bookmark")
	public ResponseEntity<ApiMessage> createBookmark(
		@PathVariable String postId,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage response = bookmarkService.addBookmark(postId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 25. 북마크 제거 DELETE /api/posts/{postID}/bookmark */
	@DeleteMapping("/posts/{postId}/bookmarks")
	public ResponseEntity<ApiMessage> deleteBookmark(
		@PathVariable String postId,
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage response = bookmarkService.removeBookmark(postId, authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 23. 사용자 북마크 조회 GET /api/users/{userId}/bookmarks */
	@GetMapping("/posts/{userId}/bookmarks")
	public ResponseEntity<ApiMessage<BookmarkResponse.ListAll>> getUserBookmarks(@PathVariable String userId) {
		ApiMessage<BookmarkResponse.ListAll> response = bookmarkService.getBookmarksByUser(userId);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	//---------------피드 관련-----------------//

	/** 18. 추천 피드 GET /api/posts/recommends */
	@GetMapping("/posts/recommends")
	public ResponseEntity<FilterPostResponse.FeedList> getRecommendPosts(
		@RequestHeader(value = "Authorization", required = false) String authHeader) {

		FilterPostResponse.FeedList response = postService.getRecommendPosts(authHeader);
		return ResponseEntity.ok(response);
	}

	/** 19. 인기 피드 GET /api/posts/populars */
	@GetMapping("/posts/populars")
	public ResponseEntity<ApiMessage<FilterPostResponse.FeedList>> getPopularPosts(
		@RequestHeader(value = "Authorization", required = false) String authHeader) {

		ApiMessage<FilterPostResponse.FeedList> response = postService.getPopularPosts(authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 20. 팔로우 피드 GET /api/posts/follows */
	@GetMapping("/posts/follows")
	public ResponseEntity<ApiMessage<FilterPostResponse.FeedList>> getFollowPosts(
		@RequestHeader("Authorization") String authHeader) {

		ApiMessage<FilterPostResponse.FeedList> response = postService.getFollowPosts(authHeader);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	/** 22. 사용자 게시글 조회 GET /api/users/{userID}/posts */
	@GetMapping("/users/{userId}/posts")
	public ResponseEntity<ApiMessage<FilterPostResponse.UserPostList>> getUserPosts(
		@PathVariable String userId,
		@RequestHeader(value = "Authorization", required = false) String authHeader) {

		ApiMessage<FilterPostResponse.UserPostList> response = postService.getUserPosts(userId);
		return ResponseEntity.status(response.getCode()).body(response);
	}
}
