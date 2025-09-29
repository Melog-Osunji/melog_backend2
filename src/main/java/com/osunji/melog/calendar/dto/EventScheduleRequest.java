package com.osunji.melog.calendar.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventScheduleRequest {
    @NotNull
    private UUID eventId;       // 캘린더(공연) ID

    @NotNull
    private LocalDate eventDate;

    private boolean schedule;   // true=저장, false=삭제
    private boolean alarm;      // 알림 on/off
    private String  alarmTime;  // "HH:mm" (옵션)
}