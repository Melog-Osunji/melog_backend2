package com.osunji.melog.user.controller;

import com.osunji.melog.user.OauthLoginRequestDTO;
import com.osunji.melog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final UserRepository userRepository;

    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLoginUser(
            @RequestBody OauthLoginRequestDTO request){
        
        return null;
    }


}
