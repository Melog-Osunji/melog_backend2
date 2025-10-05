package com.osunji.melog.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDTO {
    private String sub;       // OIDC 고유 사용자 식별자
    private String email;     // 사용자 이메일
    private String name;      // 사용자 이름 또는 닉네임
    private String picture;   // 프로필 이미지 URL
}
