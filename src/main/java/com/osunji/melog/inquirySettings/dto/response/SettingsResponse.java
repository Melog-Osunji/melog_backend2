package com.osunji.melog.inquirySettings.dto.response;

import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public class SettingsResponse {

    @Getter
    @Builder
    public static class infoSettingsResponse {
        private UUID userId;
        private Platform platform;   // KAKAO / GOOGLE / NAVER
        private String email;
        private boolean isActive;
        private String language;     // kor, eng 등
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FollowResponse {
        private UUID userId;
        private String profileImg;
        private String nickname;
        private String description;

        public static FollowResponse from(User u) {
            return FollowResponse.builder()
                    .userId(u.getId())
                    .profileImg(u.getProfileImageUrl())
                    .nickname(u.getNickname())
                    .description(u.getIntro())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CheckResponse {
        private UUID userId;

    }
}