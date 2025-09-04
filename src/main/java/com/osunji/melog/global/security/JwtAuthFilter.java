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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JwtAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // preflight

        // 공개 엔드포인트만 정확 매칭 (SecurityConfig.AUTH_WHITELIST와 동기화)
        return uri.equals("/auth/oidc/start")
                || uri.equals("/auth/oidc/callback")
                || uri.equals("/auth/refresh")
                || uri.equals("/auth/logout")
                || uri.equals("/health")
                || uri.startsWith("/docs/")
                || uri.startsWith("/v3/api-docs/")
                || uri.startsWith("**")
                || uri.startsWith("/swagger-ui/");
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

            var auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
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
