package com.osunji.melog;


import com.osunji.melog.elk.repository.ELKUserRepository;
import com.osunji.melog.global.common.RefreshCookieHelper;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.global.config.SecurityConfig;

import com.osunji.melog.user.controller.AuthController;
import com.osunji.melog.user.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static com.osunji.melog.global.common.RefreshCookieHelper.REFRESH_COOKIE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        // 보안/리소스 서버 자동설정 제거
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
})
class AuthControllerLogoutTest {

    @Autowired MockMvc mockMvc;

    // 컨트롤러가 주입받는 것만 목킹
    @MockBean
    AuthService authService;
    @MockBean
    RefreshCookieHelper refreshCookieHelper;

    // 다른 테스트에서 필요했던 목들도 동일하게 등록(컨텍스트 에러 방지)
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean
    ELKUserRepository elkUserRepository;

    @Test
    @DisplayName("로그아웃: refresh 쿠키가 있을 때 204 반환, 서비스와 쿠키 헬퍼 호출")
    void logout_withRefreshCookie_returns204_andInvokesDeps() throws Exception {
        // given
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "rt-123");

        // when & then
        mockMvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent());

        // 서비스가 정확히 전달받았는지
        verify(authService).logout("rt-123");
        // 쿠키 제거 시도가 있었는지(실제 Set-Cookie는 목킹이라 검증 어려우므로 상호작용 검사)
        verify(refreshCookieHelper).clearRefreshCookie(any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("로그아웃: refresh 쿠키가 없을 때도 204 반환, null로 서비스 호출 및 쿠키 헬퍼 호출")
    void logout_withoutRefreshCookie_returns204_andStillInvokesDeps() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        // null 전달 케이스 허용(서비스에서 null이면 그냥 return 하도록 구현했을 것)
        verify(authService).logout(isNull());
        verify(refreshCookieHelper).clearRefreshCookie(any(HttpServletResponse.class));
    }
}

