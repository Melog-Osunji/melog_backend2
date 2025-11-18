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
import java.util.Objects;

@Component
public class NaverOidcUtil {

    private static final Logger log = LoggerFactory.getLogger(NaverOidcUtil.class);

    private final String naverIssuer;
    private final String naverClientId;
    private final JWKSource<SecurityContext> naverJwkSource;
    private final ConfigurableJWTProcessor<SecurityContext> naverJwtProcessor;

    public NaverOidcUtil(
            @Qualifier("naverJwkSource") JWKSource<SecurityContext> naverJwkSource,
            @Value("${oidc.providers.naver.issuer}") String naverIssuer,
            @Value("${oidc.providers.naver.client-id}") String naverClientId
    ) {
        this.naverIssuer = naverIssuer;
        this.naverClientId = naverClientId;
        this.naverJwkSource = naverJwkSource;

        log.info("🔧 Initializing naverOidcUtil with issuer={}, clientId={}", naverIssuer, naverClientId);

        // Nimbus Processor 1회 구성 후 재사용
        var proc = new DefaultJWTProcessor<SecurityContext>();
        proc.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, naverJwkSource));
        this.naverJwtProcessor = proc;
    }

    public JWTClaimsSet verifyNaverIdToken(String idToken)
            throws ParseException, JOSEException, BadJOSEException {

        log.debug("🟡 Start verifying naver ID Token...");

        var header = SignedJWT.parse(idToken).getHeader();
        log.debug("🧩 Parsed JWT header: alg={}, kid={}", header.getAlgorithm(), header.getKeyID());

        if (!JWSAlgorithm.RS256.equals(header.getAlgorithm())) {
            log.error("❌ Unsupported algorithm: {}", header.getAlgorithm());
            throw new IllegalArgumentException("Unsupported alg: " + header.getAlgorithm());
        }
        if (header.getKeyID() == null || header.getKeyID().isBlank()) {
            log.error("❌ Missing kid in JWT header");
            throw new IllegalArgumentException("Missing kid in JWT header");
        }

        log.debug("✅ Header validation passed. Processing JWT with Nimbus...");

        JWTClaimsSet claims = naverJwtProcessor.process(idToken, null);
        log.info("✅ Successfully processed JWT. Subject={}", claims.getSubject());

        // 표준 OIDC 수동 검증
        if (!Objects.equals(naverIssuer, claims.getIssuer())) {
            log.error("❌ Invalid issuer: {}", claims.getIssuer());
            throw new BadJWTException("Invalid iss");
        }
        var aud = claims.getAudience();
        if (aud == null || aud.stream().noneMatch(naverClientId::equals)) {
            log.error("❌ Invalid audience: {}", aud);
            throw new BadJWTException("Invalid aud");
        }

        var exp = claims.getExpirationTime();
        log.debug("🕒 Token expiration time: {}", exp);
        if (exp == null || exp.toInstant().isBefore(Instant.now().minusSeconds(60))) {
            log.error("❌ Expired id_token (exp={})", exp);
            throw new BadJWTException("Expired id_token");
        }

        var azp = (String) claims.getClaim("azp");
        if (azp != null && !naverClientId.equals(azp)) {
            log.error("❌ Invalid azp: {}", azp);
            throw new BadJWTException("Invalid azp");
        }

        if (claims.getSubject() == null) {
            log.error("❌ Missing subject (sub)");
            throw new BadJWTException("Missing sub");
        }

        if (claims.getIssueTime() != null &&
                claims.getIssueTime().toInstant().isAfter(Instant.now().plusSeconds(60))) {
            log.error("❌ Invalid iat (future issue time): {}", claims.getIssueTime());
            throw new BadJWTException("Invalid iat (future)");
        }

        log.info("✅ OIDC validation complete for user sub={}", claims.getSubject());
        return claims;
    }
}
