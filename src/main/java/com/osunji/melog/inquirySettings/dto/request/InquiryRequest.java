package com.osunji.melog.inquirySettings.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryRequest {
    private String parentType;
    private String childType;
    private String title;
    private String content;
}
