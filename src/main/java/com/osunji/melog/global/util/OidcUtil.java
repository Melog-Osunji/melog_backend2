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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.Objects;

@Component
public class OidcUtil {

    private final String kakaoIssuer;
    private final String kakaoClientId;
    private final JWKSource<SecurityContext> kakaoJwkSource;
    private final ConfigurableJWTProcessor<SecurityContext> kakaoJwtProcessor;

    public OidcUtil(
            @Qualifier("kakaoJwkSource") JWKSource<SecurityContext> kakaoJwkSource,
            @Value("${oidc.providers.kakao.issuer}") String kakaoIssuer,
            @Value("${oidc.providers.kakao.client-id}") String kakaoClientId
    ) {
        this.kakaoIssuer = kakaoIssuer;
        this.kakaoClientId = kakaoClientId;
        this.kakaoJwkSource = kakaoJwkSource;

        // Nimbus Processor 1회 구성 후 재사용
        var proc = new DefaultJWTProcessor<SecurityContext>();
        proc.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, kakaoJwkSource));
        this.kakaoJwtProcessor = proc;
    }

    public JWTClaimsSet verifyKakaoIdToken(String idToken)
            throws ParseException, JOSEException, BadJOSEException {

        var header = SignedJWT.parse(idToken).getHeader();
        if (!JWSAlgorithm.RS256.equals(header.getAlgorithm())) {
            throw new IllegalArgumentException("Unsupported alg: " + header.getAlgorithm());
        }
        if (header.getKeyID() == null || header.getKeyID().isBlank()) {
            throw new IllegalArgumentException("Missing kid in JWT header");
        }

        // ✅ 올바른 사용: kakaoJwkSource를 쓰는 Processor
        JWTClaimsSet claims = kakaoJwtProcessor.process(idToken, null);

        // 표준 OIDC 수동 검증
        if (!Objects.equals(kakaoIssuer, claims.getIssuer())) {
            throw new BadJWTException("Invalid iss");
        }
        var aud = claims.getAudience();
        if (aud == null || aud.stream().noneMatch(kakaoClientId::equals)) {
            throw new BadJWTException("Invalid aud");
        }
        var exp = claims.getExpirationTime();
        if (exp == null || exp.toInstant().isBefore(Instant.now().minusSeconds(60))) { // 60s clock skew 허용
            throw new BadJWTException("Expired id_token");
        }
        var azp = (String) claims.getClaim("azp");
        if (azp != null && !kakaoClientId.equals(azp)) {
            throw new BadJWTException("Invalid azp");
        }
        if (claims.getSubject() == null) {
            throw new BadJWTException("Missing sub");
        }
        if (claims.getIssueTime() != null &&
                claims.getIssueTime().toInstant().isAfter(Instant.now().plusSeconds(60))) {
            throw new BadJWTException("Invalid iat (future)");
        }
        return claims;
    }
}



    /** Oidc Service로 분리 (본 파일은 추후 삭제 예정)
     * 페이로드 검증
     * ID 토큰의 영역 구분자인 온점(.)을 기준으로 헤더, 페이로드, 서명을 분리
     * 페이로드를 Base64 방식으로 디코딩
     * 페이로드의 키별 값 검증
     * iss: https://kauth.kakao.com와 일치해야 함
     * aud: 서비스 앱 키와 일치해야 함
     * exp: 현재 UNIX 타임스탬프(Timestamp)보다 큰 값 필요(ID 토큰의 만료 여부 확인)
     * nonce: 카카오 로그인 요청 시 전달한 값과 일치해야 함
     **/

     /**
     * 서명 검증
     * ID 토큰의 영역 구분자인 온점(.)을 기준으로 헤더, 페이로드, 서명을 분리
     * 헤더를 Base64 방식으로 디코딩
     * OIDC: 공개키 목록 조회 API로 카카오 인증 서버가 서명 시 사용하는 공개키 목록 조회
     * 공개키 목록에서 헤더의 kid에 해당하는 공개키 값 확인
     * 공개키는 일정 기간 캐싱(Caching)하여 사용할 것을 권장하며, 지나치게 빈번한 요청 시 요청이 차단될 수 있으므로 유의
     * JWT 서명 검증을 지원하는 라이브러리를 사용해 공개키로 서명 검증
     * 참고: OpenID Foundation, jwt.io
     * 라이브러리를 사용하지 않고 직접 서명 검증 구현 시, RFC7515 규격에 따라 서명 검증 가능
     */

