package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.osunji.melog.user.domain.User;

@Entity
@Table(name = "harmony_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HarmonyRoomReport {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "harmony_room_id", nullable = false)
	private HarmonyRoom harmonyRoom;

	@Column(nullable = false, length = 50)
	private String reason;

	@Column(nullable = false, length = 50)
	private String category;

	@Column(columnDefinition = "TEXT")
	private String details;

	@Column(nullable = false)
	private LocalDateTime reportedAt;

	public static HarmonyRoomReport create(User reporter, HarmonyRoom harmonyRoom,
		String reason, String category, String details) {
		HarmonyRoomReport report = new HarmonyRoomReport();
		report.reporter = reporter;
		report.harmonyRoom = harmonyRoom;
		report.reason = reason;
		report.category = category;
		report.details = details;
		report.reportedAt = LocalDateTime.now();
		return report;
	}
}

