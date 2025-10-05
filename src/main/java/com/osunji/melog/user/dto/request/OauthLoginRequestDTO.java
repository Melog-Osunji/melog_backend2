package com.osunji.melog.user.dto.request;

import com.osunji.melog.user.domain.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OauthLoginRequestDTO {

    private String idToken;
    private String accessToken;
    private Platform platform;

}
