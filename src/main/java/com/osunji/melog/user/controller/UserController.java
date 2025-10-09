package com.osunji.melog.user.controller;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.service.UserProfileMusicService;
import com.osunji.melog.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserProfileMusicService userProfileMusicService;

    public UserController(UserService userService, UserProfileMusicService userProfileMusicService) {
        this.userService = userService;
        this.userProfileMusicService = userProfileMusicService;
    }


    // 약관 동의 -> 마케팅 여부 저장
    @PostMapping("/agreement")
    public ResponseEntity<?> agreement(
            @RequestBody UserRequest.agreement request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.AgreementResponse> response = userService.createAgreement(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PatchMapping("/marketing")
    public ResponseEntity<?> marketing(
            @RequestBody UserRequest.agreement request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.AgreementResponse> response = userService.updateMarketing(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }


    @GetMapping("/marketing")
    public ResponseEntity<?> getMarketing(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.AgreementResponse> response = userService.getMarketing(userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }


    // 온보딩 생성
    @PostMapping("/onboarding")
    public ResponseEntity<?> onboarding(
            @RequestBody UserRequest.onboarding request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.OnboardingResponse> response = userService.onboarding(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // 온보딩 업데이트
    @PatchMapping("/onboarding")
    public ResponseEntity<?> patchOnboarding(
            @RequestBody UserRequest.onboardingPatch request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<?> response = userService.patchOnboarding(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // 온보딩
    @GetMapping("/onboarding")
    public ResponseEntity<?> getOnboarding(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<?> response = userService.getOnboarding(userId);
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

    // 프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.ProfileResponse> response = userService.getProfile(userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // 팔로우/언팔로우
    @PostMapping("/following")
    public ResponseEntity<?> following(
            @RequestBody UserRequest.following request,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.followingResponse> response = userService.following(request, userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    // 팔로우 여부 조회
    @GetMapping("/following/{nickname:[A-Za-z0-9가-힣._-]{2,20}}")
    public ResponseEntity<?> getFollowing(
            @PathVariable String nickname,
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        // 언더스코어를 공백으로 복원
        String realNickname = nickname.replace("_", " ");

        ApiMessage<UserResponse.followingCheckResponse> response =
                userService.followingListByNickname(userId, realNickname);

        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping("/myPage")
    public ResponseEntity<?> myPage(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId
    ) {
        ApiMessage<UserResponse.MyPageResponse> response = userService.getMyPage(userId);
        return ResponseEntity.status(response.getCode()).body(response);
    }

//    @PostMapping("/myPage/musicChange")
//    public ResponseEntity<?> updateProfileMusic(
//            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId,
//            @RequestBody Map<String, String> body
//    ) {
//        String youtube = body.get("youtube");
//        String title   = body.get("title");
//        userProfileMusicService.setActive(userId, youtube, title);
//        return ResponseEntity.ok(ApiMessage.success(200,"success",title));
//    }

}
