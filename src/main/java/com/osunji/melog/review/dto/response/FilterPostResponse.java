package com.osunji.melog.review.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

public class FilterPostResponse {

	@Getter
	@Setter
	@Builder
	//---------------추천/팔로우/인기 피드 (API 18,19,20번)-----------------//
	public static class FeedList {
		private List<FeedPostData> results;
	}

	@Getter
	@Setter
	@Builder
	//---------------특정 유저의 모든 게시글 (API 22번)-----------------//
	public static class UserPostList {
		private List<UserPostData> results;
	}

	@Getter
	@Setter
	@Builder
	public static class FeedPostData {
		private PostData post;
		private UserData user;
	}

	@Getter
	@Setter
	@Builder
	public static class PostData {
		private String id;                      // 게시글고유아이디
		private String title;                   // 제목
		private String content;                 // 내용
		private String mediaType;               // 미디어타입
		private String mediaUrl;                // 미디어url
		private List<String> tags;              // ["베토벤", "감상", "피아노"]
		private String createdAgo;              // ✅ Integer 타입으로 통일
		private Integer likeCount;              // ✅ Integer 타입으로 통일
		private List<String> hiddenUser;        // ["숨김처리","유저","목록"]
		private Integer commentCount;           // ✅ Integer 타입으로 통일
		private BestCommentData bestComment;    // 베댓정보
		private Boolean isLike;
		private Boolean isBookmark;
	}

	@Getter
	@Setter
	@Builder
	public static class UserData {
		private String id;                      // 작성자아이디
		private String nickName;                // 닉네임
		private String profileImg;              // 프로필사진url
	}

	@Getter
	@Setter
	@Builder
	public static class BestCommentData {
		private String userId;                  // ✅ 베댓작성자아이디
		private String content;                 // 베댓내용
		private String profileImg;              // 프로필사진url
	}

	@Getter
	@Setter
	@Builder
	public static class UserPostData {
		private String id;                      // 게시글고유아이디
		private String title;                   // 제목
		private String content;                 // 내용
		private String mediaType;               // 미디어타입
		private String mediaUrl;                // 미디어url
		private List<String> tags;              // ["베토벤", "감상", "피아노"]
	}

	@Getter
	@Setter
	@Builder
	public static class BookmarkData {
		private String postId;                  // 게시글 ID
		private String title;                   // 제목
		private LocalDateTime createdAt;        // 생성일시 (ISO 형식)
	}
}
