package com.osunji.melog.user.dto;

/** 서비스 ↔ 컨트롤러 간 토큰 전달 DTO */
public class RefreshResult {
    private final String accessToken;
    private final String refreshToken;
    private final Long refreshTtlSeconds;

    public RefreshResult(String accessToken, String refreshToken, Long refreshTtlSeconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    // 컨트롤러에서 쓰는 메서드 이름과 정확히 일치시켰음
    public String accessToken() { return accessToken; }
    public String refreshToken() { return refreshToken; }
    public Long refreshTtlSeconds() { return refreshTtlSeconds; }
}

