package com.osunji.melog.review.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class BookmarkResponse {

	@Getter
	@Setter
	@Builder
	//---------------유저 북마크 목록------cc-----------//
	public static class ListAll {
		private List<BookmarkData> results;
	}

	@Getter
	@Setter
	@Builder
	public static class BookmarkData {
		private String postId;          // 게시글 ID
		private String title;           // 게시글 제목
		private LocalDateTime createdAt; // 북마크 등록 일시
	}
}
