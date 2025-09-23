package com.osunji.melog.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

public class UserResponse {
    @Getter
    @Builder
    public static class AgreementResponse {
        private String id;
        private boolean marketing;
        private String createdAt; // ISO-8601 문자열 (예: 2025-07-18T11:30:00)
    }

    @Getter
    @Builder
    public static class OnboardingResponse{
        private String id;
        private String userId;
        private List<String> composer;
        private List<String> period;
        private List<String> instrument;

    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProfileResponse {
        private String id;
        private String email;
        private String platform;   // e.g. "kakao"
        private String nickName;   // GET_FROM_KAKAO or updated
        private String profileImg; // GET_FROM_KAKAO or updated
        private String intro;      // updated
    }

    @Getter
    @Builder
    public static class MarketingResponse{
        private String userId;
        private boolean marketing;
        private String createdAt; // ISO-8601 문자열 (예: 2025-07-18T11:30:00)
    }
}
