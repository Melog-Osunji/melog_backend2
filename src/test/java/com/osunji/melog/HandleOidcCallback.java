package com.osunji.melog;


import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.user.UserRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import com.osunji.melog.user.dto.RefreshResult;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import com.osunji.melog.user.service.AuthService;
import com.osunji.melog.user.service.OidcService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    OidcService oidcService;
    @Mock JWTUtil jwtUtil;
    @Mock RefreshTokenRepository refreshRepo;
    @Mock UserRepository userRepository;
    @Mock HttpServletRequest request;

    @InjectMocks
    AuthService authService;

    private static final String PROVIDER = "kakao";
    private static final Platform PLATFORM = Platform.KAKAO;
    private static final String CODE = "auth-code";
    private static final String STATE = "state-xyz";
    private static final String CODE_VERIFIER = "code-verifier";

    private static final String SUB = "kakao-sub-123";
    private static final String EMAIL = "user@example.com";
    private static final String NAME = "tester";
    private static final String PICTURE = "https://example.com/p.png";
    private static final String USER_ID = "user-uuid-1";

    // ===== handleOidcCallback =====
    @Nested
    @DisplayName("handleOidcCallback")
    class HandleOidcCallback {

        @Test
        @DisplayName("신규 유저면 회원 생성 후 토큰 발급")
        void signUpAndIssueTokens() {
            // given
            Map<String, Object> claims = Map.of(
                    "sub", SUB, "email", EMAIL, "name", NAME, "picture", PICTURE
            );
            when(oidcService.exchangeAndVerify(PROVIDER, CODE, STATE, CODE_VERIFIER)).thenReturn(claims);

            when(userRepository.findByOidcAndPlatform(SUB, PLATFORM)).thenReturn(Optional.empty());

            User saved = mock(User.class);
            when(saved.getId()).thenReturn(USER_ID);
            when(userRepository.save(any(User.class))).thenReturn(saved);

            String access = "access.jwt";
            String refresh = "refresh.jwt";
            long refreshExp = System.currentTimeMillis() + (14L * 24 * 3600 * 1000);
            String jti = "jti-123";

            when(jwtUtil.createAccessToken(eq(USER_ID), anyLong())).thenReturn(access);
            when(jwtUtil.createRefreshToken(eq(USER_ID), anyLong())).thenReturn(refresh);
            when(jwtUtil.getJtiFromRefresh(refresh)).thenReturn(jti);
            when(jwtUtil.getRefreshExpiryEpochMillis(refresh)).thenReturn(refreshExp);

            // when
            RefreshResult result = authService.handleOidcCallback(PROVIDER, CODE, STATE, CODE_VERIFIER);

            // then
            assertEquals(access, result.accessToken());
            assertEquals(refresh, result.refreshToken());
            assertTrue(result.refreshTtlSeconds() > 0);

            ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCap.capture());
            verify(refreshRepo).save(eq(USER_ID), eq(jti), eq(refresh), anyLong());

            // 불필요 스텁 없음(요청 헤더 스텁 X)
        }

        @Test
        @DisplayName("기존 유저면 로그인만 처리 후 토큰 발급")
        void loginExistingAndIssueTokens() {
            Map<String, Object> claims = Map.of(
                    "sub", SUB, "email", EMAIL, "name", NAME, "picture", PICTURE
            );
            when(oidcService.exchangeAndVerify(PROVIDER, CODE, STATE, CODE_VERIFIER)).thenReturn(claims);

            User existing = mock(User.class);
            when(existing.getId()).thenReturn(USER_ID);
            when(userRepository.findByOidcAndPlatform(SUB, PLATFORM)).thenReturn(Optional.of(existing));

            String access = "acc.jwt";
            String refresh = "ref.jwt";
            long exp = System.currentTimeMillis() + 3600_000;
            String jti = "jti-xx";

            when(jwtUtil.createAccessToken(eq(USER_ID), anyLong())).thenReturn(access);
            when(jwtUtil.createRefreshToken(eq(USER_ID), anyLong())).thenReturn(refresh);
            when(jwtUtil.getJtiFromRefresh(refresh)).thenReturn(jti);
            when(jwtUtil.getRefreshExpiryEpochMillis(refresh)).thenReturn(exp);

            RefreshResult result = authService.handleOidcCallback(PROVIDER, CODE, STATE, CODE_VERIFIER);

            assertEquals(access, result.accessToken());
            assertEquals(refresh, result.refreshToken());
            verify(userRepository, never()).save(any());
            verify(refreshRepo).save(eq(USER_ID), eq(jti), eq(refresh), anyLong());
        }

        @Test
        @DisplayName("필수 파라미터 누락 시 400")
        void badRequestWhenMissingFields() {
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.handleOidcCallback(null, CODE, STATE, CODE_VERIFIER)
            );
            assertEquals(400, ex.getStatusCode().value());
        }
    }

    // ===== rotateTokens =====
    @Nested
    @DisplayName("rotateTokens")
    class RotateTokens {

        private void allowHeaders() {
            when(request.getHeader("Origin")).thenReturn("http://localhost:3000");
            when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        }

        @Test
        @DisplayName("refresh 쿠키 없음 → 401")
        void noCookie() {
            // 헤더 스텁 불필요 (메서드 초반에서 바로 예외)
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.rotateTokens(null, request)
            );
            assertEquals(401, ex.getStatusCode().value());
            assertTrue(ex.getReason().contains("no_refresh_cookie"));
        }

        @Test
        @DisplayName("Origin 미허용 → 403")
        void invalidOrigin() {
            // 필요 스텁만 최소화
            when(request.getHeader("Origin")).thenReturn("https://evil.com");
            when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.rotateTokens("refresh.jwt", request)
            );
            assertEquals(403, ex.getStatusCode().value());
            assertTrue(ex.getReason().contains("invalid_origin"));
        }

        @Test
        @DisplayName("X-Requested-With 누락 → 400")
        void missingXRW() {
            when(request.getHeader("Origin")).thenReturn("http://localhost:3000");
            when(request.getHeader("X-Requested-With")).thenReturn(null);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.rotateTokens("refresh.jwt", request)
            );
            assertEquals(400, ex.getStatusCode().value());
            assertTrue(ex.getReason().contains("missing_xrw"));
        }

        @Test
        @DisplayName("재사용/위조 탐지 실패 → 401")
        void reuseDetected() {
            allowHeaders();

            String refresh = "refresh.jwt";
            String userId = USER_ID;
            String jti = "jti-old";

            // void 메서드 스텁: 필요한 테스트에서만
            doNothing().when(jwtUtil).validateRefresh(refresh);
            when(jwtUtil.getUserIdFromRefresh(refresh)).thenReturn(userId);
            when(jwtUtil.getJtiFromRefresh(refresh)).thenReturn(jti);
            when(refreshRepo.existsAndMatch(userId, jti, refresh)).thenReturn(false);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.rotateTokens(refresh, request)
            );
            assertEquals(401, ex.getStatusCode().value());
            assertTrue(ex.getReason().contains("refresh_reuse_or_revoked"));
        }

        @Test
        @DisplayName("남은 TTL 충분 → access만 재발급, refresh 유지")
        void accessOnlyWhenPlentyTtl() {
            allowHeaders();

            String refresh = "refresh.jwt";
            String userId = USER_ID;
            String jti = "jti-old";

            doNothing().when(jwtUtil).validateRefresh(refresh);
            when(jwtUtil.getUserIdFromRefresh(refresh)).thenReturn(userId);
            when(jwtUtil.getJtiFromRefresh(refresh)).thenReturn(jti);
            when(refreshRepo.existsAndMatch(userId, jti, refresh)).thenReturn(true);

            long exp = System.currentTimeMillis() + (10L * 24 * 3600 * 1000);
            when(jwtUtil.getRefreshExpiryEpochMillis(refresh)).thenReturn(exp);

            String newAccess = "new.access.jwt";
            when(jwtUtil.createAccessToken(eq(userId), anyLong())).thenReturn(newAccess);

            RefreshResult result = authService.rotateTokens(refresh, request);

            assertEquals(newAccess, result.accessToken());
            assertEquals(refresh, result.refreshToken());
            verify(refreshRepo, never()).save(any(), any(), any(), anyLong());
            verify(refreshRepo, never()).delete(any(), any());
        }

        @Test
        @DisplayName("남은 TTL 부족 → access + refresh 둘 다 재발급(rotate)")
        void rotateWhenTtlLow() {
            allowHeaders();

            String oldRefresh = "refresh.old.jwt";
            String userId = USER_ID;
            String oldJti = "jti-old";

            doNothing().when(jwtUtil).validateRefresh(oldRefresh);
            when(jwtUtil.getUserIdFromRefresh(oldRefresh)).thenReturn(userId);
            when(jwtUtil.getJtiFromRefresh(oldRefresh)).thenReturn(oldJti);
            when(refreshRepo.existsAndMatch(userId, oldJti, oldRefresh)).thenReturn(true);

            long expSoon = System.currentTimeMillis() + 3600_000;
            when(jwtUtil.getRefreshExpiryEpochMillis(oldRefresh)).thenReturn(expSoon);

            String newAccess = "new.access.jwt";
            String newRefresh = "new.refresh.jwt";
            String newJti = "jti-new";
            long newExp = System.currentTimeMillis() + (14L * 24 * 3600 * 1000);

            when(jwtUtil.createAccessToken(eq(userId), anyLong())).thenReturn(newAccess);
            when(jwtUtil.createRefreshToken(eq(userId), anyLong())).thenReturn(newRefresh);
            when(jwtUtil.getJtiFromRefresh(newRefresh)).thenReturn(newJti);
            when(jwtUtil.getRefreshExpiryEpochMillis(newRefresh)).thenReturn(newExp);

            RefreshResult result = authService.rotateTokens(oldRefresh, request);

            assertEquals(newAccess, result.accessToken());
            assertEquals(newRefresh, result.refreshToken());
            verify(refreshRepo).save(eq(userId), eq(newJti), eq(newRefresh), anyLong());
            verify(refreshRepo).delete(eq(userId), eq(oldJti));
        }

        @Test
        @DisplayName("유효하지 않은 refresh → 401 invalid_refresh")
        void invalidRefresh() {
            allowHeaders();

            String refresh = "bad.jwt";
            // void 예외는 doThrow
            doThrow(new RuntimeException("bad token")).when(jwtUtil).validateRefresh(refresh);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.rotateTokens(refresh, request)
            );
            assertEquals(401, ex.getStatusCode().value());
            assertTrue(ex.getReason().contains("invalid_refresh"));
        }
    }

    // ===== logout =====
    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("정상 삭제")
        void ok() {
            String refresh = "refresh.jwt";
            String userId = USER_ID;
            String jti = "jti";

            when(jwtUtil.getUserIdFromRefresh(refresh)).thenReturn(userId);
            when(jwtUtil.getJtiFromRefresh(refresh)).thenReturn(jti);

            assertDoesNotThrow(() -> authService.logout(refresh));
            verify(refreshRepo).delete(userId, jti);
        }

        @Test
        @DisplayName("예외 발생해도 무시")
        void ignoreExceptions() {
            String refresh = "refresh.jwt";
            when(jwtUtil.getUserIdFromRefresh(refresh)).thenThrow(new RuntimeException("fail"));

            assertDoesNotThrow(() -> authService.logout(refresh));
            verify(refreshRepo, never()).delete(any(), any());
        }
    }
}
