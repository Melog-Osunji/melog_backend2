package com.osunji.melog.backoffice.controller;

import com.osunji.melog.backoffice.dto.AdminInquiryAnswerRequest;
import com.osunji.melog.backoffice.service.AdminInquiryService;
import com.osunji.melog.global.dto.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @GetMapping("/api/admin/inquiries")
    public ResponseEntity<ApiMessage<Map<String, Object>>> getInquiries(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if ((fromDate != null && toDate == null) || (fromDate == null && toDate != null)) {
            return ResponseEntity.badRequest()
                    .body(ApiMessage.fail(400, "fromDate와 toDate는 함께 사용해야 합니다."));
        }
        if ((fromDate != null) && (year != null || month != null)) {
            return ResponseEntity.badRequest()
                    .body(ApiMessage.fail(400, "fromDate/toDate와 year/month는 동시에 사용할 수 없습니다."));
        }
        Map<String, Object> data = adminInquiryService.getInquiries(
                type, query, fromDate, toDate, year, month, page, size);
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }

    @GetMapping("/api/admin/inquiries/{inquiryId}")
    public ResponseEntity<ApiMessage<Map<String, Object>>> getInquiryDetail(
            @PathVariable UUID inquiryId) {
        Map<String, Object> data = adminInquiryService.getInquiryDetail(inquiryId);
        return ResponseEntity.ok(ApiMessage.success(200, "조회 성공", data));
    }

    @PutMapping("/api/admin/inquiries/{inquiryId}/answer/draft")
    public ResponseEntity<ApiMessage<Void>> saveDraft(
            @PathVariable UUID inquiryId,
            @Valid @RequestBody AdminInquiryAnswerRequest request) {
        adminInquiryService.saveDraft(inquiryId, request.getAnswer());
        return ResponseEntity.ok(ApiMessage.success(200, "임시저장 성공", null));
    }

    @PutMapping("/api/manage/inquiries/{inquiryId}/answer/publish")
    public ResponseEntity<ApiMessage<Void>> publishAnswer(
            @PathVariable UUID inquiryId,
            @Valid @RequestBody AdminInquiryAnswerRequest request) {
        adminInquiryService.publishAnswer(inquiryId, request.getAnswer());
        return ResponseEntity.ok(ApiMessage.success(200, "답변 전송 성공", null));
    }
}
