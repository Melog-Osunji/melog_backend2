package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.backoffice.controller.AdminAccountController;
import com.osunji.melog.backoffice.service.AdminAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminAccountController.class,
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
class AdminAccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AdminAccountService adminAccountService;

    @Test
    @DisplayName("계정 목록 조회 - 파라미터 없이 기본 조회 -> 200")
    void getAccounts_default() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("items", List.of());
        stub.put("page", Map.of("page", 1, "size", 10, "totalElements", 0, "totalPages", 0));
        stub.put("query", Map.of());

        when(adminAccountService.getAccounts(isNull(), isNull(), eq(1))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/account")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("계정 목록 조회 - type=email, query 지정 -> 200")
    void getAccounts_withSearchParams() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("items", List.of());
        stub.put("page", Map.of("page", 1, "size", 10, "totalElements", 0, "totalPages", 0));
        stub.put("query", Map.of("criterion", "EMAIL", "keyword", "test@test.com"));

        when(adminAccountService.getAccounts(eq("email"), eq("test@test.com"), eq(1))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/account")
                        .param("type", "email")
                        .param("query", "test@test.com")
                        .param("page", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("관리자 계정 삭제 -> 200")
    void deleteAccounts_success() throws Exception {
        doNothing().when(adminAccountService).deleteAdminAccounts(any());

        String body = """
                {
                  "users": [
                    { "loginId": "user01", "loginPw": "pass01", "loginNickName": "홍길동" }
                  ]
                }
                """;

        mockMvc.perform(delete("/api/admin/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("삭제 성공"));
    }

    @Test
    @DisplayName("관리자 계정 삭제 - users 비어있음 -> 400")
    void deleteAccounts_emptyUsers() throws Exception {
        String body = """
                {
                  "users": []
                }
                """;

        mockMvc.perform(delete("/api/admin/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
