package com.osunji.melog.global.config;

import com.osunji.melog.global.security.JwtAuthFilter; // ← 네 필터 경로에 맞게
import com.osunji.melog.global.util.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

//    private final JWTUtil jwtUtil;
//    private final JwtAuthFilter jwtAuthFilter; // Bean 주입
//
//    public SecurityConfig(JWTUtil jwtUtil, JwtAuthFilter jwtAuthFilter) {
//        this.jwtUtil = jwtUtil;
//        this.jwtAuthFilter = jwtAuthFilter;
//    }

    // 컨테이너 자동 등록 방지(중요)
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false); // 톰캣 자동 등록 OFF
        return reg;
    }

    private static final String[] AUTH_WHITELIST = {
            "/auth/oidc/verify", "/auth/refresh", "/health", "/docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { // CORS가 필요하면 지정, 아니면 제거
                    // cors.configurationSource(corsConfigurationSource()); // 필요 시 주석 해제
                })
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) -> res.setStatus(HttpServletResponse.SC_FORBIDDEN))
                );

        // \ UsernamePasswordAuthenticationFilter "앞"
//        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 필요 시 CORS 소스
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        var config = new org.springframework.web.cors.CorsConfiguration();
//        config.setAllowCredentials(true);
//        config.addAllowedOriginPattern("https://example.com"); // RN/웹 도메인
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
}
