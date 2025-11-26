package com.osunji.melog.user.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleUserInfoResponse {
    private String id;
    private String email;
    private String name;        // 구글은 name 전체
    private String given_name;  // 이름
    private String family_name; // 성
    private String picture;
}
