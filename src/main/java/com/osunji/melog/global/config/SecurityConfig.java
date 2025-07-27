package com.osunji.melog.global.config;

import com.osunji.melog.global.security.JWTUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


// Spring Security 설정
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JWTUtil jwtUtil;

    public SecurityConfig(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // csrf 설정 off
                .csrf(csrf -> csrf.disable())
                // formLogin 설정 off (id와 pw 입력 후 회원가입 하는 부분)
                .formLogin(formLogin -> formLogin.disable())
                // httpBasic 설정 off
                .httpBasic(httpBasic -> httpBasic.disable())
                // uri 경로별 인증/미인증 지정
                .authorizeHttpRequests(auth-> auth
                        .requestMatchers("/**").permitAll()
//                        .requestMatchers("/getToken").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // custum filter
//                .addFilterAt(new )
                // session STATELESS 상태로 설정 (JWT 사용)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));



        return http.build();


    }

}
