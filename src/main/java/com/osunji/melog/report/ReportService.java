package com.osunji.melog.report;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.osunji.melog.report.ReportRepository;
import com.osunji.melog.harmony.entity.Report;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ReportService {
	private final ReportRepository reportRepository;

	@Transactional
	public Reports createReport(UUID userId, ReportReason reason, UUID postId, UUID commentId, UUID reportedUserId) {
		Reports report = Reports.builder()
			.reportingUserId(userId)
			.reason(reason)
			.postId(postId)
			.commentId(commentId)
			.reportedUserId(reportedUserId)
			.createdAt(LocalDateTime.now())
			.build();

		return reportRepository.save(report);
	}


}
