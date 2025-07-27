package com.osunji.melog;


import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.security.JWTUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthHelperIntegrationTest {

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private AuthHelper authHelper;

    @Test
    void testAuthHelperWithValidToken() {
        // given
        String userId = "test-user";
        String token = jwtUtil.createJWT(userId, 10000L);  // 10초짜리 토큰
        String header = "Bearer " + token;

        // when
        String result = authHelper.authHelper(header);

        // then
        assertEquals(userId, result);
    }

    @Test
    void testAuthHelperWithExpiredToken() throws InterruptedException {
        // given
        String token = jwtUtil.createJWT("test-user", 1L);  // 1ms 만료
        Thread.sleep(10); // 토큰이 확실히 만료되도록

        String header = "Bearer " + token;

        // when + then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                authHelper.authHelper(header)
        );
        assertEquals("Token expired", ex.getMessage());
    }
}
