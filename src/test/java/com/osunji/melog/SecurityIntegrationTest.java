// src/test/java/com/osunji/melog/SecuritySliceTest.java
package com.osunji.melog;

import com.osunji.melog.global.config.SecurityConfig;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.user.controller.SecureController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecureController.class)      // 웹 레이어만 로드 (JPA/DB X)
@AutoConfigureMockMvc(addFilters = true)               // 시큐리티 필터 포함
@Import({ SecurityConfig.class, JwtAuthFilter.class, JWTUtil.class }) // 보안 설정/필터/유틸 주입
@TestPropertySource(properties = {
        "jwt.secret=2z3r8s8bJt0f1YQjJ4s0f1Uu8VYcGvC8nQkz2Y3o0sM=",
        "jwt.refresh=Yf8mQ7q2kZ2I3p7sZ9x0g2F5v8w1T4c7b0r6m1p9q2k="
})
class SecuritySliceTest {

    @Autowired MockMvc mvc;
    @Autowired JWTUtil jwtUtil;

    @Test
    void whenNoToken_then401() throws Exception {
        mvc.perform(get("/secure/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenMalformedToken_then401() throws Exception {
        mvc.perform(get("/secure/ping")
                        .header(AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenExpiredToken_then401() throws Exception {
        String expired = jwtUtil.createJWT("U1", -1000L);
        mvc.perform(get("/secure/ping")
                        .header(AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenValidAccessToken_then200() throws Exception {
        String access = jwtUtil.createJWT("U1", 5 * 60 * 1000L);
        mvc.perform(get("/secure/ping")
                        .header(AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk());
    }
}
