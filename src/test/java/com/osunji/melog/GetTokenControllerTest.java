package com.osunji.melog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 비활성화(컨트롤러 접근 보장)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // 테스트용 TTL (15분/1일과 유사)
        "dev.token.access-ttl-ms=900000",
        "dev.token.refresh-ttl-ms=86400000",
        // JWTUtil이 프로퍼티를 읽는다면 여기에 테스트용 시크릿/설정 추가
        // "jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        // "jwt.issuer=melog-api"
})
class GetTokenControllerBlackBoxTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    // === 헬퍼 ===
    private static Map<String, Object> decodePayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = new ObjectMapper().readValue(payload, Map.class);
        return map;
    }

    private static long toEpochMillis(Object expClaim) {
        // exp가 초/밀리초 어떤 포맷으로 와도 처리
        if (expClaim instanceof Number n) {
            long v = n.longValue();
            if (v < 10_000_000_000L) return v * 1000L; // seconds → ms
            return v; // already ms
        }
        return 0L;
    }

    @Test
    @DisplayName("발급 직후 토큰 payload 검증: iss/aud/userId/iat/exp 및 exp > now")
    void tokenPayload_sanity() throws Exception {
        String body = mockMvc.perform(get("/api/dev/token").param("userId", "alice"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // 캐시 금지 헤더가 없다면 여기서 실패시켜 캐시 이슈를 드러내자
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = om.readTree(body);
        String access  = json.get("accessToken").asText();
        String refresh = json.get("refreshToken").asText();

        // access payload 확인
        Map<String, Object> ap = decodePayload(access);
        assertThat(ap.get("iss")).isEqualTo("melog-api");
        // aud는 단일/배열일 수 있음 → 양쪽 다 허용
        Object aud = ap.get("aud");
        if (aud instanceof List<?> list) assertThat(list).contains("melog-client");
        else if (aud instanceof String s) assertThat(s).isEqualTo("melog-client");

        assertThat(ap.get("userId")).isEqualTo("alice");
        assertThat(ap.get("jti")).isNotNull();
        assertThat(ap.get("iat")).isNotNull();
        assertThat(ap.get("exp")).isNotNull();

        long now = Instant.now().toEpochMilli();
        long expMs = toEpochMillis(ap.get("exp"));
        assertThat(expMs).isGreaterThan(now + 1000); // 발급 직후인데 exp가 과거면 즉시 만료 이슈

        // refresh도 exp 존재/미래 확인(대략 24h ± 오차)
        Map<String, Object> rp = decodePayload(refresh);
        assertThat(rp.get("jti")).isNotNull();
        long rExpMs = toEpochMillis(rp.get("exp"));
        assertThat(rExpMs - now).isBetween(86_200_000L, 86_600_000L); // 24h ± ~200초
    }

    @Test
    @DisplayName("같은 초에 2회 호출해도 accessToken 문자열은 달라야 한다(jti 유효)")
    void consecutive_calls_yield_different_tokens() throws Exception {
        String body1 = mockMvc.perform(get("/api/dev/token").param("userId", "bob"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String access1 = om.readTree(body1).get("accessToken").asText();

        String body2 = mockMvc.perform(get("/api/dev/token").param("userId", "bob"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String access2 = om.readTree(body2).get("accessToken").asText();

        assertThat(access1).isNotEqualTo(access2);

        // jti도 실제로 다름을 확인
        String jti1 = String.valueOf(decodePayload(access1).get("jti"));
        String jti2 = String.valueOf(decodePayload(access2).get("jti"));
        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    @DisplayName("빈 userId는 컨트롤러에서 missing_userId로 400")
    void blank_userId() throws Exception {
        mockMvc.perform(get("/api/dev/token").param("userId", ""))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("missing_userId"));
    }
}
