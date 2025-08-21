package com.osunji.melog.global.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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

    /** (이전 getUserIdFromToken을 보다 명확히) */
    public String getUserIdFromAccess(String accessToken) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload()
                .get("userId", String.class);
    }

    /** 액세스 토큰 검증(서명/만료) — 유효하면 조용히 통과, 아니면 예외 */
    public void validateAccess(String accessToken) {
        // parse 과정에서 서명/만료/형식 오류가 예외로 터짐
        Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(accessToken);

        // 추가 clock skew 허용/iss, aud 검증이 필요하면 여기서 더 체크
    }

    /** (이전 isTokenExpired 대체/호환) */
    public Boolean isAccessExpired(String accessToken) {
        try {
            Date exp = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload()
                    .getExpiration();
            return exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public String createJWT(String userId, Long expiredMillis) {
        return Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMillis))
                .signWith(secretKey)
                .compact();
    }

    /* ===================== Refresh Token ===================== */

    public String getUserIdFromRefresh(String refreshToken) {
        return Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload()
                .get("userId", String.class);
    }

    public void validateRefresh(String refreshToken) {
        Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(refreshToken);
    }

    public String createRefreshJWT(String userId, Long refreshMillis) {
        return Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshMillis))
                .signWith(refreshSecretKey)
                .compact();
    }

    /* ===================== (구 메서드 호환용) ===================== */

    /** 기존 이름 유지가 필요하면 wrapper로 연결 */
    public String getUserIdFromToken(String token) {
        return getUserIdFromAccess(token);
    }

    public Boolean isTokenExpired(String token) {
        return isAccessExpired(token);
    }
}
