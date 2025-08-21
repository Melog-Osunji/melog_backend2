package com.osunji.melog.global.security;

import com.osunji.melog.global.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JwtAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS 프리플라이트
        // 화이트리스트는 필터 스킵
        return uri.startsWith("/auth/") || uri.startsWith("/health") || uri.startsWith("/docs/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 이미 인증이 있는 경우(다른 필터가 세팅) 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        try {
            // 1) 서명/만료 검증 (액세스 토큰 기준)
            jwtUtil.validateAccess(token);

            // 2) 클레임에서 userId 꺼내 인증 객체 생성
            String userId = jwtUtil.getUserIdFromAccess(token);
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,                      // principal: 간단히 userId 사용
                    null,                        // credentials
                    Collections.emptyList()      // 권한 없음(필요 시 JWT에 roles 추가)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(req, res);

        } catch (Exception e) {
            // 위조/만료/형식오류 등 → 401
            SecurityContextHolder.clearContext();
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
