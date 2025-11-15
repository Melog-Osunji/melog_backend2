package com.osunji.melog.calendar.controller;

import com.osunji.melog.calendar.CultureCategory;
import com.osunji.melog.calendar.dto.CalendarResponse;
import com.osunji.melog.calendar.service.CalendarItemProvider;
import com.osunji.melog.calendar.service.CalendarService;
import com.osunji.melog.calendar.service.CultureOpenApiClient;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final CultureOpenApiClient cultureOpenApiClient;
    private final CalendarItemProvider calendarItemProvider;

    // 캘린더 메인 화면
    @GetMapping("/main")
    public ResponseEntity<?> calendarMain(
            @RequestAttribute(JwtAuthFilter.USER_ID_ATTR) UUID userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        log.debug("📅 [GET /api/calendar/main] called with userId={}, year={}, month={}", userId, year, month);

        // year만 또는 month만 들어오면 400
        if ((year == null) ^ (month == null)) {
            log.warn("⚠️ year 또는 month 중 하나만 전달됨 → 400 반환");
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body("year과 month는 동시에 존재하거나 동시에 없어야 합니다.");
        }

        log.debug("🔍 CalendarService.calendarMain() 호출 시작");
        ApiMessage<CalendarResponse> result = calendarService.calendarMain(userId, year, month);
        log.debug("📅 [GET /api/calendar/main] called with userId={}, year={}, month={}", userId, year, month);
        return ResponseEntity.ok(result);
    }

    // 외부 공연 아이템 조회
    @GetMapping("/items")
    public ResponseEntity<?> getItems(@RequestParam String category) {
        log.debug("🎭 [GET /api/calendar/items] called with category='{}'", category);

        CultureCategory cat = parseCategory(category);
        if (cat == null) {
            log.warn("⚠️ 잘못된 category 요청 → '{}'", category);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body("잘못된 category 값입니다.");
        }

        log.debug("🔍 CultureOpenWebClient.fetchItems() 호출 시작: category={}", cat);
        List<CalendarResponse.Item> items = calendarItemProvider.getItems(cat);
        log.debug("✅ fetchItems() 완료: count={}", items.size());

        // 응답 아이템의 category 필수 검사
        boolean hasMissingCategory = items.stream()
                .anyMatch(it -> !StringUtils.hasText(it.getCategory()));
        if (hasMissingCategory) {
            log.error("🚨 응답 데이터에 category 값이 누락됨 → 500 반환");
            return ResponseEntity.status(500)
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body("응답 데이터에 category 값이 누락되었습니다.");
        }

        log.debug("📦 외부 공연 목록 정상 응답 → size={}, category={}", items.size(), cat);
        return ResponseEntity.ok(ApiMessage.success(200, "외부 공연 목록", items));
    }

    /**
     * 카테고리 파싱:
     * - 영어 enum 이름 (대소문자 무시): MUSIC, THEATER, MUSICAL, ...
     * - 한글 라벨: "음악", "연극", ...
     */
    private CultureCategory parseCategory(String raw) {
        if (!StringUtils.hasText(raw)) {
            log.warn("⚠️ parseCategory(): 입력값이 비어있음");
            return null;
        }

        String s = raw.trim();
        log.trace("🔎 parseCategory() 시도: raw='{}'", s);

        // 1) 영어 enum 이름 시도
        try {
            CultureCategory cat = CultureCategory.valueOf(s.toUpperCase());
            log.trace("✅ 영어 enum 이름으로 매칭됨: {}", cat);
            return cat;
        } catch (IllegalArgumentException ignore) {
            // 무시 후 한글 라벨 매칭 시도
        }

        // 2) 한글 라벨 매칭
        for (CultureCategory cc : CultureCategory.values()) {
            if (cc.getLabel().equals(s)) {
                log.trace("✅ 한글 라벨 매칭 성공: {}", cc);
                return cc;
            }
        }

        log.warn("❌ parseCategory 실패: '{}'는 유효하지 않은 category", s);
        return null;
    }
}


