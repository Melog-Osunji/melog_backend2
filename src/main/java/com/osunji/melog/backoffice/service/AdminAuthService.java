package com.osunji.melog.backoffice.service;

import com.osunji.melog.backoffice.dto.AdminLoginRequest;
import com.osunji.melog.backoffice.entity.AdminAccount;
import com.osunji.melog.backoffice.repository.AdminAccountRepository;
import com.osunji.melog.global.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final JWTUtil jwtUtil;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public Map<String, Object> login(AdminLoginRequest request) {
        AdminAccount admin = adminAccountRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자 계정입니다."));

        if (!bCryptPasswordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtil.createAccessToken(admin.getId().toString(), 1000L * 60 * 60 * 2);

        return Map.of(
                "accessToken", accessToken,
                "adminId", admin.getId().toString(),
                "loginId", admin.getLoginId()
        );
    }
}
