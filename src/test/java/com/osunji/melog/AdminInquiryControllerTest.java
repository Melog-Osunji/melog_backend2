package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.backoffice.controller.AdminInquiryController;
import com.osunji.melog.backoffice.service.AdminInquiryService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminInquiryController.class,
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
class AdminInquiryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AdminInquiryService adminInquiryService;

    @Test
    @DisplayName("문의 목록 조회 - 기본 -> 200")
    void getInquiries_default() throws Exception {
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("content", List.of());
        stub.put("page", 0);
        stub.put("size", 10);
        stub.put("totalElements", 0L);
        stub.put("totalPages", 0);

        when(adminInquiryService.getInquiries(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(stub);

        mockMvc.perform(get("/api/admin/inquiries")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("문의 목록 조회 - fromDate만 있음 -> 400")
    void getInquiries_missingToDate() throws Exception {
        mockMvc.perform(get("/api/admin/inquiries")
                        .param("fromDate", "2026-01-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromDate와 toDate는 함께 사용해야 합니다."));
    }

    @Test
    @DisplayName("문의 목록 조회 - fromDate/toDate + year 동시 사용 -> 400")
    void getInquiries_conflictDateParams() throws Exception {
        mockMvc.perform(get("/api/admin/inquiries")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-01-31")
                        .param("year", "2026")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromDate/toDate와 year/month는 동시에 사용할 수 없습니다."));
    }

    @Test
    @DisplayName("문의 상세 조회 -> 200")
    void getInquiryDetail() throws Exception {
        UUID inquiryId = UUID.randomUUID();
        Map<String, Object> stub = new LinkedHashMap<>();
        stub.put("inquiryId", inquiryId.toString());
        stub.put("name", "홍길동");
        stub.put("title", "문의 제목");
        stub.put("answer", null);
        stub.put("answerStatus", "PENDING");

        when(adminInquiryService.getInquiryDetail(eq(inquiryId))).thenReturn(stub);

        mockMvc.perform(get("/api/admin/inquiries/" + inquiryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.answerStatus").value("PENDING"));
    }

    @Test
    @DisplayName("답변 임시저장 -> 200")
    void saveDraft() throws Exception {
        UUID inquiryId = UUID.randomUUID();
        doNothing().when(adminInquiryService).saveDraft(eq(inquiryId), anyString());

        String body = "{\"answer\":\"임시 답변 내용\"}";

        mockMvc.perform(put("/api/admin/inquiries/" + inquiryId + "/answer/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("임시저장 성공"));

        verify(adminInquiryService).saveDraft(eq(inquiryId), eq("임시 답변 내용"));
    }

    @Test
    @DisplayName("답변 전송 -> 200")
    void publishAnswer() throws Exception {
        UUID inquiryId = UUID.randomUUID();
        doNothing().when(adminInquiryService).publishAnswer(eq(inquiryId), anyString());

        String body = "{\"answer\":\"최종 답변 내용\"}";

        mockMvc.perform(put("/api/manage/inquiries/" + inquiryId + "/answer/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("답변 전송 성공"));

        verify(adminInquiryService).publishAnswer(eq(inquiryId), eq("최종 답변 내용"));
    }

    @Test
    @DisplayName("답변 임시저장 - answer 누락 -> 400")
    void saveDraft_missingAnswer() throws Exception {
        UUID inquiryId = UUID.randomUUID();
        String body = "{}";

        mockMvc.perform(put("/api/admin/inquiries/" + inquiryId + "/answer/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
