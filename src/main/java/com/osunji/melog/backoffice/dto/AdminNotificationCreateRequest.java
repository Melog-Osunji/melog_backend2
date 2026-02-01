package com.osunji.melog.backoffice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminNotificationCreateRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private String targetType;
}
