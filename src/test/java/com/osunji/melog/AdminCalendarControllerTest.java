package com.osunji.melog;

import com.osunji.melog.backoffice.controller.AdminCalendarController;
import com.osunji.melog.backoffice.service.AdminCalendarService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminCalendarController.class,
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
class AdminCalendarControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminCalendarService adminCalendarService;

    @Test
    @DisplayName("캘린더 조회 - 기본 조회 -> 200")
    void getCalendarEvents_default() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("events", List.of());
        stub.put("page", Map.of("page", 0, "size", 10, "totalElements", 0, "totalPages", 0));
        stub.put("query", Map.of("category", "ALL"));

        when(adminCalendarService.getCalendarEvents(isNull(), isNull(), isNull(), eq(0))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/calendar")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("캘린더 조회 - 카테고리 + 날짜 필터 -> 200")
    void getCalendarEvents_withFilters() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("events", List.of(Map.of(
                "eventId", "event-uuid",
                "title", "테스트 공연",
                "category", "THEATER",
                "savedCount", 10
        )));
        stub.put("page", Map.of("page", 0, "size", 10, "totalElements", 1, "totalPages", 1));
        stub.put("query", Map.of("category", "THEATER", "fromDate", "2026-01-01", "toDate", "2026-01-31"));

        when(adminCalendarService.getCalendarEvents(
                eq("THEATER"), eq("2026-01-01"), eq("2026-01-31"), eq(0))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/calendar")
                        .param("type", "THEATER")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events[0].title").value("테스트 공연"));
    }

    @Test
    @DisplayName("캘린더 조회 - fromDate만 있고 toDate 없음 -> 400")
    void getCalendarEvents_missingToDate() throws Exception {
        mockMvc.perform(get("/api/admin/calendar")
                        .param("fromDate", "2026-01-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromDate와 toDate는 함께 사용해야 합니다."));
    }

    @Test
    @DisplayName("캘린더 조회 - toDate만 있고 fromDate 없음 -> 400")
    void getCalendarEvents_missingFromDate() throws Exception {
        mockMvc.perform(get("/api/admin/calendar")
                        .param("toDate", "2026-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromDate와 toDate는 함께 사용해야 합니다."));
    }
}
