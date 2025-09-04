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
	//---------------추천/팔로우/인기 피드 (GET /api/posts/recommends, /populars, /follows)-----------------//
	public static class FeedList {
		private List<FeedPostData> results;
	}

	@Getter
	@Setter
	@Builder
	//---------------특정 유저의 모든 게시글 (GET /api/user/{userID}/posts)-----------------//
	public static class UserPostList {
		private List<UserPostData> results;
	}

	@Getter
	@Setter
	@Builder
	//---------------특정 유저의 북마크 게시글 (GET /api/posts/{userID}/bookmarks)-----------------//
	public static class BookmarkList {
		private List<BookmarkData> results;
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
		private int createdAgo;                 // 몇시간전작성인지 Integer
		private int likeCount;                  // 좋아요개수
		private List<String> hiddenUser;        // ["숨김처리","유저","목록"]
		private int commentCount;               // 댓글개수
		private BestCommentData bestComment;    // 베댓정보
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
		private String nickName;                  // 베댓작성자닉네임
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
		private String mediaType;               // 미디어타입 (audio 등)
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
