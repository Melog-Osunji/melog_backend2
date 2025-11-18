package com.osunji.melog.calendar.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventScheduleRequest {

    // --- 스케줄/알림 토글 공통 ---
    @NotNull
    private UUID eventId;        // 캘린더 PK(UUID) - 클라이언트 제공/고정

    @NotNull
    private LocalDate eventDate; // 스케줄 날짜

    private boolean schedule;    // true=저장, false=삭제
    private boolean alarm;       // 알림 on/off
    private String  alarmTime;   // "HH:mm" (옵션)

    // --- 캘린더 생성시 사용할 필드 (없으면 생성 X) ---
    // 생성 트리거: 해당 eventId가 DB에 없을 때만 사용됨
    private String source;         // 예: "KCISA_CNV_060" / "USER"
    private String externalId;     // 외부 식별자
    private String detailUrl;
    private String title;          // 필수
    private String classification;
    private String region;
    private LocalDate startDate;   // 필수
    private LocalDate endDate;
    private String description;
    private String imageUrl;
}
