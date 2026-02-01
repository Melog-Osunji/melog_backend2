package com.osunji.melog.backoffice.controller;

import com.osunji.melog.backoffice.service.AdminCalendarService;
import com.osunji.melog.global.dto.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/calendar")
@RequiredArgsConstructor
public class AdminCalendarController {

    private final AdminCalendarService adminCalendarService;

    @GetMapping
    public ResponseEntity<ApiMessage<Map<String, Object>>> getCalendarEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page) {
        if ((fromDate != null && toDate == null) || (fromDate == null && toDate != null)) {
            return ResponseEntity.badRequest()
                    .body(ApiMessage.fail(400, "fromDate와 toDate는 함께 사용해야 합니다."));
        }
        Map<String, Object> data = adminCalendarService.getCalendarEvents(type, fromDate, toDate, page);
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }
}
