package com.osunji.melog.user.controller;

import com.osunji.melog.user.dto.OauthLoginRequestDTO;
import com.osunji.melog.user.dto.OauthLoginResponseDTO;
import com.osunji.melog.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final AuthService authService;

    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLoginUser(
            @RequestBody OauthLoginRequestDTO request){
        OauthLoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }





}
