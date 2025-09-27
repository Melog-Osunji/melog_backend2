package com.osunji.melog.inquirySettings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryResponse {
    private UUID id;               // 저장된 엔티티의 ID
    private UUID userId;           // 저장한 사용자 ID
    private LocalDateTime createdAt; // 저장 시각
}