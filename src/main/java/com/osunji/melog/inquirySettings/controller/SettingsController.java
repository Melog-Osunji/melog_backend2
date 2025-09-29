package com.osunji.melog.inquirySettings.controller;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.inquirySettings.dto.request.SettingsRequest;
import com.osunji.melog.inquirySettings.dto.response.SettingsResponse;
import com.osunji.melog.inquirySettings.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/info")
    public ResponseEntity<?> getSettingsInfo(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<SettingsResponse.infoSettingsResponse> response = settingsService.getInfoSettings(userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/follower")
    public ResponseEntity<?> getAcceptFollow(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<List<SettingsResponse.FollowResponse>> response;
        response = settingsService.getFollow(userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/block")
    public ResponseEntity<?> getBlock(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<List<SettingsResponse.FollowResponse>> response;
        response = settingsService.getBlock(userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/follower/accept")
    public ResponseEntity<?> postAcceptFollow(
            @RequestBody SettingsRequest request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<SettingsResponse.CheckResponse> response;
        response = settingsService.postAcceptUser(userId, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/follower/block")
    public ResponseEntity<?> postBlockUser(
            @RequestBody SettingsRequest request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<SettingsResponse.CheckResponse> response;
        response = settingsService.postBlockUser(userId, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }



}
