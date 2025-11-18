package com.osunji.melog.calendar.controller;

import com.osunji.melog.calendar.dto.EventScheduleRequest;
import com.osunji.melog.calendar.dto.EventScheduleResponse;
import com.osunji.melog.calendar.dto.EventScheduleUpdateRequest;
import com.osunji.melog.calendar.service.EventScheduleService;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class EventScheduleController {
    private final EventScheduleService eventScheduleService;

    @PostMapping("/event/save")
    public ResponseEntity<?> eventSchedule(
            @RequestBody EventScheduleRequest request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<EventScheduleResponse> response = eventScheduleService.saveOrDeleteSchedule(userId, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @DeleteMapping("/event/delete")
    public ResponseEntity<?> eventScheduleUpdate(
            @RequestBody EventScheduleUpdateRequest request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<EventScheduleResponse> response = eventScheduleService.deleteScheduleAndAlarm(userId, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

}
