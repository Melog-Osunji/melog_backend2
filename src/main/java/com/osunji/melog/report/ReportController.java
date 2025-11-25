package com.osunji.melog.report;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.osunji.melog.global.dto.ApiMessage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@PostMapping("/reports")
	public ResponseEntity<ApiMessage> reportContent(@RequestBody ReportRequest request) {
		try {
			UUID reportingUserId = UUID.fromString(request.getUserID());

			ReportReason reason = ReportReason.valueOf(request.getReason());

			reportService.createReport(
				reportingUserId,
				reason,
				request.getPostId() != null ? UUID.fromString(request.getPostId()) : null,
				request.getCommentId() != null ? UUID.fromString(request.getCommentId()) : null,
				request.getReportedUserId() != null ? UUID.fromString(request.getReportedUserId()) : null
			);

			return ResponseEntity.ok(ApiMessage.success(200, "신고가 접수되었습니다.",null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ApiMessage.fail(400, "잘못된 신고 사유 타입입니다."));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiMessage.fail(500, "신고 접수에 실패했습니다."));
		}
	}
}
