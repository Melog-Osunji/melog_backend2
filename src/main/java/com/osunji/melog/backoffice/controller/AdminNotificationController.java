package com.osunji.melog.backoffice.controller;

import com.osunji.melog.backoffice.dto.AdminNotificationCreateRequest;
import com.osunji.melog.backoffice.service.AdminNotificationService;
import com.osunji.melog.global.dto.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    public ResponseEntity<ApiMessage<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> data = adminNotificationService.getNotifications(page, size);
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }

    @PostMapping("/post")
    public ResponseEntity<ApiMessage<Map<String, Object>>> createNotification(
            @Valid @RequestBody AdminNotificationCreateRequest request) {
        Map<String, Object> data = adminNotificationService.createNotification(request);
        return ResponseEntity.ok(ApiMessage.success(200, "알림 작성 성공", data));
    }
}
