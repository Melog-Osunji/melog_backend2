package com.osunji.melog.review.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

public class PostResponse {

	@Getter
	@Setter
	@Builder
	//---------------단일 게시글 조회 (GET /api/posts/{postId})-----------------//
	public static class Single {
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
		private List<String> hiddenUser;        // ["숨김처리","유저","리스트"]
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
		private String nickName;                  // 베댓작성자아이디
		private String content;                 // 베댓내용
		private String profileImg;
	}
}
