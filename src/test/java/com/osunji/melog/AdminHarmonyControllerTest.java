package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.backoffice.controller.AdminHarmonyController;
import com.osunji.melog.backoffice.service.AdminHarmonyService;
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
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminHarmonyController.class,
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
class AdminHarmonyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AdminHarmonyService adminHarmonyService;

    @Test
    @DisplayName("하모니룸 검색 - 기본 조회 -> 200")
    void searchHarmonyRooms_default() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("items", List.of());
        stub.put("page", Map.of("page", 1, "size", 10, "totalElements", 0, "totalPages", 0));
        stub.put("query", Map.of());

        when(adminHarmonyService.searchHarmonyRooms(isNull(), isNull(), eq(1))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/harmonyRoom")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("하모니룸 검색 - nickname 타입으로 검색 -> 200")
    void searchHarmonyRooms_byNickname() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("items", List.of(Map.of(
                "harmonyRoomId", "room-uuid",
                "nickname", "베돌이",
                "harmonyRoomRole", "OWNER",
                "harmonyRoomName", "노래들어요"
        )));
        stub.put("page", Map.of("page", 1, "size", 10, "totalElements", 1, "totalPages", 1));
        stub.put("query", Map.of("criterion", "nickname", "keyword", "베돌이"));

        when(adminHarmonyService.searchHarmonyRooms(eq("nickname"), eq("베돌이"), eq(1))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/harmonyRoom")
                        .param("type", "nickname")
                        .param("query", "베돌이")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].nickname").value("베돌이"));
    }

    @Test
    @DisplayName("하모니룸 삭제 요청 성공 -> 200")
    void deleteHarmonyRoom_success() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("harmonyRoomId", "room-uuid");
        stub.put("harmonyRoomName", "베토벤 모임");
        stub.put("requestId", "delreq_abc123");
        stub.put("requestedAt", "2026-01-21T14:32:18");
        stub.put("deleteRequest", Map.of("title", "안녕하세요", "content", "삭제 예정", "managerName", "김운영"));

        when(adminHarmonyService.deleteHarmonyRoom(any())).thenReturn(stub);

        String body = """
                {
                  "harmonyRoomId": "d359fe47-303a-44b2-b636-df1a52c6e610",
                  "defaultTitle": "안녕하세요",
                  "defaultContent": "삭제 예정",
                  "managerName": "김운영",
                  "status": "TEMP"
                }
                """;

        mockMvc.perform(delete("/api/admin/harmonyRoom/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.harmonyRoomName").value("베토벤 모임"));
    }

    @Test
    @DisplayName("하모니룸 삭제 - 하모니룸 없음 -> 404")
    void deleteHarmonyRoom_notFound() throws Exception {
        when(adminHarmonyService.deleteHarmonyRoom(any()))
                .thenThrow(new NoSuchElementException("하모니룸을 찾을 수 없습니다."));

        String body = """
                {
                  "harmonyRoomId": "d359fe47-303a-44b2-b636-df1a52c6e610",
                  "defaultTitle": "안녕하세요",
                  "defaultContent": "삭제 예정",
                  "managerName": "김운영",
                  "status": "TEMP"
                }
                """;

        mockMvc.perform(delete("/api/admin/harmonyRoom/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("하모니룸 삭제 - 필수값 누락 -> 422")
    void deleteHarmonyRoom_validationError() throws Exception {
        when(adminHarmonyService.deleteHarmonyRoom(any()))
                .thenThrow(new AdminHarmonyService.AdminValidationException(
                        List.of(Map.of("field", "managerName", "reason", "담당자는 필수 입력값입니다."))
                ));

        String body = """
                {
                  "harmonyRoomId": "d359fe47-303a-44b2-b636-df1a52c6e610",
                  "defaultTitle": "안녕하세요",
                  "defaultContent": "",
                  "managerName": "",
                  "status": "TEMP"
                }
                """;

        mockMvc.perform(delete("/api/admin/harmonyRoom/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field").value("managerName"));
    }
}
