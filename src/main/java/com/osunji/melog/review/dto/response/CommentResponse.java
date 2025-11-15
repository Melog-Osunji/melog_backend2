package com.osunji.melog.review.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

public class CommentResponse {

	@Getter
	@Setter
	@Builder
	//---------------모든 댓글 조회 (API 16번)-----------------//
	public static class All {
		private List<CommentData> comments;
	}

	@Getter
	@Setter
	@Builder
	//---------------베스트 댓글 조회 (API 17번)-----------------//
	public static class Best {
		private String id;
		private String userID;                  // ✅ API 명세: "userID"
		private String nickname;
		private String profileUrl;              // ✅ API 명세: "profileUrl"
		private String content;                 // 댓글내용
		private Integer likes;                  // ✅ Integer 타입으로 통일
	}

	@Getter
	@Setter
	@Builder
	public static class CommentData {
		private String id;
		private String userID;                  // ✅ API 명세: "userID" (작성자아이디)
		private String nickname;
		private String profileUrl;              // ✅ API 명세: "profileUrl"
		private String content;                 // 댓글내용
		private Integer likes;                  // ✅ Integer 타입으로 통일
		private List<RecommentData> recomments; // 대댓글 리스트
	}

	@Getter
	@Setter
	@Builder
	public static class RecommentData {
		private String id;
		private String userID;                  // ✅ API 명세: "userID" (작성자아이디)
		private String nickname;
		private String content;                 // 댓글내용
		private Integer likes;                  // ✅ Integer 타입으로 통일
		private List<RecommentData> recomments; // 대대댓글 (재귀 구조)
	}

	@Getter
	@Setter
	@Builder
	public static class CommentLikeResponse {
		private String action;    // "추가" 또는 "삭제"
		private boolean liked;    // 현재 좋아요 상태
		private int likeCount;    // 좋아요 개수
	}

}
