package com.osunji.melog;

import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.security.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthHelperTest {

    private JWTUtil jwtUtil;
    private AuthHelper authHelper;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JWTUtil.class);
        authHelper = new AuthHelper(jwtUtil);
    }

    @Test
    @DisplayName("Authorization 헤더가 null 또는 빈 문자열일 경우 예외 발생")
    void testEmptyOrNullAuthHeader() {
        assertThrows(IllegalStateException.class, () -> authHelper.authHelper(null), "Empty Authorization header");
        assertThrows(IllegalStateException.class, () -> authHelper.authHelper(""), "Empty Authorization header");
    }

    @Test
    @DisplayName("Authorization 헤더가 'Bearer '로 시작하지 않으면 예외 발생")
    void testInvalidAuthHeaderPrefix() {
        String invalidHeader = "Token abc.def.ghi";
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authHelper.authHelper(invalidHeader));
        assertEquals("Invalid Authorization header", exception.getMessage());
    }

    @Test
    @DisplayName("만료된 토큰일 경우 예외 발생")
    void testExpiredToken() {
        String token = "abc.def.ghi";
        when(jwtUtil.isTokenExpired(token)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                authHelper.authHelper("Bearer " + token));

        assertEquals("Token expired", exception.getMessage());
        verify(jwtUtil).isTokenExpired(token);
        verify(jwtUtil, never()).getUserIdFromToken(anyString());
    }

    @Test
    @DisplayName("유효한 토큰일 경우 userId 반환")
    void testValidToken() {
        String token = "abc.def.ghi";
        String expectedUserId = "user123";

        when(jwtUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(expectedUserId);

        String result = authHelper.authHelper("Bearer " + token);

        assertEquals(expectedUserId, result);
        verify(jwtUtil).isTokenExpired(token);
        verify(jwtUtil).getUserIdFromToken(token);
    }
}
