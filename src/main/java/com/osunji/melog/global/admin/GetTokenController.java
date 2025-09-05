package com.osunji.melog.global.admin;


import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import com.osunji.melog.user.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class GetTokenController {

    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshRepo;
    private final AuthService authService;

    @Value("${dev.token.access-ttl-ms:900000}")     // 15분
    private long accessTtlMs;

    @Value("${dev.token.refresh-ttl-ms:86400000}")  // 1일 (dev)
    private long refreshTtlMs;

    public GetTokenController(JWTUtil jwtUtil,
                              RefreshTokenRepository refreshRepo,
                              AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.refreshRepo = refreshRepo;
        this.authService = authService;
    }

    @GetMapping("/debug-validate")
    public String debug(@RequestParam String token) {
        try {
            jwtUtil.validateAccess(token);
            return "valid, userId=" + jwtUtil.getUserIdFromAccess(token);
        } catch (Exception e) {
            return "invalid: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    /**
     * 통합 엔드포인트(개발용):
     * - 신규 발급:  GET /api/dev/token?userId=alice
     * - 자동 갱신:  GET /api/dev/token?access=<액세스토큰>  (refresh 쿠키 필요)
     *   액세스가 유효하면 그대로 반환, 만료면 refresh로 선택적 회전(액세스만/회전)
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> issueOrRefresh(
            @RequestParam(required = false) String userId,
            @RequestParam(value = "access", required = false) String accessParam,
            HttpServletRequest req
    ) {
        // 1) 신규 발급 (dev 편의)
        if (userId != null && !userId.isBlank()) {
            var access  = jwtUtil.createAccessToken(userId, accessTtlMs);
            var refresh = jwtUtil.createRefreshToken(userId, refreshTtlMs);

            var jti    = jwtUtil.getJtiFromRefresh(refresh);
            var ttlSec = ttlSeconds(jwtUtil.getRefreshExpiryEpochMillis(refresh));
            refreshRepo.save(userId, jti, refresh, ttlSec);

            return noStore().body(Map.of(
                    "mode", "issued",
                    "userId", userId,
                    "accessToken", access,
                    "refreshToken", refresh,          // dev 편의상 바디 포함(운영: 쿠키 권장)
                    "refreshTtlSeconds", ttlSec
            ));
        }

        // 2) 자동 갱신: access를 쿼리로 받음
        if (accessParam != null && !accessParam.isBlank()) {
            try {
                jwtUtil.validateAccess(accessParam); // 유효하면 그대로 리턴
                var uid = jwtUtil.getUserIdFromAccess(accessParam);
                return noStore().body(Map.of(
                        "mode", "access_valid",
                        "userId", uid,
                        "accessToken", accessParam
                ));
            } catch (ExpiredJwtException expired) {
                // 만료 → refresh 쿠키로 갱신/회전
                var refreshCookie = extractCookie(req, "refresh");
                if (refreshCookie == null || refreshCookie.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_refresh_cookie");
                }

                var result  = authService.rotateTokens(refreshCookie, req);
                var rotated = !refreshCookie.equals(result.refreshToken());

                var rb = noStore();
                if (rotated) {
                    rb.header("Set-Cookie",
                            buildRefreshSetCookie(result.refreshToken(), (int) result.refreshTtlSeconds()));
                }

                return rb.body(Map.of(
                        "mode", rotated ? "refreshed_rotated" : "refreshed_access_only",
                        "userId", jwtUtil.getUserIdFromAccess(result.accessToken()),
                        "accessToken", result.accessToken(),
                        "refreshTtlSeconds", result.refreshTtlSeconds()
                ));
            } catch (JwtException invalid) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "access_invalid");
            }
        }

        // 파라미터 부족
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_userId_or_access");
    }

    // ===== Helpers =====

    private static long ttlSeconds(long expEpochMillis) {
        var now = System.currentTimeMillis();
        return Math.max(0, (expEpochMillis - now) / 1000);
    }

    private static String extractCookie(HttpServletRequest req, String name) {
        var cs = req.getCookies();
        if (cs == null) return null;
        for (Cookie c : cs) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    private static ResponseEntity.BodyBuilder noStore() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0");
    }

    private static String buildRefreshSetCookie(String refresh, int ttlSeconds) {
        return new StringBuilder()
                .append("refresh=").append(refresh)
                .append("; HttpOnly; SameSite=Strict")
                .append("; Path=/")
                .append("; Max-Age=").append(Math.max(1, ttlSeconds))
                .append("; Secure") // https 운영 권장
                .toString();
    }
}
