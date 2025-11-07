package com.osunji.melog.global.security;

import org.springframework.stereotype.Component;

@Component
public class WhitelistPaths {

    public static final String[] AUTH_WHITELIST = {
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
            "/api/youtube/*",
            "/api/secretMelog/notices0128/**",
            "/actuator/health",
            "/actuator"

    };

    public String[] getAuthWhitelist() {
        return AUTH_WHITELIST;
    }
}
