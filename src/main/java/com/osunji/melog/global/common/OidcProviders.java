package com.osunji.melog.global.common;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "oidc")
public class OidcProviders {

    private Map<String, Provider> providers;

    public Provider get(String key) {
        return providers.get(key);
    }

    public Map<String, Provider> getProviders() { return providers; }
    public void setProviders(Map<String, Provider> providers) { this.providers = providers; }

    public static class Provider {
        private String issuer;
        private String clientId;
        private String tokenEndpoint;
        private String jwksUri;
        private String redirectUri;
        private long clockSkewSec = 120;

        public String issuer() { return issuer; }
        public String clientId() { return clientId; }
        public String tokenEndpoint() { return tokenEndpoint; }
        public String jwksUri() { return jwksUri; }
        public String redirectUri() { return redirectUri; }
        public long clockSkewSec() { return clockSkewSec; }

        public void setIssuer(String issuer) { this.issuer = issuer; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }
        public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public void setClockSkewSec(long clockSkewSec) { this.clockSkewSec = clockSkewSec; }
    }
}
