package com.osunji.melog.user.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.global.util.OidcUtil;
import com.osunji.melog.user.dto.request.OauthLoginRequestDTO;
import com.osunji.melog.user.dto.response.LoginResponseDTO;
import com.osunji.melog.user.repository.UserRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import com.osunji.melog.user.dto.RefreshResult;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.util.List;


@Service
public class AuthService {

    private final OidcService oidcService;
    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshRepo;
    private final UserRepository userRepository;

    private final long accessTtlMs;
    private final long refreshTtlMs;
    private final long refreshRoateBelow;
    private final OidcUtil oidcUtil;

    public AuthService(
            @Value("${jwt.access-expiration}") long accessTtlMs, //15분
            @Value("${jwt.refresh-expiration}") long refreshTtlMs, //14일
            @Value("${jwt.refresh-below}") long refreshRoateBelow,
            OidcService oidcService, JWTUtil jwtUtil, RefreshTokenRepository refreshRepo, UserRepository userRepository,
            OidcUtil oidcUtil) {
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
        this.refreshRoateBelow = refreshRoateBelow;
        this.oidcService = oidcService;
        this.jwtUtil = jwtUtil;
        this.refreshRepo = refreshRepo;
        this.userRepository = userRepository;
        this.oidcUtil = oidcUtil;
    }


    public LoginResponseDTO upsertUserFromKakaoIdToken(OauthLoginRequestDTO request)
            throws BadJOSEException, ParseException, JOSEException {

        // ✅ 1. ID 토큰 검증
        JWTClaimsSet claims = oidcUtil.verifyKakaoIdToken(request.getIdToken());

        // ✅ 2. 페이로드에서 값 추출
        String sub = claims.getSubject();
        if (sub == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_sub");

        String email = requireClaim(claims, "email");
        String nickname = requireClaim(claims, "nickname");
        String picture = requireClaim(claims, "picture");

        Platform platform = request.getPlatform();

        // ✅ 3. DB 조회 → 있으면 기존 유저, 없으면 새 유저 생성
        return userRepository.findByOidcAndPlatform(sub, platform)
                .map(user -> LoginResponseDTO.builder()
                        .isNewUser(false)
                        .user(convertToUserDTO(user))
                        .build()
                )
                .orElseGet(() -> {
                    User newUser = new User(email, platform, nickname, picture, null);
                    newUser.setOidc(sub);
                    User saved = userRepository.save(newUser);
                    return LoginResponseDTO.builder()
                            .isNewUser(true)
                            .user(convertToUserDTO(saved))
                            .build();
                });
    }
//    public User upsertUserFromKakaoIdToken(OauthLoginRequestDTO request)
//            throws BadJOSEException, ParseException, JOSEException {
//
//        // id token 검증
//        JWTClaimsSet claims = oidcUtil.verifyKakaoIdToken(request.getIdToken());
//
//        // 페이로드에서 값 추출 (변수명 유지)
//        String sub = claims.getSubject();
//        if (sub == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_sub");
//        String email = requireClaim(claims, "email");
//        String nickname = requireClaim(claims, "nickname");
//        String picture = requireClaim(claims, "picture");
//
//        // platform 꺼내기 (변수명 유지)
//        Platform platform = request.getPlatform();
//
//        // 1. DB 조회, 없으면 생성 (변수명 유지: user)
//
//        return userRepository.findByOidcAndPlatform(sub, platform)
//                .orElseGet(() -> {
//                    User newUser = new User(email, platform, nickname, picture, null);
//                    newUser.setOidc(sub);
//                    return userRepository.save(newUser);
//                });
//    }

    /** 2) 주어진 유저로 JWT 발급 + refresh 저장 (변수명 유지) */
    public RefreshResult issueJwtForUser(String userId) {
        // 토큰 생성 (변수명 유지)
        String access  = jwtUtil.createAccessToken(userId, accessTtlMs);
        String refresh = jwtUtil.createRefreshToken(userId, refreshTtlMs);

        // refresh 저장 (변수명 유지)
        String jti = jwtUtil.getJtiFromRefresh(refresh);
        long ttlSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(refresh));
        refreshRepo.save(userId, jti, refresh, ttlSec);

        return new RefreshResult(access, refresh, ttlSec);
    }


    /** 컨트롤러 시그니처: rotateTokens(String refreshCookie, HttpServletRequest req) */
    public RefreshResult rotateTokens(String refreshCookie, HttpServletRequest req) {
        if (refreshCookie == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_refresh_cookie");
        }

        // (선택) CSRF 완화: Origin / Custom Header 확인
        String origin = req.getHeader("Origin");
        if (!isAllowedOrigin(origin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_origin");
        }
        if (!"XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_xrw");
        }

        try {
            // 1) 토큰 검증
            jwtUtil.validateRefresh(refreshCookie);
            String userId = jwtUtil.getUserIdFromRefresh(refreshCookie);
            String oldJti = jwtUtil.getJtiFromRefresh(refreshCookie);

            // 2) 재사용/위조 탐지
            if (!refreshRepo.existsAndMatch(userId, oldJti, refreshCookie)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh_reuse_or_revoked");
            }

            // 3) 남은 TTL 계산
            long remainingSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(refreshCookie));

            // 4) 액세스만 재발급 (리프레시 충분히 남음)
            if (remainingSec > refreshRoateBelow) {
                String newAccess = jwtUtil.createAccessToken(userId, accessTtlMs);
                // 저장소 변경 없음, 기존 refresh 그대로 사용
                return new RefreshResult(newAccess, refreshCookie, remainingSec);
            }

            // 5) 리프레시 교체 (만료 임박)
            String newAccess  = jwtUtil.createAccessToken(userId, accessTtlMs);
            String newRefresh = jwtUtil.createRefreshToken(userId, refreshTtlMs);

            String newJti = jwtUtil.getJtiFromRefresh(newRefresh);
            long newTtlSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(newRefresh));

            // 저장은 새 키를 먼저, 그 다음 기존 키 삭제(짧은 경합 윈도우 최소화)
            refreshRepo.save(userId, newJti, newRefresh, newTtlSec);
            refreshRepo.delete(userId, oldJti);

            return new RefreshResult(newAccess, newRefresh, newTtlSec);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_refresh");
        }
    }

    public void logout(String refreshCookie) {
        if (refreshCookie == null) return;
        try {
            String userId = jwtUtil.getUserIdFromRefresh(refreshCookie);
            String jti = jwtUtil.getJtiFromRefresh(refreshCookie);
            refreshRepo.delete(userId, jti);
        } catch (Exception ignored) {
            // 이미 만료/삭제 등
        }
    }

    // ===== 내부 유틸 =====
    private static long ttlSecondsFromNow(long expEpochMillis) {
        long now = System.currentTimeMillis();
        return Math.max(0, (expEpochMillis - now) / 1000);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        return List.of(
                "https://app.melog.com",
                "https://staging.melog.com",
                "http://localhost:3000",
                "http://10.0.2.2:8080"
        ).contains(origin);
    }

    private String requireClaim(JWTClaimsSet claims, String key) {
        try {
            String value = claims.getStringClaim(key);
            if (value == null)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_" + key);
            return value;
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_" + key);
        }
    }

    private LoginResponseDTO.UserDTO convertToUserDTO(User user) {
        return LoginResponseDTO.UserDTO.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .platform(user.getPlatform().name().toLowerCase())
                .nickName(user.getNickname())
                .profileImg(user.getProfileImageUrl())
                .intro(user.getIntro())
                .build();
    }

}

