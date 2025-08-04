package com.osunji.melog.user;

import lombok.Getter;
import lombok.Setter;

public class OauthLoginRequestDTO {

    @Getter
    @Setter
    public static class oauthLoginRequestDTOBuilder {
        private String ocidId;
        private String accessToken;
    }
}
