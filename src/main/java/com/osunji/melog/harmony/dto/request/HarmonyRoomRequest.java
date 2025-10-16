package com.osunji.melog.harmony.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

public class HarmonyRoomRequest {

	/**
	 * 하모니룸 생성 요청 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Create {
		private String name;                // 하모니룸 이름
		private String intro;               // 소개글
		private List<String> category;      // 카테고리 리스트
		private String profileImg;          // 프로필 이미지 (선택)
	}

	/**
	 * 하모니룸 수정 요청 DTO - 값이 없으면 기존값 유지
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Update {
		private String name;                // 하모니룸 이름 (선택)
		private String intro;               // 소개글 (선택)
		private List<String> category;      // 카테고리 리스트 (선택)
		private String profileImg;          // 프로필 이미지 (선택)
		private Boolean isDirectAssign;     // 바로 승인 여부 (선택)
		private Boolean isPrivate;
	}

	/**
	 * 하모니룸 삭제 요청 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Delete {
		private String reason;
	}

	/**
	 * 하모니룸 게시글 생성 요청 DTO
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CreateHarmonyPost {
		@NotBlank(message = "게시글 내용은 필수입니다")
		private String content;
		private String mediaType;
		private String mediaUrl;
		private List<String> tags;
	}



	/**
	 * 하모니룸 공유 요청 DTO - 딥링크용
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Share {
		private String message;             // 공유 메시지 (선택)
	}

	/**
	 * 가입 승인/거절 요청 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ApproveOrDeny {
		private String userID;              // 승인/거절할 사용자 ID
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Report {
		private String reason;          // 신고 사유 ("spam", "abuse", "inappropriate" 등)
		private String category;        // 신고 카테고리
		private String details;         // 상세 내용
	}
}
