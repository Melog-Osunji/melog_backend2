package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.inquirySettings.controller.SettingsController;
import com.osunji.melog.inquirySettings.dto.request.SettingsRequest;
import com.osunji.melog.inquirySettings.dto.response.SettingsResponse;
import com.osunji.melog.inquirySettings.service.SettingsService;
import com.osunji.melog.user.domain.enums.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SettingsController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class // JwtAuthFilter만 스캔 제외
        )
)
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 전부 비활성화
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean SettingsService settingsService;

    private static ApiMessage<SettingsResponse.infoSettingsResponse> stubInfo(UUID uid) {
        var data = SettingsResponse.infoSettingsResponse.builder()
                .userId(uid)
                .platform(Platform.KAKAO)
                .email("user@example.com")
                .isActive(true)          // Lombok boolean getter -> JSON 키는 active
                .language("kor")
                .build();
        return ApiMessage.success(200, "success", data);
    }

    private static ApiMessage<List<SettingsResponse.FollowResponse>> stubList() {
        var item = SettingsResponse.FollowResponse.builder()
                .userId(UUID.randomUUID())
                .profileImg("https://example.com/profile.jpg")
                .nickname("exampleUser")
                .description("This is an example description.")
                .build();
        return ApiMessage.success(200, "success", List.of(item));
    }

    private static ApiMessage<SettingsResponse.CheckResponse> stubCheck(UUID uid) {
        var data = SettingsResponse.CheckResponse.builder()
                .userId(uid)
                .build();
        return ApiMessage.success(200, "success", data);
    }

    @Nested
    @DisplayName("GET /api/settings/info")
    class GetInfo {
        @Test
        void ok() throws Exception {
            var userId = UUID.randomUUID();
            given(settingsService.getInfoSettings(userId)).willReturn(stubInfo(userId));

            mockMvc.perform(get("/api/settings/info")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.data.active").value(true));

            var captor = ArgumentCaptor.forClass(UUID.class);
            verify(settingsService).getInfoSettings(captor.capture());
            assertThat(captor.getValue()).isEqualTo(userId);
        }

        @Test
        void missing_request_attribute_returns_400() throws Exception {
            mockMvc.perform(get("/api/settings/info")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest()); // MissingRequestAttributeException 기대
        }
    }

    @Nested
    @DisplayName("GET /api/settings/follower")
    class GetFollower {
        @Test
        void ok() throws Exception {
            var userId = UUID.randomUUID();
            given(settingsService.getFollow(userId)).willReturn(stubList());

            mockMvc.perform(get("/api/settings/follower")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data[0].nickname").value("exampleUser"));
        }
    }

    @Nested
    @DisplayName("GET /api/settings/block")
    class GetBlock {
        @Test
        void ok() throws Exception {
            var userId = UUID.randomUUID();
            given(settingsService.getBlock(userId)).willReturn(stubList());

            mockMvc.perform(get("/api/settings/block")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data[0].profileImg").value("https://example.com/profile.jpg"));
        }
    }

    @Nested
    @DisplayName("POST /api/settings/follower/accept")
    class PostAccept {
        @Test
        void ok() throws Exception {
            var userId = UUID.randomUUID();
            var targetId = UUID.randomUUID();

            var req = new SettingsRequest(); // 테스트용 바디 (내용 없어도 OK)

            // ⬇️ 역직렬화된 다른 인스턴스를 매칭하도록 any() 사용
            given(settingsService.postAcceptUser(eq(userId), any(SettingsRequest.class)))
                    .willReturn(stubCheck(targetId));

            mockMvc.perform(post("/api/settings/follower/accept")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.userId").value(targetId.toString()));

            verify(settingsService).postAcceptUser(eq(userId), any(SettingsRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/settings/follower/block")
    class PostBlock {
        @Test
        void ok() throws Exception {
            var userId = UUID.randomUUID();
            var targetId = UUID.randomUUID();

            var req = new SettingsRequest();

            // ⬇️ 동일
            given(settingsService.postBlockUser(eq(userId), any(SettingsRequest.class)))
                    .willReturn(stubCheck(targetId));

            mockMvc.perform(post("/api/settings/follower/block")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.userId").value(targetId.toString()));

            verify(settingsService).postBlockUser(eq(userId), any(SettingsRequest.class));
        }
    }
}
