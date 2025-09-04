package com.osunji.melog.global.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtil {
    private final SecretKey secretKey;
    private final SecretKey refreshSecretKey;

    public JWTUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.refresh}") String refresh) {
        this.secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
        this.refreshSecretKey = new SecretKeySpec(
                refresh.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    /* ===================== Access Token ===================== */

    public String createAccessToken(String userId, long ttlMillis) {
        return Jwts.builder()
                .claim("userId", userId)
                .issuer("melog-api")
                .audience().add("melog-client").and()
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(secretKey)
                .compact();
    }

    public void validateAccess(String token) {
        Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token); // 서명/만료/형식 체크
    }

    public String getUserIdFromAccess(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", String.class);
    }

    /* ===================== Refresh Token ===================== */

    public String createRefreshToken(String userId, long ttlMillis) {
        String jti = UUID.randomUUID().toString();
        return createRefreshToken(userId, jti, ttlMillis);
    }

    public String createRefreshToken(String userId, String jti, long ttlMillis) {
        return Jwts.builder()
                .claim("userId", userId)
                .issuer("melog-api")
                .audience().add("melog-client").and()
                .id(jti) // ★ Redis key로 사용
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(refreshSecretKey)
                .compact();
    }

    public void validateRefresh(String token) {
        Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token);
    }

    public String getUserIdFromRefresh(String token) {
        return Jwts.parser().verifyWith(refreshSecretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", String.class);
    }

    public String getJtiFromRefresh(String token) {
        return Jwts.parser().verifyWith(refreshSecretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .getId();
    }

    public long getRefreshExpiryEpochMillis(String token) {
        Date exp = Jwts.parser().verifyWith(refreshSecretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return exp.getTime();
    }

    /* ===================== 호환용 (기존 메서드) ===================== */
    public String createJWT(String userId, Long expiredMillis) {
        return createAccessToken(userId, expiredMillis);
    }

    public String createRefreshJWT(String userId, Long refreshMillis) {
        return createRefreshToken(userId, refreshMillis);
    }

    public String getUserIdFromToken(String token) {
        return getUserIdFromAccess(token);
    }

    public Boolean isTokenExpired(String token) {
        try {
            Date exp = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
