package com.osunji.melog;


import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.user.dto.RefreshResult;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import com.osunji.melog.user.service.AuthService;
import com.osunji.melog.user.service.OidcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
        // Elasticsearch 오토컨피그 제외 (테스트에 불필요)
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration,"
})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "jwt.refresh=test-refresh-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
})
class AuthServiceIT {

    // ---- Redis Testcontainer ----
    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    // ---- Beans under test ----
    @Autowired AuthService authService;
    @Autowired RefreshTokenRepository refreshRepo;
    @Autowired JWTUtil jwtUtil;
    @Autowired OidcService oidcService; // 테스트 설정에서 주입됨

    @TestConfiguration
    static class MockConfig {
        @Bean
        public OidcService oidcService() {
            return Mockito.mock(OidcService.class);
        }
    }

    private static MockHttpServletRequest validReq() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Origin", "http://localhost:3000");
        req.addHeader("X-Requested-With", "XMLHttpRequest");
        return req;
    }

    @Test
    void end_to_end_issue_rotate_logout_and_reuse_detection() {
        // 1) OIDC 콜백 스텁
        Mockito.when(oidcService.exchangeAndVerify(eq("kakao"), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("sub", "demo-sub"));

        // 2) 핸들러 호출 → 토큰 발급 + Redis 저장
        RefreshResult issued = authService.handleOidcCallback("kakao", "codeX", "stateY", "verifierZ");
        assertNotNull(issued.accessToken());
        assertNotNull(issued.refreshToken());

        String userId = "kakao:demo-sub";
        String oldJti = jwtUtil.getJtiFromRefresh(issued.refreshToken());
        assertTrue(refreshRepo.existsAndMatch(userId, oldJti, issued.refreshToken()));

        // 3) 회전 → 새 토큰, Redis 회전 확인
        RefreshResult rotated = authService.rotateTokens(issued.refreshToken(), validReq());
        String newJti = jwtUtil.getJtiFromRefresh(rotated.refreshToken());
        assertTrue(refreshRepo.existsAndMatch(userId, newJti, rotated.refreshToken()));
        assertFalse(refreshRepo.existsAndMatch(userId, oldJti, issued.refreshToken()));

        // 4) 재사용 공격 → 401
        Executable reuse = () -> authService.rotateTokens(issued.refreshToken(), validReq());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, reuse);
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("refresh_reuse_or_revoked", ex.getReason());

        // 5) 로그아웃 → refresh 삭제
        authService.logout(rotated.refreshToken());
        assertFalse(refreshRepo.existsAndMatch(userId, newJti, rotated.refreshToken()));
    }

    @Test
    void refresh_without_xrw_header_should_fail_with_400() {
        Mockito.when(oidcService.exchangeAndVerify(any(), any(), any(), any()))
                .thenReturn(Map.of("sub", "abc"));

        RefreshResult issued = authService.handleOidcCallback("kakao", "code", "state", "verifier");

        // Origin 은 허용된 값으로 넣고, XRW 헤더는 생략
        MockHttpServletRequest badReq = new MockHttpServletRequest();
        badReq.addHeader("Origin", "http://localhost:3000");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.rotateTokens(issued.refreshToken(), badReq));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("missing_xrw", ex.getReason());
    }
    // 클래스 상단 필드에 추가
    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    // 테스트 클래스 내부 아무 곳에 추가
    private static String sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void inspectRedisKeysAndValues() {
        // 1) OIDC 스텁
        Mockito.when(oidcService.exchangeAndVerify(any(), any(), any(), any()))
                .thenReturn(Map.of("sub", "inspect"));

        // 2) 토큰 발급 (→ Redis 저장)
        RefreshResult issued = authService.handleOidcCallback("kakao", "codeI", "stateI", "verifierI");
        String refresh = issued.refreshToken();
        String expectedHash = sha256(refresh);

        // 3) Redis 키/값 덤프
        var keys = redisTemplate.keys("refresh:*");
        System.out.println("=== Redis Keys & Values (hashed) ===");
        if (keys != null) {
            for (String k : keys) {
                String v = redisTemplate.opsForValue().get(k);
                System.out.printf("%s -> %s%n", k, v);
            }
        }

        // 4) 이 발급 건이 실제로 저장됐는지 확인(해시 비교)
        assertNotNull(keys, "Redis keys should not be null");
        boolean matched = false;
        for (String k : keys) {
            String v = redisTemplate.opsForValue().get(k);
            if (expectedHash.equals(v)) {
                matched = true;
                System.out.println("Matched refresh token hash in Redis.");
                System.out.println("  token (prefix): " + refresh.substring(0, Math.min(30, refresh.length())) + "..."); // 토큰 전문 노출 방지
                System.out.println("  sha256(token):  " + expectedHash);
                System.out.println("  redis value:    " + v);
                break;
            }
        }
        assertTrue(matched, "Issued refresh token hash must exist in Redis");

        // (선택) Redis 컨테이너 로그도 보고 싶다면:
        String logs = redis.getLogs();
        System.out.println("=== Redis Container Logs (tail) ===");
        System.out.println(logs.substring(Math.max(0, logs.length() - 2000))); // 너무 길면 뒤쪽만
    }

}
