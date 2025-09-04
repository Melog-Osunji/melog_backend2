package com.osunji.melog.user;

import com.osunji.melog.global.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;



@Service
public class AuthService {

    private final OidcService oidcService;
    private final JWTUtil jwtUtil;

    private static final long ACCESS_TTL_MS  = 1000L * 60 * 15;           // 15분
    private static final long REFRESH_TTL_MS = 1000L * 60 * 60 * 24 * 14; // 14일

    public AuthService(OidcService oidcService, JWTUtil jwtUtil) {
        this.oidcService = oidcService;
        this.jwtUtil = jwtUtil;
    }

    /** 컨트롤러에서 호출: PKCE 교환 + id_token 검증 → 우리 토큰 발급 */
    public RefreshResult handleOidcCallback(String provider, String code, String state, String codeVerifier) {
        if (provider == null || code == null || state == null || codeVerifier == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_fields");
        }

        Map<String, Object> claims = oidcService.exchangeAndVerify(provider, code, state, codeVerifier);

        String sub = (String) claims.get("sub");
        if (sub == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_sub");

        // TODO: provider + sub/email로 유저 조회/생성
        String userId = provider + ":" + sub; // DEMO

        String access  = jwtUtil.createJWT(userId, ACCESS_TTL_MS);
        String refresh = jwtUtil.createRefreshJWT(userId, REFRESH_TTL_MS);
        return new RefreshResult(access, refresh, REFRESH_TTL_MS / 1000);
    }

    /** 컨트롤러에서 호출: Refresh 회전 */
    public RefreshResult rotateTokens(String refreshCookie, HttpServletRequest req) {
        if (refreshCookie == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_refresh_cookie");
        }

        // CSRF 완화(필요 시 강화)
        String origin = req.getHeader("Origin");
        if (!isAllowedOrigin(origin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_origin");
        }
        if (!"XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_xrw");
        }

        try {
            jwtUtil.validateRefresh(refreshCookie);
            String userId = jwtUtil.getUserIdFromRefresh(refreshCookie);

            String newAccess  = jwtUtil.createJWT(userId, ACCESS_TTL_MS);
            String newRefresh = jwtUtil.createRefreshJWT(userId, REFRESH_TTL_MS);
            return new RefreshResult(newAccess, newRefresh, REFRESH_TTL_MS / 1000);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_refresh");
        }
    }

    /** 컨트롤러에서 호출: 로그아웃 */
    public void logout(String refreshCookie) {
        // TODO: jti 저장소 쓰면 여기서 revoke
    }

    // ---- 내부 유틸 ----
    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        return List.of(
                "https://app.melog.com",
                "https://staging.melog.com",
                "http://localhost:3000",
                "http://10.0.2.2:8080"
        ).contains(origin);
    }
}
