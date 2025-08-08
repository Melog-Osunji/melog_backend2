package com.osunji.melog.user.service;

import com.osunji.melog.user.UserRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.dto.OauthLoginRequestDTO;
import com.osunji.melog.user.dto.OauthLoginResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public OauthLoginResponseDTO login(OauthLoginRequestDTO request) {
        String oidc = request.getOcid();
        String accessToken = request.getAccessToken();

        // 1. OCID 유효성 검증


        // 2. 유저 회원가입 여부 확인 -> 미가입일시 DB 저장(orElseGet() 안 부분)
        User user = userRepository.findByOidc(oidc).orElseGet(
                ()->registerNewUser(oidc, accessToken));

        // 3. melog 토큰 발급

        // 4. 응답 생성
        return null;
    }

    // 회원가입만 하는 매서드
    private User registerNewUser(String oidc, String accessToken) {

        // 1. accessToken으로 유저 정보 조회

        // 2. oidc sub 파싱 후 유저 정보 유효성 검증(util로 따로 빼기)

        // 3. db에 저장

        // 4. User 객체 반환
        return null;

    }
}
