package com.osunji.melog.global.util;

import io.jsonwebtoken.Claims;
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
            .clockSkewSeconds(600)
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
            .clockSkewSeconds(600)
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

    /* ===================== 디버깅용 메서드 (새 JJWT 버전) ===================== */

    /** 토큰에서 UserId 추출 - 디버깅 버전 */
    public String getUserIdFromToken(String token) {
        try {
            System.out.println("🔍 JWTUtil.getUserIdFromToken 시작");
            System.out.println("입력된 토큰 길이: " + token.length());
            System.out.println("토큰 시작 부분: " + token.substring(0, Math.min(50, token.length())));

            // ✅ 새 JJWT 버전에 맞는 파싱 방법
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)  // ✅ setSigningKey → verifyWith
                .build()
                .parseSignedClaims(token)  // ✅ parseClaimsJws → parseSignedClaims
                .getPayload();  // ✅ getBody → getPayload

            String userId = claims.get("userId", String.class);
            System.out.println("JWT에서 추출된 userId: '" + userId + "'");
            System.out.println("userId 타입: " + (userId != null ? userId.getClass().getSimpleName() : "null"));
            System.out.println("userId 길이: " + (userId != null ? userId.length() : 0));

            // 추가 클레임 정보
            System.out.println("모든 클레임:");
            claims.forEach((key, value) -> System.out.println("  " + key + ": " + value + " (" + (value != null ? value.getClass().getSimpleName() : "null") + ")"));

            return userId;

        } catch (ExpiredJwtException e) {
            System.out.println("❌ JWT 만료: " + e.getMessage());
            throw new RuntimeException("Token expired", e);
        } catch (Exception e) {
            System.out.println("❌ JWT 파싱 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Token parsing failed", e);
        }
    }

    /** 토큰 만료 확인 - 디버깅 버전 */
    public boolean isTokenExpired(String token) {
        try {
            System.out.println("🔍 JWTUtil.isTokenExpired 시작");

            // ✅ 새 JJWT 버전에 맞는 만료일 추출
            Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

            Date now = new Date();
            boolean expired = expiration.before(now);

            System.out.println("토큰 만료일: " + expiration);
            System.out.println("현재 시간: " + now);
            System.out.println("만료 여부: " + expired);
            System.out.println("남은 시간(초): " + ((expiration.getTime() - now.getTime()) / 1000));

            return expired;

        } catch (ExpiredJwtException e) {
            System.out.println("❌ 토큰이 이미 만료됨: " + e.getMessage());
            return true;
        } catch (Exception e) {
            System.out.println("❌ 토큰 만료 확인 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return true;
        }
    }

    /** 토큰 전체 검증 - 디버깅 버전 */
    public boolean validateToken(String token) {
        try {
            System.out.println("🔍 JWTUtil.validateToken 시작");

            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(600)  // 10분 시간 여유
                .build()
                .parseSignedClaims(token)
                .getPayload();

            System.out.println("✅ 토큰 검증 성공");
            System.out.println("발급자: " + claims.getIssuer());
            System.out.println("대상: " + claims.getAudience());
            System.out.println("발급일: " + claims.getIssuedAt());
            System.out.println("만료일: " + claims.getExpiration());

            return true;

        } catch (ExpiredJwtException e) {
            System.out.println("❌ 토큰 만료: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("❌ 토큰 검증 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }
}
