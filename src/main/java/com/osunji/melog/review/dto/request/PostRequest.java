package com.osunji.melog.review.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.List;



// Post관련 Req
public class PostRequest {

	@Getter
	@Setter
	@NoArgsConstructor
	//---------------Create-----------------//
	public static class Create {
		@NotBlank(message = "제목을 입력해 주세요.")
		private String title;
		@NotBlank(message = "내용을 입력해 주세요.")
		private String content;
		private String mediaType;
		private String mediaUrl;
		private List<String> tags;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	//---------------Update-----------------//
	public static class Update {
		private String title;        // null이면 수정하지 않음
		private String content;      // null이면 수정하지 않음
		private String mediaType;    // null이면 수정하지 않음
		private String mediaUrl;     // null이면 수정하지 않음
		private List<String> tags;   // null이면 수정하지 않음
	}
}