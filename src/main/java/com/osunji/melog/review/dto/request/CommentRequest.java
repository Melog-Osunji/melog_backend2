package com.osunji.melog.review.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

public class CommentRequest {

	@Getter
	@Setter
	@NoArgsConstructor
	//---------------Create (API 28번)-----------------//
	public static class Create {
		@NotBlank(message = "댓글 내용을 입력해 주세요.")
		private String content;              // ✅ 댓글 내용

		private String responseTo;           // ✅ API 명세 그대로: 부모 댓글 ID (대댓글인 경우)
		// null이면 일반 댓글, 값이 있으면 대댓글
	}

	@Getter
	@Setter
	@NoArgsConstructor
	//---------------Update (실제 개발에서 필요함 - 유지)-----------------//
	public static class Update {
		@NotBlank(message = "댓글 내용을 입력해 주세요.")
		private String content;              // ✅ 댓글 수정은 실제로 많이 쓰임
	}
}
