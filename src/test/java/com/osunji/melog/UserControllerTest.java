package com.osunji.melog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.user.controller.UserController;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 단위 테스트
 * - 보안 필터 비활성화(addFilters = false)로 컨트롤러만 검증
 * - @RequestAttribute(JwtAuthFilter.USER_ID_ATTR)는 requestAttr(...)로 직접 주입
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;


    private ObjectMapper om;
    private UUID userId;

    @BeforeEach
    void setUp() {
        om = new ObjectMapper();
        userId = UUID.randomUUID();
    }

    // (생략) 클래스/필드/셋업 동일

    @Test
    @DisplayName("POST /api/users/agreement - 약관 동의 저장 성공")
    void agreement_success() throws Exception {
        String reqJson = """
                    {"agree": true}
                """;

        UserResponse.AgreementResponse body =
                UserResponse.AgreementResponse.builder()
                        .id(userId.toString())
                        .marketing(true)
                        .createdAt("2025-09-23T12:00:00")
                        .build();

        // 제네릭과 무관하게 raw로 맞춰주기 위해 캐스팅
        @SuppressWarnings("unchecked")
        ApiMessage<?> mock = (ApiMessage<?>) ApiMessage.success(
                HttpStatus.OK.value(), "약관 동의 성공", body
        );

        Mockito.when(userService.agreement(any(UserRequest.agreement.class), eq(userId)))
                .thenReturn((ApiMessage) mock); // ← 핵심

        mockMvc.perform(post("/api/users/agreement")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("약관 동의 성공"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.marketing").value(true))
                .andExpect(jsonPath("$.data.createdAt").value("2025-09-23T12:00:00"));
    }

    @Test
    @DisplayName("POST /api/users/onboarding - 온보딩 생성 성공")
    void onboarding_success() throws Exception {
        String reqJson = """
                    {
                      "composer":   ["베토벤","모차르트"],
                      "period":     ["고전주의"],
                      "instrument": ["피아노"]
                    }
                """;

        UserResponse.OnboardingResponse body =
                UserResponse.OnboardingResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId.toString())
                        .composer(List.of("베토벤", "모차르트"))
                        .period(List.of("고전주의"))
                        .instrument(List.of("피아노"))
                        .build();

        @SuppressWarnings("unchecked")
        ApiMessage<?> mock = (ApiMessage<?>) ApiMessage.success(
                HttpStatus.CREATED.value(), "온보딩 성공", body
        );

        Mockito.when(userService.onboarding(any(UserRequest.onboarding.class), eq(userId)))
                .thenReturn((ApiMessage) mock); // ← 핵심

        mockMvc.perform(post("/api/users/onboarding")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("온보딩 성공"))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.composer[0]").value("베토벤"))
                .andExpect(jsonPath("$.data.period[0]").value("고전주의"))
                .andExpect(jsonPath("$.data.instrument[0]").value("피아노"));
    }

    @Test
    @DisplayName("PATCH /api/users/onboarding - 온보딩 수정 성공")
    void patchOnboarding_success() throws Exception {
        String reqJson = """
                    {
                      "composer":   ["드보르자크"],
                      "period":     ["낭만주의"],
                      "instrument": ["첼로"]
                    }
                """;

        UserResponse.OnboardingResponse body =
                UserResponse.OnboardingResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId.toString())
                        .composer(List.of("드보르자크"))
                        .period(List.of("낭만주의"))
                        .instrument(List.of("첼로"))
                        .build();

        @SuppressWarnings("unchecked")
        ApiMessage<?> mock = (ApiMessage<?>) ApiMessage.success(
                HttpStatus.OK.value(), "온보딩 수정 성공", body
        );

        Mockito.when(userService.patchOnboarding(any(UserRequest.onboardingPatch.class), eq(userId)))
                .thenReturn((ApiMessage) mock); // ← 핵심

        mockMvc.perform(patch("/api/users/onboarding")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("온보딩 수정 성공"))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.composer[0]").value("드보르자크"))
                .andExpect(jsonPath("$.data.period[0]").value("낭만주의"))
                .andExpect(jsonPath("$.data.instrument[0]").value("첼로"));
    }

    @Test
    @DisplayName("PATCH /api/users/profile - 프로필 수정 성공")
    void profile_success() throws Exception {
        String reqJson = """
                    {
                      "nickName":  "새닉",
                      "profileImg":"https://img.example.com/a.png",
                      "intro":     "클래식 러버"
                    }
                """;

        UserResponse.ProfileResponse body =
                UserResponse.ProfileResponse.builder()
                        .id(userId.toString())
                        .email("user@example.com")
                        .platform("kakao")
                        .nickName("새닉")
                        .profileImg("https://img.example.com/a.png")
                        .intro("클래식 러버")
                        .build();

        @SuppressWarnings("unchecked")
        ApiMessage<?> mock = (ApiMessage<?>) ApiMessage.success(
                HttpStatus.OK.value(), "프로필 수정 성공", body
        );

        Mockito.when(userService.profile(any(UserRequest.profilePatch.class), eq(userId)))
                .thenReturn((ApiMessage) mock); // ← 핵심

        mockMvc.perform(patch("/api/users/profile")
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("프로필 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.platform").value("kakao"))
                .andExpect(jsonPath("$.data.nickName").value("새닉"))
                .andExpect(jsonPath("$.data.profileImg").value("https://img.example.com/a.png"))
                .andExpect(jsonPath("$.data.intro").value("클래식 러버"));
    }

    @Nested
    class NegativeCases {
        @Test
        @DisplayName("POST /api/users/onboarding - 이미 완료된 경우 409")
        void onboarding_conflict409() throws Exception {
            String reqJson = """
                        {
                          "composer":   ["베토벤"],
                          "period":     ["고전주의"],
                          "instrument": ["피아노"]
                        }
                    """;

            @SuppressWarnings("unchecked")
            ApiMessage<?> conflict = (ApiMessage<?>) ApiMessage.fail(
                    HttpStatus.CONFLICT.value(), "이미 온보딩을 완료한 사용자입니다."
            );

            Mockito.when(userService.onboarding(any(UserRequest.onboarding.class), eq(userId)))
                    .thenReturn((ApiMessage) conflict); // ← 핵심

            mockMvc.perform(post("/api/users/onboarding")
                            .requestAttr(JwtAuthFilter.USER_ID_ATTR, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(reqJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 온보딩을 완료한 사용자입니다."))
                    .andExpect(jsonPath("$.body").doesNotExist());
        }
    }
}