package com.osunji.melog.global.common;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

public class CookieUtils {
    private CookieUtils() {}

    public static String getCookie(HttpServletRequest req, String name) {
        var cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    public static ResponseCookie httpOnlyCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)          // 운영시 변경
                .sameSite("Strict")
                .path("/")
                .maxAge(Math.max(1, maxAgeSeconds))
                .build();
    }
}
