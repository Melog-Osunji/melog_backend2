package com.osunji.melog.backoffice.controller;

import com.osunji.melog.backoffice.service.AdminStatisticsService;
import com.osunji.melog.global.dto.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/users/social-login")
    public ResponseEntity<ApiMessage<Map<String, Object>>> getSocialLoginStatistics() {
        Map<String, Object> data = adminStatisticsService.getSocialLoginStatistics();
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }

    @GetMapping("/users/active-time")
    public ResponseEntity<ApiMessage<Map<String, Object>>> getActiveTimeStatistics() {
        Map<String, Object> data = adminStatisticsService.getActiveTimeStatistics();
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }
}
