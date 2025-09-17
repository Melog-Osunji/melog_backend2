package com.osunji.melog.global.security;

import com.osunji.melog.global.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JwtAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        final String method = request.getMethod();

        System.out.println("🔍 JwtAuthFilter.shouldNotFilter 체크");
        System.out.println("URI: " +
            uri);
        System.out.println("Method: " + method);

        if ("OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("✅ OPTIONS 요청 - 필터 스킵");
            return true;
        }
        var m = new org.springframework.util.AntPathMatcher();
        String[] skip = {
            "/auth/oidc/start",
            "/auth/oidc/callback",
            "/auth/refresh",
            "/auth/logout",
            "/api/auth/oidc/start",        // /auth → /api/auth 수정
            "/api/auth/oidc/callback",     // /auth → /api/auth 수정
            "/api/auth/refresh",           // /auth → /api/auth 수정
            "/api/auth/logout",            // /auth → /api/auth 수정
            "/health",
            "/api/dev/**",
            "/docs/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/posts/**",
            "/api/users/*/posts",
            "/api/posts/*/bookmarks",
            "/api/posts/*/comments/*",
            "/api/youtube/*",
            "/api/posts",
            "/api/posts/*/like",
            "/api/search/**",
            "/api/search",
            "/api/harmony/**",
            "/api/harmony"

        };
        for (String p : skip) {
            if (m.match(p, uri)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {

        var existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)) {
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
            jwtUtil.validateAccess(token);
            String userId = jwtUtil.getUserIdFromAccess(token);

            // 🎯 기본 ROLE_USER 권한 부여
            List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
            );

            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            res.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Invalid or expired token\"}");
        }
    }
}
