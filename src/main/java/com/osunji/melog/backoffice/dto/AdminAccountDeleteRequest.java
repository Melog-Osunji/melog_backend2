package com.osunji.melog.backoffice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdminAccountDeleteRequest {

    @NotEmpty
    private List<AdminUser> users;

    @Getter
    @NoArgsConstructor
    public static class AdminUser {
        private String loginId;
        private String loginPw;
        private String loginNickName;
    }
}
