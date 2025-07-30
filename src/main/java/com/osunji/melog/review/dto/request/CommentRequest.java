package com.osunji.melog.review.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

public class CommentRequest {

	@Getter
	@Setter
	@NoArgsConstructor
	//---------------Create-----------------//
	public static class Create {
		@NotBlank(message = "댓글 내용을 입력해 주세요.")
		private String content;
		private String responseTo;
	}


}
