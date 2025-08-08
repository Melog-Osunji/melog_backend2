package com.osunji.melog.user.dto;

import com.osunji.melog.user.domain.User;

public record OauthLoginResponseDTO(
        String accessToken,
        boolean isNewUser,
        UserInfo user
) {
    public record UserInfo(
            String id,
            String email,
            String platform,
            String nickName,
            String profileImg,
            String intro
    ) {}

    public static OauthLoginResponseDTO of(String accessToken, boolean isNewUser, User entity) {
        return new OauthLoginResponseDTO(
                accessToken,
                isNewUser,
                new UserInfo(
                        entity.getId(),
                        entity.getEmail(),
                        entity.getPlatform(),
                        entity.getNickname(),
                        entity.getProfileImageUrl(),
                        entity.getIntro()
                )
        );
    }
}
