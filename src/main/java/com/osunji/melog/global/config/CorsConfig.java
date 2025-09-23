package com.osunji.melog.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// 단순 연결을 위해 CORS 설정 OFF / 8월 7일 이후 다시 ON
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 경로에 대해
                        .allowedOrigins("*") // 모든 origin 허용
                        .allowedMethods("*") // 모든 HTTP 메서드 허용 (GET, POST, PUT 등)
                        .allowedHeaders("*") // 모든 헤더 허용
                        .allowCredentials(false); // 자격 증명 허용 안 함 (true면 allowedOrigins에 * 사용 불가)
            }
        };
    }
}
