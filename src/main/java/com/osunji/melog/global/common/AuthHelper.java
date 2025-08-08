package com.osunji.melog.global.common;

import com.osunji.melog.global.util.JWTUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthHelper {
    private final JWTUtil jwtUtil;

    public String authHelper(String authHeader){

        // 헤더가 비었는지 확인
        if (authHeader == null || authHeader.isEmpty()) {
            throw new IllegalStateException("Empty Authorization header");
        }

        // 헤더가 Bearer로 시작하는지 확인
        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Invalid Authorization header");
        }

        // 토큰 앞의 "Bearer"을 제외하고, 토큰 부분만 token 변수에 할당
        String token = authHeader.substring("Bearer ".length());

        // 토큰의 만료 여부 확인
        if (jwtUtil.isTokenExpired(token)) {
            throw new IllegalStateException("Token expired");
        }

        // 모두 정확할 경우 userId를 String으로 반환
        return jwtUtil.getUserIdFromToken(token);
    }

}
