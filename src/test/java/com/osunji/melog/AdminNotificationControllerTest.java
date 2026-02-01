package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.backoffice.controller.AdminNotificationController;
import com.osunji.melog.backoffice.service.AdminNotificationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminNotificationController.class,
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
class AdminNotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AdminNotificationService adminNotificationService;

    @Test
    @DisplayName("알림 목록 조회 -> 200")
    void getNotifications() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("content", List.of());
        stub.put("page", Map.of("page", 0, "size", 10, "totalElements", 0, "totalPages", 0));

        when(adminNotificationService.getNotifications(eq(0), eq(10))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/notifications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("알림 작성 성공 -> 200")
    void createNotification_success() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("id", "noti-uuid");
        stub.put("title", "공지사항");
        stub.put("content", "내용입니다");
        stub.put("targetType", "ALL");

        when(adminNotificationService.createNotification(any())).thenReturn(stub);

        String body = """
                {
                  "title": "공지사항",
                  "content": "내용입니다",
                  "targetType": "ALL"
                }
                """;

        mockMvc.perform(post("/api/admin/notifications/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림 작성 성공"))
                .andExpect(jsonPath("$.data.title").value("공지사항"));
    }

    @Test
    @DisplayName("알림 작성 - title 누락 -> 400")
    void createNotification_missingTitle() throws Exception {
        String body = """
                {
                  "content": "내용입니다",
                  "targetType": "ALL"
                }
                """;

        mockMvc.perform(post("/api/admin/notifications/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
