package com.osunji.melog.elk.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HarmonyReportLog {

	private String reportId;            // 신고 고유 ID
	private String harmonyId;           // 하모니룸 ID
	private String harmonyName;         // 하모니룸 이름
	private String reporterId;          // 신고자 ID
	private String reason;              // 신고 사유
	private String category;            // 신고 카테고리
	private String details;             // 상세 내용
	private LocalDateTime reportTime;   // 신고 시간
}

