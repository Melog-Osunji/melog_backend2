package com.osunji.melog;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.user.controller.UserController;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class   // JwtAuthFilter를 컴포넌트 스캔에서 제외
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    // Service는 mock으로 주입
    @MockBean
    private UserService userService;

    private static final UUID FIXED_UID =
            UUID.fromString("11111111-2222-3333-4444-555555555555");

    // ===== Agreement / Marketing =====
    @Nested
    class AgreementMarketing {

        @Test
        @DisplayName("POST /api/users/agreement - 약관 동의(마케팅) 저장 성공 → 200")
        void agreement_ok() throws Exception {
            ApiMessage<UserResponse.AgreementResponse> stub =
                    ApiMessage.success(200, "OK", Mockito.mock(UserResponse.AgreementResponse.class));
            when(userService.createAgreement(any(UserRequest.agreement.class), eq(FIXED_UID)))
                    .thenReturn(stub);

            Map<String, Object> req = Map.of("marketing", true);

            mockMvc.perform(post("/api/users/agreement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("PATCH /api/users/marketing - 마케팅 동의 업데이트 → 200")
        void marketing_patch_ok() throws Exception {
            ApiMessage<UserResponse.AgreementResponse> stub =
                    ApiMessage.success(200, "OK", Mockito.mock(UserResponse.AgreementResponse.class));
            when(userService.updateMarketing(any(UserRequest.agreement.class), eq(FIXED_UID)))
                    .thenReturn(stub);

            Map<String, Object> req = Map.of("marketing", false);

            mockMvc.perform(patch("/api/users/marketing")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/marketing - 마케팅 동의 조회 → 200")
        void marketing_get_ok() throws Exception {
            ApiMessage<UserResponse.AgreementResponse> stub =
                    ApiMessage.success(200, "OK", Mockito.mock(UserResponse.AgreementResponse.class));
            when(userService.getMarketing(eq(FIXED_UID))).thenReturn(stub);

            mockMvc.perform(get("/api/users/marketing")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isOk());
        }
    }

    // ===== Onboarding =====
    @Nested
    class Onboarding {

        @Test
        @DisplayName("POST /api/users/onboarding - 온보딩 생성 성공 → 201 or 200")
        void onboarding_post_ok() throws Exception {
            ApiMessage<UserResponse.OnboardingResponse> stub =
                    ApiMessage.success(201, "CREATED", Mockito.mock(UserResponse.OnboardingResponse.class));
            when(userService.onboarding(any(UserRequest.onboarding.class), eq(FIXED_UID)))
                    .thenReturn(stub);

            Map<String, Object> req = new HashMap<>();
            req.put("composer", List.of("Beethoven", "Mozart"));
            req.put("period", List.of("Classical", "Romantic"));
            req.put("instrument", List.of("Piano"));

            mockMvc.perform(post("/api/users/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("PATCH /api/users/onboarding - 온보딩 부분 수정 → 200")
        void onboarding_patch_ok() throws Exception {
            ApiMessage<?> stub = ApiMessage.success(200, "OK", Map.of("patched", true));
            when(userService.patchOnboarding(any(UserRequest.onboardingPatch.class), eq(FIXED_UID)))
                    .thenReturn((ApiMessage<UserResponse.OnboardingResponse>) stub);

            Map<String, Object> req = new HashMap<>();
            req.put("instrument", List.of("Violin", "Cello"));

            mockMvc.perform(patch("/api/users/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/onboarding - 온보딩 조회 → 200")
        void onboarding_get_ok() throws Exception {
            ApiMessage<?> stub = ApiMessage.success(200, "OK", Map.of("exists", true));
            when(userService.getOnboarding(eq(FIXED_UID))).thenReturn((ApiMessage<UserResponse.OnboardingResponse>) stub);

            mockMvc.perform(get("/api/users/onboarding")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isOk());
        }
    }

    // ===== Profile =====
    @Nested
    class Profile {

        @Test
        @DisplayName("PATCH /api/users/profile - 프로필 수정 → 200")
        void profile_patch_ok() throws Exception {
            ApiMessage<UserResponse.ProfileResponse> stub =
                    ApiMessage.success(200, "OK", Mockito.mock(UserResponse.ProfileResponse.class));
            when(userService.profile(any(UserRequest.profilePatch.class), eq(FIXED_UID)))
                    .thenReturn(stub);

            Map<String, Object> req = new HashMap<>();
            req.put("nickname", "martin");
            req.put("bio", "hello world");

            mockMvc.perform(patch("/api/users/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/users/profile - 프로필 조회 → 200")
        void profile_get_ok() throws Exception {
            ApiMessage<UserResponse.ProfileResponse> stub =
                    ApiMessage.success(200, "OK", Mockito.mock(UserResponse.ProfileResponse.class));
            when(userService.getProfile(eq(FIXED_UID))).thenReturn(stub);

            mockMvc.perform(get("/api/users/profile")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isOk());
        }
    }

    // ===== Negative / Edge =====
    @Nested
    class NegativeCases {

        @Test
        @DisplayName("USER_ID_ATTR 누락 시 → 400 (MissingServletRequestAttributeException)")
        void missing_user_id_attr_400() throws Exception {
            Map<String, Object> req = Map.of("marketing", true);

            mockMvc.perform(post("/api/users/agreement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("온보딩 중복 생성 시 → 409 (서비스에서 Fail 코드 반환)")
        void onboarding_conflict_409() throws Exception {
            ApiMessage<UserResponse.OnboardingResponse> stub =
                    ApiMessage.fail(409, "이미 온보딩을 완료한 사용자입니다.");
            when(userService.onboarding(any(UserRequest.onboarding.class), eq(FIXED_UID)))
                    .thenReturn(stub);

            Map<String, Object> req = new HashMap<>();
            req.put("composer", List.of("Mozart"));
            req.put("period", List.of("Classical"));
            req.put("instrument", List.of("Piano"));

            mockMvc.perform(post("/api/users/onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(req))
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, FIXED_UID))
                    .andExpect(status().isConflict());
        }
    }
}
