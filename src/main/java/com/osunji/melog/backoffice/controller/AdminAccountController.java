package com.osunji.melog.backoffice.controller;

import com.osunji.melog.backoffice.dto.AdminAccountDeleteRequest;
import com.osunji.melog.backoffice.service.AdminAccountService;
import com.osunji.melog.global.dto.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    public ResponseEntity<ApiMessage<Map<String, Object>>> getAccounts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page) {
        Map<String, Object> data = adminAccountService.getAccounts(type, query, page);
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiMessage<Void>> deleteAccounts(@Valid @RequestBody AdminAccountDeleteRequest request) {
        adminAccountService.deleteAdminAccounts(request);
        return ResponseEntity.ok(ApiMessage.success(200, "삭제 성공", null));
    }
}
