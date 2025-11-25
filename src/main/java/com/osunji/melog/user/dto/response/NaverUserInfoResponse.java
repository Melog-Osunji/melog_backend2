package com.osunji.melog.user.dto.response;

import lombok.Getter;

@Getter
public class NaverUserInfoResponse {

    private String resultcode;
    private String message;
    private Response response;

    @Getter
    public static class Response {
        private String id;
        private String email;
        private String nickname;
        private String profile_image;
    }
}
