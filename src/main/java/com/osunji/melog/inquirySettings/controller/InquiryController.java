package com.osunji.melog.inquirySettings.controller;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.inquirySettings.dto.InquiryRequest;
import com.osunji.melog.inquirySettings.dto.InquiryResponse;
import com.osunji.melog.inquirySettings.service.InquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> agreement(
            @RequestBody InquiryRequest request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<InquiryResponse> response = inquiryService.createAgreement(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
