package com.osunji.melog.user.dto.response;



import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @param refreshToken      회전 없으면 null
 * @param refreshTtlSeconds 회전 없으면 null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefreshResponse(String accessToken, String refreshToken, Long refreshTtlSeconds) {

    public static RefreshResponse rotated(String access, String refresh, Long ttlSeconds) {
        return new RefreshResponse(access, refresh, ttlSeconds);
    }

    public static RefreshResponse accessOnly(String access) {
        return new RefreshResponse(access, null, null);
    }

}


