package com.osunji.melog;

import com.osunji.melog.backoffice.controller.AdminStatisticsController;
import com.osunji.melog.backoffice.service.AdminStatisticsService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminStatisticsController.class,
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
class AdminStatisticsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminStatisticsService adminStatisticsService;

    @Test
    @DisplayName("소셜로그인 통계 -> 200")
    void getSocialLoginStatistics() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("total", 100L);
        stub.put("platforms", List.of(
                Map.of("platform", "KAKAO", "count", 50L, "percentage", 50.0),
                Map.of("platform", "GOOGLE", "count", 30L, "percentage", 30.0),
                Map.of("platform", "NAVER", "count", 20L, "percentage", 20.0)
        ));

        when(adminStatisticsService.getSocialLoginStatistics()).thenReturn(stub);

        mockMvc.perform(get("/api/admin/statistics/users/social-login")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(100))
                .andExpect(jsonPath("$.data.platforms[0].platform").value("KAKAO"))
                .andExpect(jsonPath("$.data.platforms[0].count").value(50));
    }

    @Test
    @DisplayName("활동시간 통계 -> 200")
    void getActiveTimeStatistics() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("timeSlots", List.of(Map.of("hour", 0, "count", 0)));
        stub.put("totalSessions", 0);

        when(adminStatisticsService.getActiveTimeStatistics()).thenReturn(stub);

        mockMvc.perform(get("/api/admin/statistics/users/active-time")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.timeSlots").isArray());
    }
}
