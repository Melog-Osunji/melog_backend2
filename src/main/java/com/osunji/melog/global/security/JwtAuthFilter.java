package com.osunji.melog.global.security;

import com.osunji.melog.global.util.JWTUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTR = "USER_ID";
    private final JWTUtil jwtUtil;

    public JwtAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        final String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("🔕 shouldNotFilter = true (OPTIONS) {} {}", method, uri);
            return true;
        }

        AntPathMatcher m = new AntPathMatcher();
        String[] skip = {
                "/api/auth/oidc/start",
                "/api/auth/oidc/callback",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/health",
                "/api/dev/**",
                "/docs/**",
                "/secure/ping",
                "/api/auth/login/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api/dev/token",
                "/secure/ping",
                "/api/youtube/*",
//                "/api/**",
//                "/api/*",
                "/api/secretMelog/notices0128/**"
        };
        for (String p : skip) {
            if (m.match(p, uri)) {
                log.debug("🔕 shouldNotFilter = true (matched `{}`) {} {}", p, method, uri);
                return true;
            }
        }
        log.debug("✅ shouldNotFilter = false {} {}", method, uri);
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {

        final String uri = req.getRequestURI();
        final String method = req.getMethod();
        log.debug("➡️ JwtAuthFilter in: {} {}", method, uri);

        final String uri = req.getRequestURI();
        final String method = req.getMethod();
        log.debug("➡️ JwtAuthFilter in: {} {}", method, uri);

        // 이미 인증되어 있으면 통과하며, USER_ID_ATTR도 보정
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)) {
            log.debug("ℹ️ existing Authentication found: principal={}, name={}",
                    existing.getPrincipal(), existing.getName());
            if (existing.getPrincipal() instanceof UUID uuid) {
                req.setAttribute(USER_ID_ATTR, uuid);
                log.debug("🆔 setAttribute(USER_ID_ATTR) from principal={}", uuid);
            } else if (existing.getName() != null) {
                try {
                    UUID parsed = UUID.fromString(existing.getName());
                    req.setAttribute(USER_ID_ATTR, parsed);
                    log.debug("🆔 setAttribute(USER_ID_ATTR) from name={}", parsed);
                } catch (IllegalArgumentException ignored) {
                    log.debug("⚠️ existing name is not UUID format: {}", existing.getName());
                }
            }
            chain.doFilter(req, res);
            log.debug("⬅️ JwtAuthFilter out (existing auth) {} {}", method, uri);
            return;
        }

        // 토큰 추출(헤더 → 쿠키 → 쿼리파라미터[개발용])
        String token = extractAccessToken(req);
        log.debug("🔎 extractAccessToken: {}", (token != null ? "present" : "null"));
        if (token == null) {
            chain.doFilter(req, res);
            log.debug("⬅️ JwtAuthFilter out (no token) {} {}", method, uri);
            return;
        }

        try {
            jwtUtil.validateAccess(token); // 만료/서명/iss/aud 등 검증
            String userIdStr = jwtUtil.getUserIdFromAccess(token);
            log.debug("✅ token validated, userIdStr={}", userIdStr);

            UUID userId = UUID.fromString(userIdStr); // UUID 확정
            var authorities = Collections.<SimpleGrantedAuthority>emptyList();
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);

            req.setAttribute(USER_ID_ATTR, userId);
            log.debug("🆔 setAttribute(USER_ID_ATTR)={}", userId);

            chain.doFilter(req, res);
            log.debug("⬅️ JwtAuthFilter out (auth set) {} {}", method, uri);

        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.warn("❌ JWT invalid: {} ({})", e.getMessage(), e.getClass().getSimpleName());
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

