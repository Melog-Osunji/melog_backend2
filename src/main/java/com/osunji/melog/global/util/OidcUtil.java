package com.osunji.melog.global.util;

import org.springframework.stereotype.Component;

@Component
public class OidcUtil {
    /**
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
}
