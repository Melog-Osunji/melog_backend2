package com.osunji.melog.global.config;

import com.osunji.melog.global.security.JwtAuthFilter;
import com.osunji.melog.global.util.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JWTUtil jwtUtil, JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // 서블릿 컨테이너 자동 등록 방지 (체인에만 수동 등록)
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        var reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    // 공개 엔드포인트 (JwtAuthFilter.shouldNotFilter와 1:1 동기화)
    private static final String[] AUTH_WHITELIST = {

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
            "/api/dev/token",
            "/secure/ping",
            "/api/youtube/*",
            "/api/secretMelog/notices0128/**"



    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setHeader("WWW-Authenticate", "Bearer");
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((req, res, e) -> res.setStatus(HttpServletResponse.SC_FORBIDDEN))
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    /**
     * 네이티브 앱이 쿠키(리프레시)를 사용하므로 credentials 허용 + 정확한 오리진만 화이트리스트.
     * 운영에서는 와일드카드 금지!
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(
                "https://app.melog.com",     // 실서비스 앱이 호출할 API 오리진
                "https://staging.melog.com", // 스테이징
                "http://10.0.2.2:8080",      // 필요 시 에뮬레이터/로컬 맵핑
                "http://localhost:3000"      // 개발용 (RN 디버그에서 프록시를 쓴다면)
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true); // ★ 쿠키 전송 허용 (반드시 정확한 오리진만)
        c.setExposedHeaders(List.of("Authorization")); // 필요 시
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}
