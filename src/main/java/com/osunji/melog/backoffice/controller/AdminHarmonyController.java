package com.osunji.melog.backoffice.controller;

import com.osunji.melog.backoffice.dto.AdminHarmonyDeleteRequest;
import com.osunji.melog.backoffice.service.AdminHarmonyService;
import com.osunji.melog.global.dto.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/harmonyRoom")
@RequiredArgsConstructor
public class AdminHarmonyController {

    private final AdminHarmonyService adminHarmonyService;

    @GetMapping
    public ResponseEntity<ApiMessage<Map<String, Object>>> searchHarmonyRooms(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page) {
        Map<String, Object> data = adminHarmonyService.searchHarmonyRooms(type, query, page);
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteHarmonyRoom(@RequestBody AdminHarmonyDeleteRequest request) {
        try {
            Map<String, Object> data = adminHarmonyService.deleteHarmonyRoom(request);
            return ResponseEntity.ok(ApiMessage.success(200, "삭제 요청 성공", data));
        } catch (AdminHarmonyService.AdminValidationException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("errors", e.getErrors()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiMessage.fail(404, e.getMessage()));
        }
    }
}
