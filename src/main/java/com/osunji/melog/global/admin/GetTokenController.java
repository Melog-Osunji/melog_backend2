package com.osunji.melog.global.admin;


import com.osunji.melog.global.common.CookieUtils;
import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import com.osunji.melog.user.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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

    private final long accessTtlMs;
    private final long refreshTtlMs;
    private final long refreshRoateBelow;

    public GetTokenController(
            @Value("${jwt.access-expiration}") long accessTtlMs,
            @Value("${jwt.refresh-expiration}") long refreshTtlMs,
            @Value("${jwt.refresh-below}") long refreshRoateBelow,
            JWTUtil jwtUtil,
            RefreshTokenRepository refreshRepo,
            AuthService authService
    ) {
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
        this.refreshRoateBelow = refreshRoateBelow;
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
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "access", required = false) String accessParam,
            @RequestParam(value = "refresh", required = false) String refreshParam,
            HttpServletRequest req
    ){
        // 0) 필수 파라미터 체크: dev 편의 엔드포인트라도 userId는 반드시 필요
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_userId");
        }

        // 1) 우선 순위대로 refresh 토큰을 찾아온다: param -> cookie -> repo(by userId)
        String refreshFromCookie = CookieUtils.getCookie(req, "refreshCookie");
        String refreshToken = firstNonBlank(refreshParam, refreshFromCookie, refreshRepo.findLatestByUserId(userId).orElse(null));

        boolean hadRefresh = refreshToken != null && !refreshToken.isBlank();
        String mode = "issued"; // issued | rotated | reused | reissued

        // 2) refresh 토큰 검증 & 저장소와의 일치 확인
        String jti = null;

        if (hadRefresh) {
            try {
                jwtUtil.validateRefresh(refreshToken);
                // 저장소의 jti와 일치하는지 확인(탈취/구버전 토큰 방지)
                String repoRefresh = refreshRepo.findLatestByUserId(userId).orElse(null);
                if (repoRefresh == null) {
                    // 저장소에 없는데 클라이언트만 가지고 온 경우: dev 모드에서는 재발급
                    mode = "reissued";
                    refreshToken = reissueRefresh(userId); // 새로 발급 + 저장
                } else {
                    String repoJti = jwtUtil.getJtiFromRefresh(repoRefresh);
                    String reqJti = jwtUtil.getJtiFromRefresh(refreshToken);
                    if (!repoJti.equals(reqJti)) {
                        // 저장소와 요청의 JTI가 다르면 구/탈취 토큰으로 간주 → 재발급
                        mode = "reissued";
                        refreshToken = reissueRefresh(userId);
                    } else {
                        // 남은 TTL 확인해서 회전 여부 결정
                        long expMs = jwtUtil.getRefreshExpiryEpochMillis(refreshToken);
                        long ttlLeftSec = ttlSeconds(expMs);
                        if (ttlLeftSec <= 0) {
                            mode = "reissued";
                            refreshToken = reissueRefresh(userId);
                        } else if (ttlLeftSec * 1000L < refreshRoateBelow) {
                            mode = "rotated";
                            refreshToken = rotateRefresh(userId); // 새 JTI로 회전 + 저장
                        } else {
                            mode = "reused"; // 아직 충분히 남아 재사용
                        }
                    }
                }
            } catch (io.jsonwebtoken.JwtException e) {
                // 서명/만료/형식 문제 → 재발급(dev)
                mode = "reissued";
                refreshToken = reissueRefresh(userId);
            }
        } else {
            // 3) 아예 refresh가 없다면 첫 발급(dev)
            mode = "issued";
            refreshToken = reissueRefresh(userId);
        }

        // 4) access 토큰은 항상 새로 발급
        String accessToken = jwtUtil.createAccessToken(userId, accessTtlMs);

        // 5) 쿠키 갱신(HTTP-only)
        long refreshTtlSec = ttlSeconds(jwtUtil.getRefreshExpiryEpochMillis(refreshToken));
        ResponseCookie cookie = CookieUtils.httpOnlyCookie("refreshCookie", refreshToken, refreshTtlSec);

        // 6) 응답(캐시 금지 + dev 편의로 refresh도 바디에 포함)
        return noStore()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                        "mode", mode,
                        "userId", userId,
                        "accessToken", accessToken,
                        "refreshToken", refreshToken,
                        "refreshTtlSeconds", refreshTtlSec
                ));

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

    private String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

    /** 새 refresh 발급 + 저장소 등록 */
    private String reissueRefresh(String userId) {
        String newRefresh = jwtUtil.createRefreshToken(userId, refreshTtlMs);
        String jti = jwtUtil.getJtiFromRefresh(newRefresh);
        long ttlSec = ttlSeconds(jwtUtil.getRefreshExpiryEpochMillis(newRefresh));
        refreshRepo.save(userId, jti, newRefresh, ttlSec);
        return newRefresh;
    }

    /** 남은 TTL이 임계치 미만일 때 회전(새 JTI로 다시 발급) */
    private String rotateRefresh(String userId) {
        String rotated = jwtUtil.createRefreshToken(userId, refreshTtlMs);
        String jti = jwtUtil.getJtiFromRefresh(rotated);
        long ttlSec = ttlSeconds(jwtUtil.getRefreshExpiryEpochMillis(rotated));
        refreshRepo.save(userId, jti, rotated, ttlSec);
        return rotated;
    }
}
