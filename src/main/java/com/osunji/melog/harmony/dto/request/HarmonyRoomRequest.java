package com.osunji.melog.harmony.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.util.List;

public class HarmonyRoomRequest {


	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Create {
		@NotBlank(message = "하모니룸 이름은 필수입니다.")
		private String name;

		private String intro;

		private List<String> category;

		private String profileImg;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Update {
		private String name;

		private String intro;

		private List<String> category;

		private String profileImg;

		private Boolean isDirectAssign;

		private Boolean isPrivate;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Delete {
		private String reason;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class CreateHarmonyPost {
		@NotBlank(message = "게시글 내용은 필수입니다.")
		private String content;

		private String mediaType;

		private String mediaUrl;

		private List<String> tags;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class UpdateHarmonyPost {
		private String content;

		private String mediaType;

		private String mediaUrl;

		private List<String> tags;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class CreateComment {
		@NotBlank(message = "댓글 내용은 필수입니다.")
		private String content;

		private String responseTo;  // 부모 댓글 ID. null이면 일반 댓글
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class UpdateComment {
		@NotBlank(message = "댓글 내용은 필수입니다.")
		private String content;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ApproveOrDeny {
		private String userID;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Report {
		private String reason;

		private String category;

		private String details;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Share {
		private String message;
	}
}
