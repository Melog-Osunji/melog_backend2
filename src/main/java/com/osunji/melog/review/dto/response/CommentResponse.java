package com.osunji.melog.review.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

public class CommentResponse {

	@Getter
	@Setter
	@Builder
	//---------------모든 댓글 조회 (GET /api/posts/{postId}/comments)-----------------//
	public static class All {
		private List<CommentData> comments;
	}

	@Getter
	@Setter
	@Builder
	//---------------베스트 댓글 조회 (GET /api/posts/{postId}/bestComment)-----------------//
	public static class Best {
		private String nickname;              // 작성자닉네임
		private String profileUrl;          // 유저 프로필 이미지 url
		private String content;             // 댓글내용
		private int likes;                  // 댓글좋아요수
	}


	@Getter
	@Setter
	@Builder
	public static class CommentData {
		private String nickname;              // 작성자닉네임
		private String profileUrl;          // 유저 프로필 이미지 url
		private String content;             // 댓글내용
		private int likes;                  // 댓글좋아요수
		private List<RecommentData> recomments; // 대댓글 리스트
	}

	@Getter
	@Setter
	@Builder
	public static class RecommentData {
		private String nickname;              // 작성자닉네임
		private String content;             // 댓글내용
		private int likes;                  // 댓글좋아요수
		private List<RecommentData> recomments; // 대대댓글 (재귀 구조)
	}
}
