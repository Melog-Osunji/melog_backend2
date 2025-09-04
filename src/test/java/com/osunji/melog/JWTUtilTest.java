package com.osunji.melog;

import com.osunji.melog.global.util.JWTUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=unit-test-secret-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.refresh=unit-test-refresh-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
})
class JWTUtilTest {

    @Autowired
    JWTUtil jwtUtil;

    @Test
    void access_token_create_and_validate() {
        String token = jwtUtil.createAccessToken("user:123", 60_000);
        jwtUtil.validateAccess(token);
        assertEquals("user:123", jwtUtil.getUserIdFromAccess(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void refresh_token_create_and_validate_and_jti() {
        String rt = jwtUtil.createRefreshToken("user:abc", 3600_000);
        jwtUtil.validateRefresh(rt);
        assertEquals("user:abc", jwtUtil.getUserIdFromRefresh(rt));
        assertNotNull(jwtUtil.getJtiFromRefresh(rt));
        assertTrue(jwtUtil.getRefreshExpiryEpochMillis(rt) > System.currentTimeMillis());
    }
}
