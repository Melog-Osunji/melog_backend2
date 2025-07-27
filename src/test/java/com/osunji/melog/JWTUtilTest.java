package com.osunji.melog;


import com.osunji.melog.global.security.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JWTUtilTest {

    private JWTUtil jwtUtil;
    private final String secret = "test-secret-key-for-jwt-should-be-long-enough";

//    @BeforeEach
//    void setUp() {
//        jwtUtil = new JWTUtil(secret);
//    }

    @Test
    void createJWT_shouldReturnValidToken() {
        // given
        String userId = "user123";
        Long expiredMillis = 1000L * 60 * 10; // 10 minutes

        // when
        String token = jwtUtil.createJWT(userId, expiredMillis);

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        // given
        String userId = "user123";
        String token = jwtUtil.createJWT(userId, 1000L * 60 * 10);

        // when
        String extractedUserId = jwtUtil.getUserIdFromToken(token);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void isTokenExpired_shouldReturnFalseForValidToken() {
        // given
        String token = jwtUtil.createJWT("user123", 1000L * 60 * 10);

        // when
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // then
        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpired_shouldReturnTrueForExpiredToken() throws InterruptedException {
        // given
        String token = jwtUtil.createJWT("user123", 100L); // 100ms짜리로 늘리기
        Thread.sleep(150); // 충분히 지난 다음 체크

        // when
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // then
        assertThat(isExpired).isTrue();
    }
}