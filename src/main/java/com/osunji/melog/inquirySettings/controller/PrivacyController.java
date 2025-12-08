package com.osunji.melog.inquirySettings.controller;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.inquirySettings.dto.response.InquiryResponse;
import com.osunji.melog.inquirySettings.dto.response.PrivacyResponse;
import com.osunji.melog.inquirySettings.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class PrivacyController {


    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @GetMapping("/privacy")
    public ResponseEntity<?> agreement() {
        ApiMessage<PrivacyResponse> response = privacyService.privacyService();
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
