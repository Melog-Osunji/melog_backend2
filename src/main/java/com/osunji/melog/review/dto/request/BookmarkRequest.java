package com.osunji.melog.review.dto.request;

import lombok.Getter;
import lombok.Setter;

public class BookmarkRequest {

	@Getter
	@Setter
	//---------------북마크 추가 요청-----------------//
	public static class Create {
		private String postId;   // 북마크할 게시글 ID
	}

	@Getter
	@Setter
	//---------------북마크 삭제 요청-----------------//
	public static class Delete {
		private String postId;   // 북마크 해제할 게시글 ID
	}
}
