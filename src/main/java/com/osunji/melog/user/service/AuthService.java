package com.osunji.melog.user.service;

import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import com.osunji.melog.user.dto.RefreshResult;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import com.osunji.melog.user.repository.UserRepository;
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
    private final RefreshTokenRepository refreshRepo;
    private final UserRepository userRepository;

    // 나중에 properties로 옮길 예정
    private static final long ACCESS_TTL_MS  = 1000L * 60 * 15;           // 15분
    private static final long REFRESH_TTL_MS = 1000L * 60 * 60 * 24 * 14; // 14일

    // 남은 TTL이 이 값 이하일 때만 refresh 교체 3일
    private static final long REFRESH_ROTATE_BELOW_SEC = 60L * 60 * 24 * 3;

    public AuthService(OidcService oidcService, JWTUtil jwtUtil, RefreshTokenRepository refreshRepo, UserRepository userRepository) {
        this.oidcService = oidcService;
        this.jwtUtil = jwtUtil;
        this.refreshRepo = refreshRepo;
        this.userRepository = userRepository;
    }

    /** 컨트롤러 시그니처: provider, code, state, code_verifier */
    public RefreshResult handleOidcCallback(String provider, String code, String state, String codeVerifier) {
        if (provider == null || code == null || state == null || codeVerifier == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_fields");
        }

        Map<String, Object> claims = oidcService.exchangeAndVerify(provider, code, state, codeVerifier);

        String sub = (String) claims.get("sub");
        if (sub == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_sub");
        String email = (String) claims.get("email");
        String nickname = (String) claims.get("name");
        String picture = (String) claims.get("picture");

        // 1. DB 조회
        User user = userRepository.findByOidcAndPlatform(sub, Platform.valueOf(provider.toUpperCase()))
                .orElseGet(() -> {
                    // 2. 없으면 새 유저 생성
                    User newUser = new User(email, Platform.valueOf(provider.toUpperCase()), nickname, picture, null);
                    newUser.setOidc(sub);
                    return userRepository.save(newUser);
                });

        // 토큰 생성
        String userId = user.getId();
        String access  = jwtUtil.createAccessToken(userId, ACCESS_TTL_MS);
        String refresh = jwtUtil.createRefreshToken(userId, REFRESH_TTL_MS);

        String jti = jwtUtil.getJtiFromRefresh(refresh);
        long ttlSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(refresh));
        refreshRepo.save(userId, jti, refresh, ttlSec);

        return new RefreshResult(access, refresh, ttlSec);
    }

    /** 컨트롤러 시그니처: rotateTokens(String refreshCookie, HttpServletRequest req) */
    public RefreshResult rotateTokens(String refreshCookie, HttpServletRequest req) {
        if (refreshCookie == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_refresh_cookie");
        }

        // (선택) CSRF 완화: Origin / Custom Header 확인
        String origin = req.getHeader("Origin");
        if (!isAllowedOrigin(origin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_origin");
        }
        if (!"XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_xrw");
        }

        try {
            // 1) 토큰 검증
            jwtUtil.validateRefresh(refreshCookie);
            String userId = jwtUtil.getUserIdFromRefresh(refreshCookie);
            String oldJti = jwtUtil.getJtiFromRefresh(refreshCookie);

            // 2) 재사용/위조 탐지
            if (!refreshRepo.existsAndMatch(userId, oldJti, refreshCookie)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh_reuse_or_revoked");
            }

            // 3) 남은 TTL 계산
            long remainingSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(refreshCookie));

            // 4) 액세스만 재발급 (리프레시 충분히 남음)
            if (remainingSec > REFRESH_ROTATE_BELOW_SEC) {
                String newAccess = jwtUtil.createAccessToken(userId, ACCESS_TTL_MS);
                // 저장소 변경 없음, 기존 refresh 그대로 사용
                return new RefreshResult(newAccess, refreshCookie, remainingSec);
            }

            // 5) 리프레시 교체 (만료 임박)
            String newAccess  = jwtUtil.createAccessToken(userId, ACCESS_TTL_MS);
            String newRefresh = jwtUtil.createRefreshToken(userId, REFRESH_TTL_MS);

            String newJti = jwtUtil.getJtiFromRefresh(newRefresh);
            long newTtlSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(newRefresh));

            // 저장은 새 키를 먼저, 그 다음 기존 키 삭제(짧은 경합 윈도우 최소화)
            refreshRepo.save(userId, newJti, newRefresh, newTtlSec);
            refreshRepo.delete(userId, oldJti);

            return new RefreshResult(newAccess, newRefresh, newTtlSec);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_refresh");
        }
    }

    public void logout(String refreshCookie) {
        if (refreshCookie == null) return;
        try {
            String userId = jwtUtil.getUserIdFromRefresh(refreshCookie);
            String jti = jwtUtil.getJtiFromRefresh(refreshCookie);
            refreshRepo.delete(userId, jti);
        } catch (Exception ignored) {
            // 이미 만료/삭제 등
        }
    }

    // ===== 내부 유틸 =====
    private static long ttlSecondsFromNow(long expEpochMillis) {
        long now = System.currentTimeMillis();
        return Math.max(0, (expEpochMillis - now) / 1000);
    }

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

