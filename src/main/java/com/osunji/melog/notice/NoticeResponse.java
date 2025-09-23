package com.osunji.melog.notice;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class NoticeResponse {

	/**
	 * 공지사항 목록 조회 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class NoticeList {
		private List<NoticeInfo> notices;
		private Integer totalCount;
		private Boolean hasMore;            // 더 있는지 여부

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		public static class NoticeInfo {
			private String id;
			private String title;
			private String content;
			private Boolean isImportant;
			private String category;
			private String imageUrl;
			private LocalDateTime createdAt;
			private LocalDateTime updatedAt;
		}
	}

	/**
	 * 공지사항 상세 조회 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class NoticeDetail {
		private String id;
		private String title;
		private String content;
		private Boolean isImportant;
		private String category;
		private String imageUrl;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}
}
