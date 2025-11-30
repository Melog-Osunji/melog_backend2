package com.osunji.melog.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResignationResponseDTO {
    private LocalDateTime deleteAt;        // 삭제일시 (ISO 형식)
}
