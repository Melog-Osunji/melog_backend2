package com.osunji.melog;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.user.controller.UserController;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.service.UserProfileMusicService;
import com.osunji.melog.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 팔로우 / 언팔로우 슬라이스 테스트
 */
@WebMvcTest(
        controllers = UserController.class,
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
class UserFollowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @MockBean
    UserProfileMusicService userProfileMusicService;

    @Test
    @DisplayName("팔로우 요청 성공 시 200과 followed 응답이 내려온다")
    void follow_success() throws Exception {
        // given
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        UserResponse.followingResponse body = UserResponse.followingResponse.builder()
                .userId(me.toString())
                .followingId(target.toString())
                .msg("followed")
                .build();

        ApiMessage<UserResponse.followingResponse> serviceResponse =
                ApiMessage.success(HttpStatus.OK.value(), "followed", body);

        Mockito.when(userService.following(any(UserRequest.following.class), eq(me)))
                .thenReturn(serviceResponse);

        UserRequest.following requestDto = UserRequest.following.builder()
                .follower(target.toString())
                .build();

        // when & then
        mockMvc.perform(post("/api/users/following")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("followed"))
                .andExpect(jsonPath("$.data.userId").value(me.toString()))
                .andExpect(jsonPath("$.data.followingId").value(target.toString()))
                .andExpect(jsonPath("$.data.msg").value("followed"));
    }

    @Test
    @DisplayName("언팔로우 요청 성공 시 200과 unfollowed 응답이 내려온다")
    void unfollow_success() throws Exception {
        // given
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        UserResponse.followingResponse body = UserResponse.followingResponse.builder()
                .userId(me.toString())
                .followingId(target.toString())
                .msg("unfollowed")
                .build();

        ApiMessage<UserResponse.followingResponse> serviceResponse =
                ApiMessage.success(HttpStatus.OK.value(), "unfollowed", body);

        Mockito.when(userService.following(any(UserRequest.following.class), eq(me)))
                .thenReturn(serviceResponse);

        UserRequest.following requestDto = UserRequest.following.builder()
                .follower(target.toString())
                .build();

        // when & then
        mockMvc.perform(post("/api/users/following")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("unfollowed"))
                .andExpect(jsonPath("$.data.userId").value(me.toString()))
                .andExpect(jsonPath("$.data.followingId").value(target.toString()))
                .andExpect(jsonPath("$.data.msg").value("unfollowed"));
    }

    @Test
    @DisplayName("차단된 유저를 팔로우 시도하면 403과 에러 메시지가 내려온다")
    void follow_blocked_forbidden() throws Exception {
        // given
        UUID me = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        ApiMessage<UserResponse.followingResponse> serviceResponse =
                ApiMessage.fail(
                        HttpStatus.FORBIDDEN.value(),
                        "차단된 사용자와는 팔로우할 수 없습니다."
                );

        Mockito.when(userService.following(any(UserRequest.following.class), eq(me)))
                .thenReturn(serviceResponse);

        UserRequest.following requestDto = UserRequest.following.builder()
                .follower(target.toString())
                .build();

        // when & then
        mockMvc.perform(post("/api/users/following")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .requestAttr(JwtAuthFilter.USER_ID_ATTR, me))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("차단된 사용자와는 팔로우할 수 없습니다."));
    }
}

