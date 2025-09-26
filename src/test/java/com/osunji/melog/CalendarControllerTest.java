package com.osunji.melog;

import com.osunji.melog.calendar.controller.CalendarController;
import com.osunji.melog.calendar.dto.CalendarResponse;
import com.osunji.melog.calendar.service.CalendarService;
import com.osunji.melog.calendar.service.CultureOpenApiService;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CalendarController 슬라이스 테스트
 */
@WebMvcTest(
        controllers = CalendarController.class,
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
class CalendarControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CalendarService calendarService;

    @MockBean
    CultureOpenApiService cultureOpenApiService;

    // ---- /api/calendar/main ----

    @Test
    @DisplayName("[/main] year, month 모두 없음 -> 200 + JSON")
    void calendarMain_ok_when_no_year_month() throws Exception {
        var stub = ApiMessage.success(200, "OK", new CalendarResponse());
        Mockito.when(calendarService.calendarMain(any(UUID.class), isNull(), isNull()))
                .thenReturn(stub);

        mockMvc.perform(get("/api/calendar/main")
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    @DisplayName("[/main] year만 있으면 -> 400 (text/plain; charset=UTF-8)")
    void calendarMain_badRequest_when_only_year() throws Exception {
        mockMvc.perform(get("/api/calendar/main")
                        .param("year", "2025")
                        .accept(MediaType.TEXT_PLAIN)
                        .characterEncoding("UTF-8")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("year과 month는 동시에 존재하거나 동시에 없어야 합니다."));
    }

    @Test
    @DisplayName("[/main] year, month 모두 있으면 -> 200 + JSON")
    void calendarMain_ok_when_both_year_month() throws Exception {
        var stub = ApiMessage.success(200, "OK", new CalendarResponse());
        Mockito.when(calendarService.calendarMain(any(UUID.class), eq(2025), eq(9)))
                .thenReturn(stub);

        mockMvc.perform(get("/api/calendar/main")
                        .param("year", "2025")
                        .param("month", "9")
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    // ---- /api/calendar/items ----

    @Test
    @DisplayName("[/items] category 잘못된 값 -> 400 (text/plain; charset=UTF-8)")
    void items_badRequest_when_invalid_category() throws Exception {
        mockMvc.perform(get("/api/calendar/items")
                        .param("category", "INVALID")
                        .accept(MediaType.TEXT_PLAIN)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("잘못된 category 값입니다."));
    }

    @Test
    @DisplayName("[/items] category 올바른 값(MUSIC) -> 200 + JSON 목록")
    void items_ok_when_valid_category() throws Exception {
        var item = CalendarResponse.Item.builder()
                .id(UUID.randomUUID())
                .title("테스트 공연")
                .category("MUSIC") // 응답에서 enum name 사용
                .build();

        Mockito.when(cultureOpenApiService.fetchItems(any()))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/calendar/items")
                        .param("category", "MUSIC") // 존재하는 enum
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("외부 공연 목록"))
                .andExpect(jsonPath("$.data[0].title").value("테스트 공연"))
                .andExpect(jsonPath("$.data[0].category").value("MUSIC"));
    }

    @Test
    @DisplayName("[/items] 응답 아이템에 category 누락 있으면 -> 500 (text/plain; charset=UTF-8)")
    void items_ise_when_missing_category_in_items() throws Exception {
        var bad = CalendarResponse.Item.builder()
                .id(UUID.randomUUID())
                .title("누락 데이터")
                .category("") // 빈 문자열 → invalid
                .build();

        Mockito.when(cultureOpenApiService.fetchItems(any()))
                .thenReturn(List.of(bad));

        mockMvc.perform(get("/api/calendar/items")
                        .param("category", "MUSIC")
                        .accept(MediaType.TEXT_PLAIN)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("응답 데이터에 category 값이 누락되었습니다."));
    }
}
