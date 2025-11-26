package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.inquirySettings.controller.SettingsController;
import com.osunji.melog.inquirySettings.dto.request.SettingsRequest;
import com.osunji.melog.inquirySettings.dto.response.SettingsResponse;
import com.osunji.melog.inquirySettings.service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SettingsController 슬라이스 테스트
 */
@WebMvcTest(
        controllers = SettingsController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.osunji.melog.global.config.SecurityConfig.class,
                        com.osunji.melog.global.security.JwtAuthFilter.class
                }
        )
)
class SettingsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SettingsService settingsService;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("GET /api/settings/info - 유저 설정 정보 조회 성공")
    void getSettingsInfo_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        ApiMessage<SettingsResponse.infoSettingsResponse> apiMessage = mock(ApiMessage.class);
        given(apiMessage.getCode()).willReturn(200);

        given(settingsService.getInfoSettings(userId))
                .willReturn(apiMessage);

        // when & then
        mockMvc.perform(get("/api/settings/info")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId))
                .andExpect(status().isOk());

        verify(settingsService).getInfoSettings(userId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("GET /api/settings/follower - 팔로우 요청 목록 조회 성공")
    void getAcceptFollow_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        ApiMessage<List<SettingsResponse.FollowResponse>> apiMessage = mock(ApiMessage.class);
        given(apiMessage.getCode()).willReturn(200);

        given(settingsService.getFollow(userId))
                .willReturn(apiMessage);

        // when & then
        mockMvc.perform(get("/api/settings/follower")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId))
                .andExpect(status().isOk());

        verify(settingsService).getFollow(userId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("GET /api/settings/follower/block - 차단한 유저 목록 조회 성공")
    void getBlock_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        ApiMessage<List<SettingsResponse.FollowResponse>> apiMessage = mock(ApiMessage.class);
        given(apiMessage.getCode()).willReturn(200);

        given(settingsService.getBlock(userId))
                .willReturn(apiMessage);

        // when & then
        mockMvc.perform(get("/api/settings/follower/block")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId))
                .andExpect(status().isOk());

        verify(settingsService).getBlock(userId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("POST /api/settings/follower/accept - 팔로우 요청 수락 성공")
    void postAcceptFollow_success() throws Exception {
        // given
        UUID me = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();

        SettingsRequest request = SettingsRequest.builder()
                .acceptUserId(followerId)
                .build();

        ApiMessage<SettingsResponse.CheckResponse> apiMessage = mock(ApiMessage.class);
        given(apiMessage.getCode()).willReturn(200);

        given(settingsService.postAcceptUser(eq(me), any(SettingsRequest.class)))
                .willReturn(apiMessage);

        // when & then
        mockMvc.perform(post("/api/settings/follower/accept")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, me)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // service 파라미터 검증
        ArgumentCaptor<SettingsRequest> captor = ArgumentCaptor.forClass(SettingsRequest.class);
        verify(settingsService).postAcceptUser(eq(me), captor.capture());
        SettingsRequest passed = captor.getValue();
        // acceptUserId가 제대로 전달되는지만 체크
        org.assertj.core.api.Assertions.assertThat(passed.getAcceptUserId())
                .isEqualTo(followerId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("POST /api/settings/block - 유저 차단 성공")
    void postBlockUser_success() throws Exception {
        // given
        UUID me = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        SettingsRequest request = SettingsRequest.builder()
                .acceptUserId(targetId)
                .build();

        ApiMessage<SettingsResponse.CheckResponse> apiMessage = mock(ApiMessage.class);
        given(apiMessage.getCode()).willReturn(200);

        given(settingsService.postBlockUser(eq(me), any(SettingsRequest.class)))
                .willReturn(apiMessage);

        // when & then
        mockMvc.perform(post("/api/settings/block")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, me)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<SettingsRequest> captor = ArgumentCaptor.forClass(SettingsRequest.class);
        verify(settingsService).postBlockUser(eq(me), captor.capture());
        SettingsRequest passed = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(passed.getAcceptUserId())
                .isEqualTo(targetId);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("POST /api/settings/unblock - 유저 차단 해제 성공")
    void postUnblockUser_success() throws Exception {
        // given
        UUID me = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        SettingsRequest request = SettingsRequest.builder()
                .acceptUserId(targetId)
                .build();

        ApiMessage<SettingsResponse.CheckResponse> apiMessage = mock(ApiMessage.class);
        given(apiMessage.getCode()).willReturn(200);

        given(settingsService.postUnblockUser(eq(me), any(SettingsRequest.class)))
                .willReturn(apiMessage);

        // when & then
        mockMvc.perform(post("/api/settings/unblock")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, me)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<SettingsRequest> captor = ArgumentCaptor.forClass(SettingsRequest.class);
        verify(settingsService).postUnblockUser(eq(me), captor.capture());
        SettingsRequest passed = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(passed.getAcceptUserId())
                .isEqualTo(targetId);
    }
}
