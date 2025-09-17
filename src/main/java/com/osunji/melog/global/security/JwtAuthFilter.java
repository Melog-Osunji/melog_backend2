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

    /** 컨트롤러/서비스에서 쉽게 꺼내 쓰도록 request attribute key 제공 */
    public static final String USER_ID_ATTR = "USER_ID";

    private final JWTUtil jwtUtil;

    public JwtAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // preflight

        AntPathMatcher m = new AntPathMatcher();
        String[] skip = {
                "/auth/oidc/start",
                "/auth/oidc/callback",
                "/auth/refresh",
                "/auth/logout",
                "/health",
                "/api/dev/**",      // 개발용 (있다면)
                "/docs/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
        };
        for (String p : skip) {
            if (m.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 이미 인증되어 있으면 그대로 통과(기존 동작 유지)
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)) {
            // 편의상 USER_ID_ATTR도 채워주기 (다른 필터가 먼저 인증했을 수도 있으니)
            if (existing.getName() != null) {
                req.setAttribute(USER_ID_ATTR, existing.getName());
            }
            chain.doFilter(req, res);
            return;
        }

        // 1) 토큰 추출 (헤더 → 쿠키 → (개발용) 쿼리파라미터)
        String token = extractAccessToken(req);
        if (token == null) {
            // 토큰 없으면 익명으로 통과. 최종 접근 권한은 SecurityConfig에서 제어됨.
            chain.doFilter(req, res);
            return;
        }

        try {
            // 2) 검증 + userId/roles 추출
            jwtUtil.validateAccess(token);
            String userId = jwtUtil.getUserIdFromAccess(token);

            // roles(optional) → 권한 매핑
//            Collection<SimpleGrantedAuthority> authorities = extractAuthorities(token);

            // 3) SecurityContext 세팅(기존 목적 유지)
            var authorities = Collections.<SimpleGrantedAuthority>emptyList();
// 또는 Collections.emptyList() 도 가능 (권한 검사는 안 하니까)

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null,authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
            // 4) 컨트롤러에서 헤더 없이도 userId를 사용할 수 있도록 attribute 세팅(요청 범위)
            req.setAttribute(USER_ID_ATTR, userId);

            chain.doFilter(req, res);

        } catch (JwtException e) {
            // 기존처럼 invalid_token 응답
            SecurityContextHolder.clearContext();
            res.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setCharacterEncoding("UTF-8");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Invalid or expired token\"}");
        }
    }

    /** Authorization 헤더 → access 쿠키 → (개발용) accessToken 쿼리파라미터 순으로 토큰을 추출 */
    private String extractAccessToken(HttpServletRequest req) {
        // 1) Authorization: Bearer ...
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 2) access 쿠키
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("access".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }

        // 3) (개발 편의) 쿼리 파라미터 ?accessToken=... — 운영에서는 꺼둘 것을 권장
        String qp = req.getParameter("accessToken");
        if (qp != null && !qp.isBlank()) {
            return qp;
        }

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
