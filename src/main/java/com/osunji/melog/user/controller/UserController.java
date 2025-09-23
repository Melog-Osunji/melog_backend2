package com.osunji.melog.user.controller;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    // 약관 동의 저장
    @PostMapping("/agreement")
    public ResponseEntity<?> agreement(
            @RequestBody UserRequest.agreement request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage response = userService.agreement(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // 온보딩
    @PostMapping("/onboarding")
    public ResponseEntity<?> onboarding(
            @RequestBody UserRequest.onboarding request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage response = userService.onboarding(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // 온보딩
    @PatchMapping("/onboarding")
    public ResponseEntity<?> patchOnboarding(
            @RequestBody UserRequest.onboardingPatch request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<?> response = userService.patchOnboarding(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // 프로필 업데이트
    @PatchMapping("/profile")
    public ResponseEntity<?> profile(
            @RequestBody UserRequest.profilePatch request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.ProfileResponse> response = userService.profile(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

}
