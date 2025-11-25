package com.osunji.melog.report;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {
	private String userID;     // 토큰에서 추출한 유저 UUID 문자열
	private String reason;          // enum명 문자열 (ReportReason 중 하나)
	private String postId;          // 게시글 신고 시 UUID
	private String commentId;       // 댓글 신고 시 UUID (게시글 아닌 경우)
	private String reportedUserId;  // 신고 대상 유저 ID (선택)
	private LocalDateTime createdAt; // 신고 생성 시간 (요청 시 서버 시간 자동 생성도 가능)
}
