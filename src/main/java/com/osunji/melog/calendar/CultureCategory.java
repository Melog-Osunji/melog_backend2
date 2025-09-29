package com.osunji.melog.calendar;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
@Getter
@RequiredArgsConstructor
public enum CultureCategory {
    THEATER("연극"), MUSICAL("뮤지컬"), OPERA("오페라"),
    MUSIC("음악"), CONCERT("콘서트"), GUGAK("국악"),
    DANCE("무용"), EXHIBITION("전시"), ETC("기타"), ALL("전체");

    private final String label;

    /** CNV_060의 dtype값(한글 그대로) */
    public Optional<String> dtype() {
        return this == ALL ? Optional.empty() : Optional.of(label);
    }
}
