package com.osunji.melog; // 실제 패키지로 맞추세요

import com.osunji.melog.elk.repository.ELKUserRepository;
import com.osunji.melog.global.common.RefreshCookieHelper;
import com.osunji.melog.global.config.SecurityConfig;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.user.controller.AuthController;
import com.osunji.melog.user.dto.RefreshResult;
import com.osunji.melog.user.service.AuthService; // 서비스 실제 경로
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,   // 네가 만든 보안 @Configuration
                        JwtAuthFilter.class     // 컴포넌트로 등록된 필터
                })
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {
        // 보안/리소스 서버 자동설정 제거 (빌드파일에 보안 관련 스타터가 있으므로 안전하게 제외)
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
})
// @WebMvcTest 가 슬라이스 컨텍스트를 띄우므로 @SpringBootTest 절대 쓰지 않습니다.
public class AuthControllerRefreshTest {

    @Autowired MockMvc mockMvc;
    @MockBean
    RefreshCookieHelper refreshCookieHelper; // ★ 추가


    @MockBean AuthService authService; // 컨트롤러가 주입받는 것만 목킹
    @MockBean JwtAuthFilter jwtAuthFilter;

    @MockBean
    ELKUserRepository elkUserRepository;     // ★ 이번 에러의 원인


    @Test
    @DisplayName("회전 발생: access + refresh + ttl 과 no-store 헤더")
    void rotatesTokens() throws Exception {
        String auth = "Bearer rrr-token";
        when(authService.extractRefreshFromHeaders(auth, null))
                .thenReturn("refresh-token-1");
        when(authService.rotateTokens(eq("refresh-token-1"), any(HttpServletRequest.class)))
                .thenReturn(new RefreshResult("access-abc", "refresh-def", 3600L));

        mockMvc.perform(post("/api/auth/refresh").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(jsonPath("$.accessToken").value("access-abc"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-def"))
                .andExpect(jsonPath("$.refreshTtlSeconds").value(3600));

        verify(authService).extractRefreshFromHeaders(auth, null);
        verify(authService).rotateTokens(eq("refresh-token-1"), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("회전 없음: access 만, refresh 계열은 미포함(JsonInclude.NON_NULL일 경우)")
    void accessOnly_noRotation() throws Exception {
        String xRt = "rt-hdr";
        when(authService.extractRefreshFromHeaders(null, xRt))
                .thenReturn("refresh-token-2");
        when(authService.rotateTokens(eq("refresh-token-2"), any(HttpServletRequest.class)))
                .thenReturn(new RefreshResult("access-xyz", null, 0L));
        mockMvc.perform(post("/api/auth/refresh").header("X-Refresh-Token", xRt))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(jsonPath("$.accessToken").value("access-xyz"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.refreshTtlSeconds").doesNotExist());

        verify(authService).extractRefreshFromHeaders(null, xRt);
        verify(authService).rotateTokens(eq("refresh-token-2"), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("헤더 전달 정확성 검증(ArgumentCaptor)")
    void passesHeadersToService() throws Exception {
        String auth = "Bearer abc";
        String xRt  = "rt-123";
        when(authService.extractRefreshFromHeaders(any(), any())).thenReturn("rt-123");
        when(authService.rotateTokens(eq("rt-123"), any(HttpServletRequest.class)))
                .thenReturn(new RefreshResult("acc", null, null));

        mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .header("X-Refresh-Token", xRt))
                .andExpect(status().isOk());

        ArgumentCaptor<String> capAuth = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> capX    = ArgumentCaptor.forClass(String.class);
        verify(authService).extractRefreshFromHeaders(capAuth.capture(), capX.capture());
        assert capAuth.getValue().equals(auth);
        assert capX.getValue().equals(xRt);
    }
}
