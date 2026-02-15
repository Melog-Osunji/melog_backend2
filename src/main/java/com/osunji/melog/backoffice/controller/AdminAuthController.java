package com.osunji.melog.backoffice.controller;

import com.osunji.melog.backoffice.dto.AdminLoginRequest;
import com.osunji.melog.backoffice.service.AdminAuthService;
import com.osunji.melog.global.dto.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController { //체크

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiMessage<Map<String, Object>>> login(@Valid @RequestBody AdminLoginRequest request) {
        Map<String, Object> data = adminAuthService.login(request);
        return ResponseEntity.ok(ApiMessage.success(200, "로그인 성공", data));
    }
}
