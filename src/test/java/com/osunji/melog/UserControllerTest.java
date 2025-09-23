package com.osunji.melog;

import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.user.controller.UserController;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.service.UserService;
import com.osunji.melog.global.dto.ApiMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerFollowingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("팔로우 여부 조회 - 언더스코어를 공백으로 복원해서 서비스가 호출된다")
    void getFollowing_success_withUnderscoreRestored() throws Exception {
        UUID me = UUID.randomUUID();
        String rawNickname = "홍길동_1";
        String realNickname = "홍길동 1";

        @SuppressWarnings("unchecked")
        ApiMessage<UserResponse.followingCheckResponse> api = org.mockito.Mockito.mock(ApiMessage.class);
        when(api.getCode()).thenReturn(200);
        when(userService.followingListByNickname(eq(me), eq(realNickname))).thenReturn(api);

        mockMvc.perform(
                        get("/api/users/following/{nickname}", rawNickname)
                                .requestAttr(JwtAuthFilter.USER_ID_ATTR, me)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(userService).followingListByNickname(eq(me), eq(realNickname));
    }

    @Test
    @DisplayName("팔로우 여부 조회 - 정규식 미스매치(너무 짧은 닉네임)는 404")
    void getFollowing_invalidPattern_returns404() throws Exception {
        UUID me = UUID.randomUUID();
        String invalid = "a"; // {2,20}에 걸려 매핑 자체 실패

        mockMvc.perform(
                        get("/api/users/following/{nickname}", invalid)
                                .requestAttr(JwtAuthFilter.USER_ID_ATTR, me)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("팔로우 여부 조회 - 공백 복원이 여러 개의 언더스코어에서도 동작")
    void getFollowing_multipleUnderscores() throws Exception {
        UUID me = UUID.randomUUID();
        String raw = "A_B_C";
        String real = "A B C";

        @SuppressWarnings("unchecked")
        ApiMessage<UserResponse.followingCheckResponse> api = org.mockito.Mockito.mock(ApiMessage.class);
        when(api.getCode()).thenReturn(200);
        when(userService.followingListByNickname(eq(me), eq(real))).thenReturn(api);

        mockMvc.perform(
                        get("/api/users/following/{nickname}", raw)
                                .requestAttr(JwtAuthFilter.USER_ID_ATTR, me)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(userService).followingListByNickname(eq(me), eq(real));
    }
}
