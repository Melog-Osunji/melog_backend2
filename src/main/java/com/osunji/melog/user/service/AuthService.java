package com.osunji.melog.user.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.osunji.melog.global.util.GoogleOidcUtil;
import com.osunji.melog.global.util.JWTUtil;
import com.osunji.melog.global.util.KakaoOidcUtil;
import com.osunji.melog.global.util.NaverOidcUtil;
import com.osunji.melog.user.dto.request.OauthLoginRequestDTO;
import com.osunji.melog.user.dto.response.LoginResponseDTO;
import com.osunji.melog.user.repository.UserRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import com.osunji.melog.user.dto.RefreshResult;
import com.osunji.melog.user.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final OidcService oidcService;
    private final JWTUtil jwtUtil;
    private final RefreshTokenRepository refreshRepo;
    private final UserRepository userRepository;

    private final long accessTtlMs;
    private final long refreshTtlMs;
    private final long refreshRoateBelow;
    private final KakaoOidcUtil kakaoOidcUtil;
    private final GoogleOidcUtil googleOidcUtil;
    private final NaverOidcUtil naverOidcUtil;

    public AuthService(
            @Value("${jwt.access-expiration}") long accessTtlMs,
            @Value("${jwt.refresh-expiration}") long refreshTtlMs,
            @Value("${jwt.refresh-below}") long refreshRotateBelow,
            OidcService oidcService, JWTUtil jwtUtil, RefreshTokenRepository refreshRepo,
            UserRepository userRepository, KakaoOidcUtil kakaoOidcUtil, GoogleOidcUtil googleOidcUtil, NaverOidcUtil naverOidcUtil) {

        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
        this.refreshRoateBelow = refreshRotateBelow;
        this.oidcService = oidcService;
        this.jwtUtil = jwtUtil;
        this.refreshRepo = refreshRepo;
        this.userRepository = userRepository;
        this.kakaoOidcUtil = kakaoOidcUtil;

        log.info("✅ AuthService initialized (accessTtlMs={}ms, refreshTtlMs={}ms, rotateBelow={}s)",
                accessTtlMs, refreshTtlMs, refreshRotateBelow);
        this.googleOidcUtil = googleOidcUtil;
        this.naverOidcUtil = naverOidcUtil;
    }

    public LoginResponseDTO upsertUserFromKakaoIdToken(OauthLoginRequestDTO request)
            throws BadJOSEException, ParseException, JOSEException {

        log.debug("🟡 [AuthService] upsertUserFromKakaoIdToken() called for platform={}, idToken length={}",
                request.getPlatform(), request.getIdToken() != null ? request.getIdToken().length() : 0);

        // 1. ID 토큰 검증
        JWTClaimsSet claims = kakaoOidcUtil.verifyKakaoIdToken(request.getIdToken());
        log.debug("✅ ID Token verified. Claims: sub={}, email={}, nickname={}",
                claims.getSubject(), claims.getStringClaim("email"), claims.getStringClaim("nickname"));

        // 2. 페이로드에서 값 추출
        String sub = claims.getSubject();
        if (sub == null) {
            log.error("❌ Missing sub claim");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_sub");
        }

        String email = requireClaim(claims, "email");
        String nickname = requireClaim(claims, "nickname");
        String picture = requireClaim(claims, "picture");
        Platform platform = request.getPlatform();

        log.debug("📦 Extracted user info: sub={}, email={}, nickname={}, platform={}", sub, email, nickname, platform);

        // 3. DB 조회 → 있으면 기존 유저, 없으면 새 유저 생성
        return userRepository.findByOidcAndPlatform(sub, platform)
                .map(user -> {
                    log.info("👤 Existing user found (oidc={}, platform={})", sub, platform);
                    return LoginResponseDTO.builder()
                            .isNewUser(false)
                            .user(convertToUserDTO(user))
                            .build();
                })
                .orElseGet(() -> {
                    log.info("🆕 No existing user found. Creating new user (email={}, platform={})", email, platform);
                    User newUser = new User(email, platform, nickname, picture, null);
                    newUser.setOidc(sub);
                    User saved = userRepository.save(newUser);
                    log.info("✅ New user created with ID={}", saved.getId());
                    return LoginResponseDTO.builder()
                            .isNewUser(true)
                            .user(convertToUserDTO(saved))
                            .build();
                });
    }

    public LoginResponseDTO upsertUserFromGoogleIdToken(OauthLoginRequestDTO request)
            throws BadJOSEException, ParseException, JOSEException {

        log.debug("🟡 [AuthService] upsertUserFromGoogleIdToken() called for platform={}, idToken length={}",
                request.getPlatform(), request.getIdToken() != null ? request.getIdToken().length() : 0);

        // 1. ID 토큰 검증
        JWTClaimsSet claims = googleOidcUtil.verifyGoogleIdToken(request.getIdToken());
        log.debug("✅ ID Token verified. Claims: sub={}, email={}, nickname={}",
                claims.getSubject(), claims.getStringClaim("email"), claims.getStringClaim("nickname"));

        // 2. 페이로드에서 값 추출
        String sub = claims.getSubject();
        if (sub == null) {
            log.error("❌ Missing sub claim");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_sub");
        }

        String email = requireClaim(claims, "email");
        String nickname = requireClaim(claims, "nickname");
        String picture = requireClaim(claims, "picture");
        Platform platform = request.getPlatform();

        log.debug("📦 Extracted user info: sub={}, email={}, nickname={}, platform={}", sub, email, nickname, platform);

        // 3. DB 조회 → 있으면 기존 유저, 없으면 새 유저 생성
        return userRepository.findByOidcAndPlatform(sub, platform)
                .map(user -> {
                    log.info("👤 Existing user found (oidc={}, platform={})", sub, platform);
                    return LoginResponseDTO.builder()
                            .isNewUser(false)
                            .user(convertToUserDTO(user))
                            .build();
                })
                .orElseGet(() -> {
                    log.info("🆕 No existing user found. Creating new user (email={}, platform={})", email, platform);
                    User newUser = new User(email, platform, nickname, picture, null);
                    newUser.setOidc(sub);
                    User saved = userRepository.save(newUser);
                    log.info("✅ New user created with ID={}", saved.getId());
                    return LoginResponseDTO.builder()
                            .isNewUser(true)
                            .user(convertToUserDTO(saved))
                            .build();
                });
    }

    public LoginResponseDTO upsertUserFromNaverIdToken(OauthLoginRequestDTO request)
            throws BadJOSEException, ParseException, JOSEException {

        log.debug("🟡 [AuthService] upsertUserFromNaverIdToken() called for platform={}, idToken length={}",
                request.getPlatform(), request.getIdToken() != null ? request.getIdToken().length() : 0);

        // 1. ID 토큰 검증
        JWTClaimsSet claims = naverOidcUtil.verifyNaverIdToken(request.getIdToken());
        log.debug("✅ ID Token verified. Claims: sub={}, email={}, nickname={}",
                claims.getSubject(), claims.getStringClaim("email"), claims.getStringClaim("nickname"));

        // 2. 페이로드에서 값 추출
        String sub = claims.getSubject();
        if (sub == null) {
            log.error("❌ Missing sub claim");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_sub");
        }

        String email = requireClaim(claims, "email");
        String nickname = requireClaim(claims, "nickname");
        String picture = requireClaim(claims, "picture");
        Platform platform = request.getPlatform();

        log.debug("📦 Extracted user info: sub={}, email={}, nickname={}, platform={}", sub, email, nickname, platform);

        // 3. DB 조회 → 있으면 기존 유저, 없으면 새 유저 생성
        return userRepository.findByOidcAndPlatform(sub, platform)
                .map(user -> {
                    log.info("👤 Existing user found (oidc={}, platform={})", sub, platform);
                    return LoginResponseDTO.builder()
                            .isNewUser(false)
                            .user(convertToUserDTO(user))
                            .build();
                })
                .orElseGet(() -> {
                    log.info("🆕 No existing user found. Creating new user (email={}, platform={})", email, platform);
                    User newUser = new User(email, platform, nickname, picture, null);
                    newUser.setOidc(sub);
                    User saved = userRepository.save(newUser);
                    log.info("✅ New user created with ID={}", saved.getId());
                    return LoginResponseDTO.builder()
                            .isNewUser(true)
                            .user(convertToUserDTO(saved))
                            .build();
                });
    }


    public RefreshResult issueJwtForUser(String userId) {
        log.debug("🔑 [AuthService] issueJwtForUser() called for userId={}", userId);
        String access  = jwtUtil.createAccessToken(userId, accessTtlMs);
        String refresh = jwtUtil.createRefreshToken(userId, refreshTtlMs);
        String jti = jwtUtil.getJtiFromRefresh(refresh);
        long ttlSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(refresh));

        log.info("✅ JWTs issued for userId={} (accessTTL={}ms, refreshTTL={}ms, refreshJti={})",
                userId, accessTtlMs, refreshTtlMs, jti);

        refreshRepo.save(userId, jti, refresh, ttlSec);
        log.debug("💾 Refresh token saved in repository (ttlSec={})", ttlSec);

        return new RefreshResult(access, refresh, ttlSec);
    }

    public RefreshResult rotateTokens(String refreshToken, HttpServletRequest req) {
        log.debug("♻️ [AuthService] rotateTokens() called");

        // 1) 헤더 기반이므로 null/blank 먼저 차단
        if (refreshToken == null || refreshToken.isBlank()) {
            log.error("❌ Missing refresh token in headers");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_refresh_token");
        }

        // 2) RN 고려: Origin은 null일 수 있음 → null은 허용, 값이 있을 때만 화이트리스트 검사
        String origin = req.getHeader("Origin");
        if (origin != null && !isAllowedOrigin(origin)) {
            log.warn("⚠️ Forbidden origin attempted: {}", origin);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid_origin");
        }

//        // 3) X-Requested-With 정책: 필요시만 검사 (프로퍼티로 온/오프 권장)
//        String xrw = req.getHeader("X-Requested-With");
//        if (requireXrw && !"XMLHttpRequest".equals(xrw)) {
//            log.warn("⚠️ Missing or invalid X-Requested-With header");
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing_xrw");
//        }

        try {
            // 4) 토큰 검증/파싱
            jwtUtil.validateRefresh(refreshToken);
            String userId = jwtUtil.getUserIdFromRefresh(refreshToken);
            String oldJti = jwtUtil.getJtiFromRefresh(refreshToken);
            log.debug("🧾 Refresh token validated (userId={}, jti={})", userId, oldJti);

            // 5) 재사용/폐기 여부
            if (!refreshRepo.existsAndMatch(userId, oldJti, refreshToken)) {
                log.error("❌ Refresh reuse or revoked (userId={}, jti={})", userId, oldJti);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh_reuse_or_revoked");
            }

            Long remainingSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(refreshToken));
            log.debug("🕒 Remaining TTL for refresh token: {}s", remainingSec);

            if (remainingSec > refreshRoateBelow) {
                log.info("🔁 Access token reissued only (refresh still valid)");
                String newAccess = jwtUtil.createAccessToken(userId, accessTtlMs);
                return new RefreshResult(newAccess, refreshToken, remainingSec);
            }

            // 7) 회전
            log.info("⏳ Refresh token nearing expiration, issuing new refresh...");
            String newAccess  = jwtUtil.createAccessToken(userId, accessTtlMs);
            String newRefresh = jwtUtil.createRefreshToken(userId, refreshTtlMs);

            String newJti  = jwtUtil.getJtiFromRefresh(newRefresh);
            long newTtlSec = ttlSecondsFromNow(jwtUtil.getRefreshExpiryEpochMillis(newRefresh));

            // 새 키 저장 → 기존 키 삭제 (경합 최소화)
            refreshRepo.save(userId, newJti, newRefresh, newTtlSec);
            refreshRepo.delete(userId, oldJti);
            log.info("✅ Tokens rotated successfully (userId={}, newJti={})", userId, newJti);

            return new RefreshResult(newAccess, newRefresh, newTtlSec);

        } catch (ResponseStatusException e) {
            log.error("❌ Token rotation failed: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during token rotation", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_refresh");
        }
    }


    public String extractRefreshFromHeaders(String authorization, String xRefreshToken) {
        if (xRefreshToken != null && !xRefreshToken.isBlank()) {
            return xRefreshToken.trim();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            return token.isBlank() ? null : token;
        }
        return null;
    }

    public void logout(String refreshCookie) {
        log.debug("🚪 [AuthService] logout() called");
        if (refreshCookie == null) {
            log.warn("⚠️ logout() called with null cookie");
            return;
        }
        try {
            String userId = jwtUtil.getUserIdFromRefresh(refreshCookie);
            String jti = jwtUtil.getJtiFromRefresh(refreshCookie);
            refreshRepo.delete(userId, jti);
            log.info("✅ User logged out (userId={}, jti deleted={})", userId, jti);
        } catch (Exception e) {
            log.warn("⚠️ logout() ignored error: {}", e.getMessage());
        }
    }

    // ===== 내부 유틸 =====
    private static long ttlSecondsFromNow(long expEpochMillis) {
        long now = System.currentTimeMillis();
        return Math.max(0, (expEpochMillis - now) / 1000);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null) return false;
        boolean allowed = List.of(
                "https://melog.org",
                "http://yanggang.iptime.org",
                "http://localhost:3000",
                "http://10.0.2.2:8080"
        ).contains(origin);
        if (!allowed) log.debug("❌ Disallowed origin detected: {}", origin);
        return allowed;
    }

    private String requireClaim(JWTClaimsSet claims, String key) {
        try {
            String value = claims.getStringClaim(key);
            if (value == null) {
                log.error("❌ Missing claim: {}", key);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_" + key);
            }
            return value;
        } catch (ParseException e) {
            log.error("❌ Invalid claim format for {}", key);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_" + key);
        }
    }

    private LoginResponseDTO.UserDTO convertToUserDTO(User user) {
        log.debug("🧩 Converting User entity to DTO (userId={})", user.getId());
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
