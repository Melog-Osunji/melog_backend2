package com.osunji.melog.global.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieHelper {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    public void setRefreshCookie(HttpServletResponse res, String refreshToken, long maxAgeSec) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)       // HTTPS 필수
                .sameSite("None")   // 크로스 도메인일 때
                .path("/")
                .maxAge(maxAgeSec)
                // .domain("melog.com") // 필요 시 지정
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearRefreshCookie(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }
}
