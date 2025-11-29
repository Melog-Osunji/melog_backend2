package com.osunji.melog.global.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class GoogleOidcUtil {

    private static final Logger log = LoggerFactory.getLogger(GoogleOidcUtil.class);

    private final String googleIssuer;
    private final String googleClientId;
    private final ConfigurableJWTProcessor<SecurityContext> googleJwtProcessor;

    public GoogleOidcUtil(
            @Qualifier("googleJwkSource") JWKSource<SecurityContext> googleJwkSource,
            @Value("${oidc.providers.google.issuer}") String googleIssuer,
            @Value("${oidc.providers.google.client-id}") String googleClientId
    ) {
        this.googleIssuer = googleIssuer;
        this.googleClientId = googleClientId;

        log.info("🔧 GoogleOidcUtil initialized.");
        log.info("     ▸ Expected issuer     : {}", googleIssuer);
        log.info("     ▸ Expected clientId   : {}", googleClientId);

        var proc = new DefaultJWTProcessor<SecurityContext>();
        proc.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, googleJwkSource));
        this.googleJwtProcessor = proc;
    }

    public JWTClaimsSet verifyGoogleIdToken(String idToken)
            throws ParseException, JOSEException, BadJOSEException {

        log.debug("🟡 Start verifying google ID Token...");

        // Header 로그
        var header = SignedJWT.parse(idToken).getHeader();
        log.debug("🧩 Header: alg={}, kid={}", header.getAlgorithm(), header.getKeyID());

        // alg check
        if (!JWSAlgorithm.RS256.equals(header.getAlgorithm())) {
            log.error("❌ Unsupported algorithm: {}", header.getAlgorithm());
            throw new IllegalArgumentException("Unsupported alg: " + header.getAlgorithm());
        }

        // kid check
        if (header.getKeyID() == null || header.getKeyID().isBlank()) {
            log.error("❌ Missing kid in JWT header");
            throw new IllegalArgumentException("Missing kid in JWT header");
        }

        // 서명 검증 및 기본 파싱
        JWTClaimsSet claims = googleJwtProcessor.process(idToken, null);
        log.info("✅ Signature valid. sub={}", claims.getSubject());

        // Issuer 검증
        if (!Objects.equals(googleIssuer, claims.getIssuer())) {
            log.error("❌ Invalid issuer: {}", claims.getIssuer());
            throw new BadJWTException("Invalid iss");
        }

        // ▣ 여기서부터 aud/azp 상세 디버깅
        List<String> audList = claims.getAudience();
        String azp = (String) claims.getClaim("azp");

        log.warn("🎯 Token Client Info (for debugging)");
        log.warn("   ▸ Token aud list : {}", audList);
        log.warn("   ▸ Token azp      : {}", azp);
        log.warn("   ▸ Expected clientId : {}", googleClientId);

        // aud 검증
        if (audList == null || audList.stream().noneMatch(googleClientId::equals)) {
            log.error("❌ Invalid audience.");
            log.error("   ▸ aud in token      : {}", audList);
            log.error("   ▸ expected clientId : {}", googleClientId);
            throw new BadJWTException("Invalid aud");
        }

        // azp 검증 (optional but recommended)
        if (azp != null && !googleClientId.equals(azp)) {
            log.error("❌ Invalid azp.");
            log.error("   ▸ azp in token      : {}", azp);
            log.error("   ▸ expected clientId : {}", googleClientId);
            throw new BadJWTException("Invalid azp");
        }

        // Expiration
        var exp = claims.getExpirationTime();
        if (exp == null || exp.toInstant().isBefore(Instant.now().minusSeconds(60))) {
            log.error("❌ Token expired (exp={})", exp);
            throw new BadJWTException("Expired id_token");
        }

        log.info("✅ OIDC validation complete. sub={}", claims.getSubject());
        return claims;
    }
}
