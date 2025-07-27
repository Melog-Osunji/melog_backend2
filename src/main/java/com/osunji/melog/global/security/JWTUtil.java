package com.osunji.melog.global.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// 0.12.3 토큰
@Component
public class JWTUtil {
    private final SecretKey secretKey;
    private final SecretKey refreshSecretKey;


    // 토큰 생성 시 key값 주입
    public JWTUtil(@Value("${jwt.secret}")String secret
    , @Value("${jwt.refresh}")String refresh) {
        this.secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
        this.refreshSecretKey = new SecretKeySpec(
                refresh.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    // 토큰에서 UserId 추출
    public String getUserIdFromToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("userId", String.class);
    }

    // 토큰 만료 여부 확인
    public Boolean isTokenExpired(String token) {
        try{ // 토큰 검증
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload().getExpiration()
                    .before(new Date());

        } catch (ExpiredJwtException e) { // 파싱 단계에서부터 오류 발생시 바로 true 반환
            return true;
        }
    }

    // jwt 토큰 생성
    public String createJWT(String userId, Long expiredMillis) {
        return Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+ expiredMillis))
                .signWith(secretKey)
                .compact();
    }

    // refresh 토큰 생성
    public String createRefreshJWT(String userId, Long refreshMillis) {
        return Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+ refreshMillis))
                .signWith(refreshSecretKey)
                .compact();
    }

}
