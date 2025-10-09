package com.osunji.melog.calendar.controller;

import com.osunji.melog.calendar.CultureCategory;
import com.osunji.melog.calendar.dto.CalendarResponse;
import com.osunji.melog.calendar.service.CalendarService;
import com.osunji.melog.calendar.service.CultureOpenApiService;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final CultureOpenApiService cultureOpenApiService;

    // 캘린더 메인 화면
    @GetMapping("/main")
    public ResponseEntity<?> calendarMain(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        // year만 또는 month만 들어오면 400
        if ((year == null) ^ (month == null)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body("year과 month는 동시에 존재하거나 동시에 없어야 합니다.");
        }

        ApiMessage<CalendarResponse> result = calendarService.calendarMain(userId, year, month);
        return ResponseEntity.ok(result);
    }

    // 외부 공연 아이템 조회
    @GetMapping("/items")
    public ResponseEntity<?> getItems(@RequestParam String category) {
        CultureCategory cat = parseCategory(category);
        if (cat == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body("잘못된 category 값입니다.");
        }

        List<CalendarResponse.Item> items = cultureOpenApiService.fetchItems(cat);

        // 응답 아이템의 category 필수 검사 (빈 문자열/null이면 서버 에러로 간주)
        boolean hasMissingCategory = items.stream()
                .anyMatch(it -> !StringUtils.hasText(it.getCategory()));
        if (hasMissingCategory) {
            return ResponseEntity.status(500)
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body("응답 데이터에 category 값이 누락되었습니다.");
        }

        return ResponseEntity.ok(ApiMessage.success(200, "외부 공연 목록", items));
    }

    /**
     * 카테고리 파싱:
     * - 영어 enum 이름 (대소문자 무시): MUSIC, THEATER, MUSICAL, ...
     * - 한글 라벨: "음악", "연극", ...
     */
    private CultureCategory parseCategory(String raw) {
        if (!StringUtils.hasText(raw)) return null;

        String s = raw.trim();

        // 1) 영어 enum 이름 시도
        try {
            return CultureCategory.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignore) { }

        // 2) 한글 라벨 매칭
        for (CultureCategory cc : CultureCategory.values()) {
            if (cc.getLabel().equals(s)) {
                return cc;
            }
        }
        return null;
    }
}
