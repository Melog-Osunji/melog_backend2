package com.osunji.melog.backoffice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class AdminHarmonyDeleteRequest {
    private UUID harmonyRoomId;
    private String defaultTitle;
    private String defaultContent;
    private String managerName;
    private String status;
}
