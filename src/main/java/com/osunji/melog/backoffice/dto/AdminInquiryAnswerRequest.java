package com.osunji.melog.backoffice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminInquiryAnswerRequest {
    @NotBlank
    private String answer;
}
