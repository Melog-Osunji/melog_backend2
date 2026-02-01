package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.backoffice.controller.AdminAuthController;
import com.osunji.melog.backoffice.dto.AdminLoginRequest;
import com.osunji.melog.backoffice.service.AdminAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminAuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.osunji.melog.global.config.SecurityConfig.class,
                        com.osunji.melog.global.security.JwtAuthFilter.class
                }
        )
)
class AdminAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AdminAuthService adminAuthService;

    @Test
    @DisplayName("로그인 성공 -> 200 + accessToken 반환")
    void login_success() throws Exception {
        Map<String, Object> stub = Map.of(
                "accessToken", "jwt-token-123",
                "adminId", "admin-uuid",
                "loginId", "admin01"
        );
        when(adminAuthService.login(any(AdminLoginRequest.class))).thenReturn(stub);

        String body = "{\"loginId\":\"admin01\",\"password\":\"pass123\"}";

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token-123"))
                .andExpect(jsonPath("$.data.loginId").value("admin01"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호 -> 서비스에서 예외 전파")
    void login_fail_wrong_password() throws Exception {
        when(adminAuthService.login(any(AdminLoginRequest.class)))
                .thenThrow(new IllegalArgumentException("비밀번호가 일치하지 않습니다."));

        String body = "{\"loginId\":\"admin01\",\"password\":\"wrong\"}";

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)));
    }

    @Test
    @DisplayName("로그인 실패 - loginId 누락 -> 400")
    void login_fail_missing_loginId() throws Exception {
        String body = "{\"password\":\"pass123\"}";

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
