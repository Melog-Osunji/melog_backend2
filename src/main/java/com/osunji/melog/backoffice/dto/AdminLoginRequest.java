package com.osunji.melog.backoffice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminLoginRequest {
    @NotBlank
    private String loginId;
    @NotBlank
    private String password;
}
