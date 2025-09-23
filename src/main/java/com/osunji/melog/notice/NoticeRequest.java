package com.osunji.melog.notice;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

public class NoticeRequest {

	/**
	 * 공지사항 생성 요청 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Create {
		private String title;
		private String content;
		private Boolean isImportant;        // 중요 공지사항 여부
		private String category;            // 카테고리
		private String imageUrl;            // 첨부 이미지
	}

	/**
	 * 공지사항 수정 요청 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Update {
		private String title;
		private String content;
		private Boolean isImportant;
		private String category;
		private String imageUrl;
	}
}
