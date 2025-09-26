package com.osunji.melog.calendar.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;


// Response
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventScheduleResponse {
    private UUID eventId;
    private LocalDate eventDate;
    private boolean  schedule;
    private boolean  alarm;
    private String   alarmTime; // "HH:mm"
}


