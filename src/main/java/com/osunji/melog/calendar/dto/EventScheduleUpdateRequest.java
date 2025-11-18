package com.osunji.melog.calendar.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 일정/알림 동시 삭제 요청 DTO
 * - scheduleId: 삭제 대상 EventSchedule의 UUID (필수)
 */
@Getter
@Setter
@NoArgsConstructor
public class EventScheduleUpdateRequest {

    @NotNull(message = "scheduleId는 필수입니다.")
    private UUID scheduleId;
}
