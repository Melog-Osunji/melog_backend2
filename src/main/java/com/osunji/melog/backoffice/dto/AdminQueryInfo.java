package com.osunji.melog.backoffice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminQueryInfo {
    private String criterion;
    private String keyword;
    private String category;
    private String fromDate;
    private String toDate;
}
