package com.osunji.melog.user.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {

    private final StringRedisTemplate redis;

    public RefreshTokenRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static String key(String userId, String jti) {
        return "refresh:%s:%s".formatted(userId, jti);
    }

    /** 토큰 저장 (value는 해시로 저장) */
    public void save(String userId, String jti, String refreshToken, long ttlSeconds) {
        String k = key(userId, jti);
        String v = sha256(refreshToken);
        redis.opsForValue().set(k, v, Duration.ofSeconds(ttlSeconds));
    }

    /** 토큰 일치 여부 확인 (재사용/위조 탐지에 사용) */
    public boolean existsAndMatch(String userId, String jti, String refreshToken) {
        String v = redis.opsForValue().get(key(userId, jti));
        if (v == null) return false;
        return v.equals(sha256(refreshToken));
    }

    /** 토큰 삭제 (로그아웃, 갱신 이전 토큰 제거) */
    public void delete(String userId, String jti) {
        redis.delete(key(userId, jti));
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unsupported", e);
        }
    }

    public Optional<String> findLatestByUserId(String userId) {
        String v = redis.opsForValue().get(userId);
        return Optional.ofNullable(v).filter(s -> !s.isBlank());
    }

}
