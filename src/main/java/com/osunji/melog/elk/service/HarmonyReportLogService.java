package com.osunji.melog.elk.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.osunji.melog.elk.entity.HarmonyReportLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HarmonyReportLogService {

	private final ElasticsearchClient elasticsearchClient;

	/**
	 * 하모니룸 신고 로그 기록 - 간소화 버전 (Field 오류 해결)
	 */
	public void logHarmonyReport(String reportId, String harmonyId, String harmonyName,
		String reporterId, String reason, String details) {
		try {
			// 안전한 필드 처리
			String safeReportId = processReportId(reportId);
			String safeHarmonyId = processHarmonyId(harmonyId);
			String safeHarmonyName = processHarmonyName(harmonyName);
			String safeReporterId = processReporterId(reporterId);
			String safeReason = processReason(reason);
			String safeDetails = processDetails(details);

			HarmonyReportLog reportLog = HarmonyReportLog.builder()
				.reportId(safeReportId)
				.harmonyId(safeHarmonyId)
				.harmonyName(safeHarmonyName)
				.reporterId(safeReporterId)
				.reason(safeReason)
				.details(safeDetails)
				.reportTime(LocalDateTime.now())
				.build();

			IndexRequest<HarmonyReportLog> request = IndexRequest.of(i -> i
				.index("harmony_reports")
				.document(reportLog)
			);

			elasticsearchClient.index(request);

			log.info("📊 하모니룸 신고 로그 저장 완료: reportId='{}', harmonyId='{}', reason='{}'",
				safeReportId, safeHarmonyId, safeReason);

		} catch (Exception e) {
			log.error("💥 하모니룸 신고 로그 저장 실패: reportId='{}', error: {}",
				reportId, e.getMessage());
		}
	}

	/**
	 * reportId 필드 처리
	 */
	private String processReportId(String reportId) {
		if (reportId == null || reportId.trim().isEmpty()) {
			return "UNKNOWN_REPORT_ID";
		}
		return reportId.trim();
	}

	/**
	 * harmonyId 필드 처리
	 */
	private String processHarmonyId(String harmonyId) {
		if (harmonyId == null || harmonyId.trim().isEmpty()) {
			return "UNKNOWN_HARMONY_ID";
		}
		return harmonyId.trim();
	}

	/**
	 * harmonyName 필드 처리 (한글 지원)
	 */
	private String processHarmonyName(String harmonyName) {
		if (harmonyName == null || harmonyName.trim().isEmpty()) {
			return "UNKNOWN_HARMONY_NAME";
		}

		// UTF-8 안전성 보장 (한글 처리)
		try {
			String trimmed = harmonyName.trim();
			// 길이 제한 (100자)
			if (trimmed.length() > 100) {
				trimmed = trimmed.substring(0, 100) + "...";
			}
			// UTF-8 바이트로 변환 후 다시 문자열로 변환하여 인코딩 보장
			return new String(trimmed.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.warn("HarmonyName 인코딩 처리 실패, 원본 사용: {}", harmonyName);
			return harmonyName.trim();
		}
	}

	/**
	 * reporterId 필드 처리
	 */
	private String processReporterId(String reporterId) {
		if (reporterId == null || reporterId.trim().isEmpty()) {
			return "anonymous_reporter";
		}
		return reporterId.trim();
	}

	/**
	 * reason 필드 처리 (한글 지원)
	 */
	private String processReason(String reason) {
		if (reason == null || reason.trim().isEmpty()) {
			return "UNKNOWN_REASON";
		}

		try {
			String trimmed = reason.trim();
			// 길이 제한 (200자)
			if (trimmed.length() > 200) {
				trimmed = trimmed.substring(0, 200) + "...";
			}
			return new String(trimmed.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.warn("Reason 인코딩 처리 실패, 원본 사용: {}", reason);
			return reason.trim();
		}
	}

	/**
	 * details 필드 처리 (한글 지원, null 허용)
	 */
	private String processDetails(String details) {
		if (details == null) {
			return null;  // null 허용
		}

		String trimmed = details.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		try {
			// 길이 제한 (500자)
			if (trimmed.length() > 500) {
				trimmed = trimmed.substring(0, 500) + "...";
			}
			return new String(trimmed.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.warn("Details 인코딩 처리 실패, 원본 사용: {}", details);
			return trimmed;
		}
	}

	/**
	 * 신고 카테고리별 로그 (명시적)
	 */
	public void logHarmonyReportByCategory(String reportId, String harmonyId, String harmonyName,
		String reporterId, String reason, String category, String details) {
		// category를 reason에 포함시켜서 로그
		String reasonWithCategory = reason + " (카테고리: " + (category != null ? category : "N/A") + ")";
		logHarmonyReport(reportId, harmonyId, harmonyName, reporterId, reasonWithCategory, details);
	}

	/**
	 * 통계 로그
	 */
	public void logReportStatistics(String harmonyId) {
		try {
			log.info("📊 하모니룸 신고 통계 업데이트 - ID: {}", harmonyId);

			// 통계용 로그
			logHarmonyReport(
				"STATS_" + System.currentTimeMillis(),
				harmonyId,
				"STATISTICS_LOG",
				"system",
				"REPORT_STATISTICS_UPDATE",
				"신고 통계 업데이트"
			);
		} catch (Exception e) {
			log.error("신고 통계 로깅 실패: {}", e.getMessage());
		}
	}

	/**
	 * 어뷰징 감지 로그
	 */
	public void logAbusingDetection(String reporterId, int reportCount) {
		try {
			logHarmonyReport(
				"ABUSE_" + System.currentTimeMillis(),
				"SYSTEM_MONITORING",
				"ABUSE_DETECTION",
				reporterId,
				"FREQUENT_REPORTING_DETECTED",
				"24시간 내 " + reportCount + "회 신고 감지"
			);
		} catch (Exception e) {
			log.error("어뷰징 감지 로그 실패: {}", e.getMessage());
		}
	}

	/**
	 * 벌크 신고 로그 저장
	 */
	public void logMultipleHarmonyReports(List<HarmonyReportLog> reportLogs) {
		try {
			for (HarmonyReportLog reportLog : reportLogs) {
				IndexRequest<HarmonyReportLog> request = IndexRequest.of(i -> i
					.index("harmony_reports")
					.document(reportLog)
				);
				elasticsearchClient.index(request);
			}
			log.info("📊 벌크 하모니룸 신고 로그 저장 완료: {}개", reportLogs.size());
		} catch (Exception e) {
			log.error("💥 벌크 신고 로그 저장 실패: {}", e.getMessage());
		}
	}
}
