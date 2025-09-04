package com.osunji.melog.user.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.osunji.melog.global.common.OidcProviders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OidcService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final OidcProviders providers; // 아래 클래스 참고

    public OidcService(OidcProviders providers) {
        this.providers = providers;
    }

    /**
     * 1) code + code_verifier로 token 교환
     * 2) id_token 로컬 검증(JWK, iss/aud/azp/exp)
     * 3) claims 반환
     */
    public Map<String, Object> exchangeAndVerify(String providerKey, String code, String state, String codeVerifier) {
        OidcProviders.Provider cfg = providers.get(providerKey);
        if (cfg == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown_provider");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", cfg.clientId());
        form.add("redirect_uri", cfg.redirectUri());
        form.add("code_verifier", codeVerifier);

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                cfg.tokenEndpoint(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "oidc_exchange_failed");
        }

        Map<String, Object> tokenResp = resp.getBody();
        if (tokenResp == null || tokenResp.get("id_token") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no_id_token");
        }

        String idToken = (String) tokenResp.get("id_token");
        JWTClaimsSet claims = verifyIdToken(idToken, cfg);
        return claims.toJSONObject();
    }

    private JWTClaimsSet verifyIdToken(String idToken, OidcProviders.Provider cfg) {
        try {
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(cfg.jwksUri()));
            ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSAlgorithmFamilyJWSKeySelector<>(JWSAlgorithm.Family.RSA, jwkSource);
            processor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = processor.process(idToken, null);

            // iss
            if (!cfg.issuer().equals(claims.getIssuer())) {
                throw new IllegalArgumentException("invalid_iss");
            }

            // exp with skew
            Instant now = Instant.now();
            Instant exp = claims.getExpirationTime().toInstant();
            if (now.isAfter(exp.plusSeconds(cfg.clockSkewSec()))) {
                throw new IllegalArgumentException("expired_id_token");
            }

            // aud/azp
            List<String> aud = claims.getAudience();
            if (aud == null || aud.isEmpty() || !aud.contains(cfg.clientId())) {
                throw new IllegalArgumentException("invalid_aud");
            }
            String azp = claims.getStringClaim("azp");
            if (aud.size() > 1 && (azp == null || !cfg.clientId().equals(azp))) {
                throw new IllegalArgumentException("invalid_azp");
            }

            // (강력 권장) nonce 확인하려면 외부 저장소와 비교
            // String nonce = claims.getStringClaim("nonce");

            return claims;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_id_token");
        }
    }

    private static String url(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
