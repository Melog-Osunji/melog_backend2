package com.osunji.melog.user.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.osunji.melog.elk.repository.ELKUserRepository;
import com.osunji.melog.global.common.RefreshCookieHelper;
import com.osunji.melog.user.dto.request.OauthLoginRequestDTO;
import com.osunji.melog.user.dto.response.LoginResponseDTO;
import com.osunji.melog.user.dto.response.RefreshResponse;
import com.osunji.melog.user.service.AuthService;
import com.osunji.melog.user.dto.RefreshResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Map;

import static com.osunji.melog.global.common.RefreshCookieHelper.REFRESH_COOKIE_NAME;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieHelper cookieHelper;

    public AuthController(AuthService authService, RefreshCookieHelper cookieHelper, ELKUserRepository elkUserRepository) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
    }

    @PostMapping("/login/kakao")
    public ResponseEntity<?> callback(@RequestBody OauthLoginRequestDTO request)
            throws BadJOSEException, ParseException, JOSEException {

        // 1) OIDC 처리
        LoginResponseDTO loginResponse = authService.upsertUserFromKakaoIdToken(request);

        // 2) JWT 발급
        RefreshResult refreshResult = authService.issueJwtForUser(loginResponse.getUser().getId());

        // 3) RefreshToken 쿠키 생성
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshResult.refreshToken())
                .httpOnly(true)   // JS 접근 불가 (보안 필수)
                .secure(true)     // HTTPS에서만 전송
                .path("/")        // 모든 경로에서 유효
                .maxAge(refreshResult.refreshTtlSeconds()) // TTL 설정
                .sameSite("Strict") // CSRF 방지
                .build();

        // 4) 헤더 + 쿠키 설정 후 응답
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + refreshResult.accessToken())   // accessToken 헤더
                .header("X-Refresh-Token", refreshResult.refreshToken())            // refreshToken 헤더
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())           // refreshToken 쿠키
                .header("X-Refresh-TTL", String.valueOf(refreshResult.refreshTtlSeconds())) // 선택: TTL
                .body(loginResponse);
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
    public ResponseEntity<RefreshResponse> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Refresh-Token", required = false) String xRefreshToken,
            HttpServletRequest req
    ) {
        String refreshToken = authService.extractRefreshFromHeaders(authorization, xRefreshToken);
        RefreshResult result = authService.rotateTokens(refreshToken, req);

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        headers.add("Pragma", "no-cache");

        RefreshResponse body = (result.refreshToken() == null)
                ? RefreshResponse.accessOnly(result.accessToken())
                : RefreshResponse.rotated(result.accessToken(), result.refreshToken(), result.refreshTtlSeconds());

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
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
