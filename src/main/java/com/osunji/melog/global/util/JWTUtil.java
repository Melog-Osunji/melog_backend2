package com.osunji.melog.global.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

    public JWTUtil(@org.springframework.beans.factory.annotation.Value("${jwt.secret}") String secretB64Url,
                   @org.springframework.beans.factory.annotation.Value("${jwt.refresh}") String refreshB64Url) {
        // Base64URL로 고정 사용
        byte[] accessBytes  = Decoders.BASE64URL.decode(secretB64Url.trim());
        byte[] refreshBytes = Decoders.BASE64URL.decode(refreshB64Url.trim());
        if (accessBytes.length < 32 || refreshBytes.length < 32) {
            throw new IllegalArgumentException("HS256 secret must be ≥ 32 bytes.");
        }
        this.secretKey = Keys.hmacShaKeyFor(accessBytes);
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshBytes);
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
                .id(jti)
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
