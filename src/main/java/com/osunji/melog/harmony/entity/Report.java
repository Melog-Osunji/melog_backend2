package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

	/**
	 * 고유 ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	/**
	 * 신고자 (FK)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	/**
	 * 신고 대상 타입 (POST/USER/HARMONY_ROOM)
	 */
	@Column(nullable = false, length = 20)
	private String targetType;

	/**
	 * 신고 대상 ID
	 */
	@Column(nullable = false, columnDefinition = "uuid")
	private UUID targetId;

	/**
	 * 신고 사유
	 */
	@Column(nullable = false, length = 100)
	private String reason;

	/**
	 * 상세 내용
	 */
	@Column(columnDefinition = "TEXT")
	private String details;

	/**
	 * 신고 카테고리
	 */
	@Column(nullable = false, length = 50)
	private String category;

	/**
	 * 신고한 시간
	 */
	@Column(nullable = false)
	private LocalDateTime createdAt;

	// ========== 생성 메서드 ==========

	/**
	 * Report 생성
	 */
	public static Report create(User reporter, String targetType, UUID targetId,
		String reason, String details, String category) {
		Report report = new Report();
		report.reporter = reporter;
		report.targetType = targetType;
		report.targetId = targetId;
		report.reason = reason;
		report.details = details;
		report.category = category;
		report.createdAt = LocalDateTime.now();
		return report;
	}
}
