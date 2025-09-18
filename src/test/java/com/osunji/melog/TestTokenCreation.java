package com.osunji.melog;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestTokenCreation {

    // ★ 여기 2가지만 채워주세요 ★
    // ① curl에 사용했던 "그" JWT (access 토큰)
    private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI3Mjc5MTJiNy1jYTEyLTQ3OWUtODc1My1hNTViMGEyZjc0YWEiLCJpc3MiOiJtZWxvZy1hcGkiLCJhdWQiOlsibWVsb2ctY2xpZW50Il0sImp0aSI6IjVkZjViMGRmLTIzZWItNDRjNS04YjVhLTA0OTFkN2EwMTkyNSIsImlhdCI6MTc1ODA1NjM4NCwiZXhwIjoxNzU4MDU3Mjg0fQ.HIHkRsU0zNjc_tlUAOKUIFkTFpVpIdL1y0Pldf9MRp8";

    // ② 같은 환경에서 서버가 쓰는 시크릿(둘 다 넣어도 되고 하나만 넣어도 됨)
    //    - 평문 시크릿을 쓰는 경우: PLAIN_SECRET에 값 입력, B64_SECRET는 비워둬도 됨
    //    - Base64 시크릿을 쓰는 경우: B64_SECRET에 값 입력, PLAIN_SECRET는 비워둬도 됨
    private static final String PLAIN_SECRET = "gcp3fb4j4kv6d3qxkof1lz1f7mb7fmvqd56z76i009ogxv6gayr3t2e88c2cs2ny";
    private static final String B64_SECRET   = "";

    private static SecretKey keyFromPlain(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("plain secret empty");
        // HS256은 최소 32바이트(256bit) 권장
        return Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
    }

    private static SecretKey keyFromBase64(String b64) {
        if (b64 == null || b64.isBlank()) throw new IllegalArgumentException("base64 secret empty");
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(b64));
    }

    @Test
    void parseWithPlainSecret_shouldPass_ifServerUsesPlainSecret() {
        // 서버가 평문 시크릿을 쓴다면 이 테스트는 "통과"해야 함
        SecretKey secretKey = keyFromPlain(PLAIN_SECRET);
        assertDoesNotThrow(() ->
                Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(JWT)
        );
    }

    @Test
    void parseWithBase64Secret_shouldPass_ifServerUsesBase64Secret() {
        // 서버가 Base64 시크릿을 쓴다면 이 테스트는 "통과"해야 함
        SecretKey secretKey = keyFromBase64(B64_SECRET);
        assertDoesNotThrow(() ->
                Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(JWT)
        );
    }

    @Test
    void sanity_check_wrongKey_shouldFail() {
        // 반대로, 잘못된 키 방식이면 파싱이 실패해야 정상
        if (!PLAIN_SECRET.isBlank() && !B64_SECRET.isBlank()) {
            // 평문 키로 성공했다면 Base64 키로는 실패해야 하고, 그 반대도 동일
            SecretKey plain = keyFromPlain(PLAIN_SECRET);
            SecretKey b64   = keyFromBase64(B64_SECRET);

            boolean plainOk = canParse(plain);
            boolean b64Ok   = canParse(b64);

            // 정확히 하나만 성공해야 한다(둘 다 성공/둘 다 실패면 환경이 꼬여있는 것)
            if (plainOk && b64Ok) {
                throw new AssertionError("Both plain and base64 keys parse the token. 시크릿이 동일/동등값인지 점검하세요.");
            }
            if (!plainOk && !b64Ok) {
                throw new AssertionError("Neither key parses the token. 시크릿 값 자체가 다르거나 토큰이 다른 환경에서 생성되었습니다.");
            }
        }
    }

    private boolean canParse(SecretKey k) {
        try {
            Jwts.parser().verifyWith(k).build().parseSignedClaims(JWT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
