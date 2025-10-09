package com.osunji.melog.global.security;

import com.osunji.melog.global.util.JWTUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTR = "USER_ID";

    private final JWTUtil jwtUtil;

    public JwtAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        AntPathMatcher m = new AntPathMatcher();
        String[] skip = {

            "/api/auth/oidc/start",        // /auth → /api/auth 수정
            "/api/auth/oidc/callback",     // /auth → /api/auth 수정
            "/api/auth/refresh",           // /auth → /api/auth 수정
            "/api/auth/logout",            // /auth → /api/auth 수정
            "/health",
            "/api/dev/**",
            "/docs/**",
                "/secure/ping",
                "/api/auth/login/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            "/api/youtube/*",

            "/api/secretMelog/notices0128/**"

        };
        for (String p : skip) if (m.match(p, uri)) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 이미 인증되어 있으면 통과하며, USER_ID_ATTR도 보정
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)) {
            if (existing.getPrincipal() instanceof UUID uuid) {
                req.setAttribute(USER_ID_ATTR, uuid);
            } else if (existing.getName() != null) {
                // 이름(문자열)만 있는 경우 보정 시도
                try { req.setAttribute(USER_ID_ATTR, UUID.fromString(existing.getName())); } catch (IllegalArgumentException ignored) {}
            }
            chain.doFilter(req, res);
            return;
        }

        // 토큰 추출(헤더 → 쿠키 → 쿼리파라미터[개발용])
        String token = extractAccessToken(req);
        if (token == null) {
            chain.doFilter(req, res);
            return;
        }

        try {
            jwtUtil.validateAccess(token); // 만료/서명/iss/aud 등 검증
            String userIdStr = jwtUtil.getUserIdFromAccess(token);

            // ★ UUID로 확정
            UUID userId = UUID.fromString(userIdStr);

            var authorities = Collections.<SimpleGrantedAuthority>emptyList();
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 요청 스코프 attr도 UUID로
            req.setAttribute(USER_ID_ATTR, userId);

            chain.doFilter(req, res);

        } catch (JwtException | IllegalArgumentException e) { // 잘못된 토큰/UUID 포맷
            SecurityContextHolder.clearContext();
            res.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setCharacterEncoding("UTF-8");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Invalid or expired token\"}");
        }
    }

    private String extractAccessToken(HttpServletRequest req) {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);

        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("access".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        String qp = req.getParameter("accessToken"); // 개발 편의
        if (qp != null && !qp.isBlank()) return qp;

        return null;
    }



//    /** roles 클레임을 권한으로 매핑(없어도 무방) */
//    @SuppressWarnings("unchecked")
//    private Collection<SimpleGrantedAuthority> extractAuthorities(String token) {
//        if (raw == null) return Collections.emptyList();
//
//        if (raw instanceof Collection<?>) {
//            return ((Collection<?>) raw).stream()
//                    .map(String::valueOf)
//                    .filter(s -> !s.isBlank())
//                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
//                    .collect(Collectors.toList());
//        }
//        // 문자열로 들어온 경우: "USER,ADMIN"
//        String s = String.valueOf(raw);
//        if (s.isBlank()) return Collections.emptyList();
//        return Arrays.stream(s.split(","))
//                .map(String::trim)
//                .filter(t -> !t.isEmpty())
//                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
//                .collect(Collectors.toList());
//    }
}
