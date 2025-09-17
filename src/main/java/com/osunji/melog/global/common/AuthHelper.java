package com.osunji.melog.global.common;

import java.util.UUID;

import com.osunji.melog.global.util.JWTUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthHelper {
    private final JWTUtil jwtUtil;

    public String authHelper(String authHeader) {
        System.out.println("🔍 AuthHelper.authHelper 시작");

        // 헤더가 비었는지 확인
        if (authHeader == null || authHeader.isEmpty()) {
            System.out.println("❌ Authorization 헤더가 비어있음");
            throw new IllegalStateException("Empty Authorization header");
        }
        System.out.println("✅ Authorization 헤더 존재: " + authHeader.length() + "자");

        // 헤더가 Bearer로 시작하는지 확인
        if (!authHeader.startsWith("Bearer ")) {
            System.out.println("❌ Bearer 형식이 아님. 실제: " + authHeader.substring(0, Math.min(20, authHeader.length())));
            throw new IllegalStateException("Invalid Authorization header");
        }
        System.out.println("✅ Bearer 형식 확인");

        // 토큰 앞의 "Bearer"을 제외하고, 토큰 부분만 token 변수에 할당
        String token = authHeader.substring("Bearer ".length());
        System.out.println("✅ 토큰 추출 완료: " + token.length() + "자");

        // 토큰의 만료 여부 확인
        if (jwtUtil.isTokenExpired(token)) {
            System.out.println("❌ 토큰이 만료됨");
            throw new IllegalStateException("Token expired");
        }
        System.out.println("✅ 토큰 만료 확인 통과");

        // 모두 정확할 경우 userId를 String으로 반환
        String userId = jwtUtil.getUserIdFromToken(token);
        System.out.println("✅ AuthHelper 완료 - 반환할 userId: " + userId);
        return userId;
    }

    public UUID authHelperAsUUID(String authHeader) {
        System.out.println("🔍 AuthHelper.authHelperAsUUID 시작");

        try {
            String userId = authHelper(authHeader);
            System.out.println("authHelper에서 받은 userId: '" + userId + "'");

            if (userId == null || userId.trim().isEmpty()) {
                System.out.println("❌ userId가 null 또는 빈 문자열");
                throw new IllegalStateException("Invalid or expired token");
            }

            // UUID 변환 시도
            System.out.println("UUID 변환 시도: '" + userId + "'");
            UUID uuid = UUID.fromString(userId.trim());  // ✅ trim() 추가
            System.out.println("✅ UUID 변환 성공: " + uuid);

            return uuid;

        } catch (IllegalArgumentException e) {
            System.out.println("❌ UUID 변환 실패: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Invalid UUID format in token");
        } catch (Exception e) {
            System.out.println("❌ 전체 처리 실패: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Invalid or expired token");
        }
    }
}