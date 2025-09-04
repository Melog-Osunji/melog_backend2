package com.osunji.melog.user.controller;

import com.osunji.melog.global.common.RefreshCookieHelper;
import com.osunji.melog.user.service.AuthService;
import com.osunji.melog.user.dto.RefreshResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.osunji.melog.global.common.RefreshCookieHelper.REFRESH_COOKIE_NAME;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieHelper cookieHelper;

    public AuthController(AuthService authService, RefreshCookieHelper cookieHelper) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
    }

    @PostMapping("/oidc/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> body, HttpServletResponse res) {
        String provider = body.getOrDefault("provider", "kakao"); // "kakao" 기본
        String code = body.get("code");
        String state = body.get("state");
        String codeVerifier = body.get("code_verifier");

        RefreshResult result = authService.handleOidcCallback(provider, code, state, codeVerifier);

        cookieHelper.setRefreshCookie(res, result.refreshToken(), result.refreshTtlSeconds());
        return ResponseEntity.ok(Map.of("accessToken", result.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refresh,
            HttpServletRequest req,
            HttpServletResponse res
    ) {
        RefreshResult result = authService.rotateTokens(refresh, req);
        cookieHelper.setRefreshCookie(res, result.refreshToken(), result.refreshTtlSeconds());
        return ResponseEntity.ok(Map.of("accessToken", result.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refresh,
            HttpServletResponse res
    ) {
        authService.logout(refresh);
        cookieHelper.clearRefreshCookie(res);
        return ResponseEntity.noContent().build();
    }

}
