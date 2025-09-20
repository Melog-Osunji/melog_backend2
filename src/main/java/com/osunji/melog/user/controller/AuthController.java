package com.osunji.melog.user.controller;

import com.osunji.melog.elk.repository.ELKUserRepository;
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
    private final ELKUserRepository elkUserRepository;

    public AuthController(AuthService authService, RefreshCookieHelper cookieHelper, ELKUserRepository elkUserRepository) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
        this.elkUserRepository = elkUserRepository;
    }
    @PostMapping("/oidc/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> body,
                                      HttpServletRequest req,
                                      HttpServletResponse res) {
        // 파라미터 수정 예정
        String provider     = body.getOrDefault("provider", "kakao");
        String code         = body.get("code");
        String state        = body.get("state");
        String codeVerifier = body.get("code_verifier");

        // 1) OIDC 처리 (토큰 발급 등)
        RefreshResult result = authService.handleOidcCallback(provider, code, state, codeVerifier);

//
////        String clientIp   = extractClientIp(req);
//        String userAgent  = req.getHeader("User-Agent");
//
//        // (선택) 메타데이터로 남길 JSON 문자열
//        String metaJson = String.format("{\"provider\":\"%s\"}", provider);


        // 4) 쿠키/응답 처리
        cookieHelper.setRefreshCookie(res, result.refreshToken(), result.refreshTtlSeconds());
        return ResponseEntity.ok(Map.of("accessToken", result.accessToken()));
    }

//    /** 프록시/로드밸런서 환경 고려한 IP 추출 (추후 추가 예정) 지금은 아님
//    private String extractClientIp(HttpServletRequest request) {
//        // 1) Forwarded 표준 헤더: e.g. "for=203.0.113.195;proto=https;by=203.0.113.43"
//        String forwarded = request.getHeader("Forwarded");
//        if (forwarded != null && !forwarded.isBlank()) {
//            // 가장 앞의 for= 값만 추출
//            for (String part : forwarded.split(";")) {
//                String p = part.trim().toLowerCase();
//                if (p.startsWith("for=")) {
//                    String v = p.substring(4).trim();
//                    // 따옴표 제거 및 IPv6 괄호 제거
//                    v = v.replace("\"", "").replace("[", "").replace("]", "");
//                    // for=client-ip:port 형태일 수 있으니 : 앞부분만
//                    int colon = v.indexOf(':');
//                    return colon > 0 ? v.substring(0, colon) : v;
//                }
//            }
//        }
//
//        // 2) Nginx/ELB 등에서 많이 쓰는 헤더
//        String xff = request.getHeader("X-Forwarded-For");
//        if (xff != null && !xff.isBlank()) {
//            // "client, proxy1, proxy2" → 첫 번째가 실제 클라이언트
//            return xff.split(",")[0].trim();
//        }
//
//        String xri = request.getHeader("X-Real-IP");
//        if (xri != null && !xri.isBlank()) {
//            return xri.trim();
//        }
//
//        // 3) 직접 연결된 클라이언트
//        return request.getRemoteAddr();
//    }

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
