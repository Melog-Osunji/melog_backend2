package com.osunji.melog.user.dto.response;

import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Getter
    @Builder
    public static class followingResponse{
        private String userId;
        private String followingId;
        private String msg;
    }

    @Getter
    @Builder
    public static class followingCheckResponse{
        private boolean result;
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class MyPageResponse {
        private String profileImg;
        private String nickname;
        private String introduction;

        private ProfileMusic profileMusic;

        private long followers;   // 나를 팔로우하는 사람 수
        private long followings;  // 내가 팔로우하는 사람 수

        private List<HarmonyRoomItem> harmonyRooms;

        private List<FilterPostResponse.FeedPostData> posts;
        private List<FilterPostResponse.FeedPostData> mediaPosts;
        private List<BookmarkResponse.BookmarkData> bookmarks;

    }

    @Getter  @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class ProfileMusic {
        private String youtube;   // 예: https://youtube.com/watch?v=...
        private String title;     // 예: Example Song
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class HarmonyRoomItem {
        private UUID roomId;
        private String roomName;
        private boolean isManager;
        private String roomImg;
        private boolean bookmark;
    }
}
