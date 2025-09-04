package com.osunji.melog.global.admin;

import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

    @RestController
    @RequestMapping("/api/dev")
    public class GetTokenController {

        private final JWTUtil jwtUtil;
        private final RefreshTokenRepository refreshRepo;

        // 기본값(개발용) 제공. 필요하면 application-<profile>.yml에서 override
        @Value("${dev.token.access-ttl-ms:900000}")   // 15분
        private long accessTtlMs;

        @Value("${dev.token.refresh-ttl-ms:86400000}") // 1일
        private long refreshTtlMs;

        public GetTokenController(JWTUtil jwtUtil, RefreshTokenRepository refreshRepo) {
            this.jwtUtil = jwtUtil;
            this.refreshRepo = refreshRepo;
        }

        /**
         * 예: GET /api/dev/token?userId=test123
         * access/refresh 토큰 생성 후 JSON 반환.
         * refresh는 Redis에도 저장(회전/재사용 방지 로직과 정합성 유지).
         */
        @GetMapping("/token")
        public Map<String, Object> getToken(@RequestParam String userId) {
            if (userId == null || userId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_userId");
            }

            String access  = jwtUtil.createAccessToken(userId, accessTtlMs);
            String refresh = jwtUtil.createRefreshToken(userId, refreshTtlMs);

            String jti = jwtUtil.getJtiFromRefresh(refresh);
            long ttlSec = Math.max(0, (jwtUtil.getRefreshExpiryEpochMillis(refresh) - System.currentTimeMillis()) / 1000);
            refreshRepo.save(userId, jti, refresh, ttlSec);

            return Map.of(
                    "userId", userId,
                    "accessToken", access,
                    "refreshToken", refresh,
                    "refreshTtlSeconds", ttlSec
            );
        }
    }
