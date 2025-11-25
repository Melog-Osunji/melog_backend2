package com.osunji.melog.calendar.service;

import com.osunji.melog.calendar.domain.EventSchedule;
import com.osunji.melog.calendar.dto.CalendarResponse;
import com.osunji.melog.calendar.repository.EventScheduleRepository;
import com.osunji.melog.global.dto.ApiMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final EventScheduleRepository eventScheduleRepository;

    private static final DayOfWeek WEEK_START = DayOfWeek.SATURDAY;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final CultureOpenApiClient cultureOpenApiClient;

    public ApiMessage<CalendarResponse> calendarMain(UUID userId, Integer year, Integer month) {
        log.debug("📅 [calendarMain] called with userId={}, year={}, month={}", userId, year, month);

        // year/month 없으면 오늘 기준
        LocalDate today = LocalDate.now(KST);
        int y = (year != null) ? year : today.getYear();
        int m = (month != null) ? month : today.getMonthValue();
        log.debug("🕒 기준 연월 결정: year={}, month={}", y, m);

        // 1) 달력 그리드 from/to 계산
        LocalDate firstOfMonth = LocalDate.of(y, m, 1);
        LocalDate lastOfMonth  = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
        DayOfWeek weekEnd = DayOfWeek.of(((WEEK_START.getValue() + 5) % 7) + 1); // start+6

        LocalDate fromDate = firstOfMonth.with(TemporalAdjusters.previousOrSame(WEEK_START));
        LocalDate toDate   = lastOfMonth.with(TemporalAdjusters.nextOrSame(weekEnd));
        log.debug("🗓️ 달력 기간 계산: fromDate={}, toDate(beforeAdjust)={}", fromDate, toDate);

        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days < 35) {
            toDate = fromDate.plusDays(34);
            log.debug("⚙️ 최소 35일 보정 적용됨 → toDate(afterAdjust)={}", toDate);
        }

        // 2) 내 일정 조회 (from/to와 겹치는 것만)
        List<EventSchedule> schedules =
                eventScheduleRepository.findByUser_IdAndEventDateBetween(userId, fromDate, toDate);
        log.debug("📘 조회된 일정 개수 = {}", schedules.size());

        // 3) items 매핑
        List<CalendarResponse.Item> items = schedules.stream()
                .map(this::toItem)
                .toList();
        log.debug("🧩 매핑된 CalendarResponse.Item 개수 = {}", items.size());

        // 3-1) 날짜별 event 집계 (유저가 실제로 저장한 날짜만 표시)
        Map<LocalDate, List<UUID>> eventsByDate = new HashMap<>();
        for (EventSchedule es : schedules) {
            LocalDate d = es.getEventDate();

            // 혹시라도 범위를 벗어나는 데이터 방어적 체크
            if (d.isBefore(fromDate) || d.isAfter(toDate)) {
                continue;
            }

            eventsByDate
                    .computeIfAbsent(d, __ -> new ArrayList<>())
                    .add(es.getId());   // scheduleId
        }

        log.debug("📆 날짜별 이벤트 집계 완료: 총 {}일에 이벤트 존재", eventsByDate.size());

        // 4) 그리드 생성
        List<List<CalendarResponse.Day>> weeks = buildWeeksGrid(fromDate, toDate, eventsByDate);
        log.debug("🧱 달력 주차 수 = {}", weeks.size());

//        List<CalendarResponse.Item> CNV060Items = cultureOpenApiService.fetchItems(ALL);
        log.debug("✅ fetchItems() 완료: count={}", items.size());

        CalendarResponse body = CalendarResponse.builder()
                .meta(CalendarResponse.Meta.builder()
                        .year(y)
                        .month(m)
                        .page(0)
                        .size(items.size())
                        .alarm(false)
                        .build())
                .calendar(CalendarResponse.Calendar.builder()
                        .weeks(weeks)
                        .build())
                .schedule(items)
//                .items(CNV060Items)
                .build();

        log.debug("✅ CalendarResponse 생성 완료 (items={}, weeks={})", items.size(), weeks.size());
        return ApiMessage.success(200, "캘린더 전송 성공", body);
    }

    private List<List<CalendarResponse.Day>> buildWeeksGrid(
            LocalDate fromDate, LocalDate toDate, Map<LocalDate, List<UUID>> eventsByDate
    ) {
        log.debug("📅 [buildWeeksGrid] fromDate={}, toDate={}", fromDate, toDate);
        List<List<CalendarResponse.Day>> weeks = new ArrayList<>();
        LocalDate cursor = fromDate;

        while (!cursor.isAfter(toDate)) {
            List<CalendarResponse.Day> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                LocalDate cur = cursor.plusDays(i);
                List<UUID> ids = eventsByDate.getOrDefault(cur, Collections.emptyList());
                boolean hasEvent = !ids.isEmpty();
                week.add(CalendarResponse.Day.builder()
                        .date(cur.toString())
                        .event(hasEvent)
                        .eventList(hasEvent ? List.copyOf(ids) : Collections.emptyList())
                        .build());
            }
            log.trace("🧭 주차 데이터 추가: 시작일={}", cursor);
            weeks.add(week);
            cursor = cursor.plusWeeks(1);
        }
        log.debug("📋 buildWeeksGrid 완료: 총 {}주 생성", weeks.size());
        return weeks;
    }

    private CalendarResponse.Item toItem(EventSchedule es) {
        var c = es.getCalendar();
        LocalDate start = c.getStartDate();
        LocalDate end   = (c.getEndDate() != null) ? c.getEndDate() : start;

        int dDay = (start != null)
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(KST), start)
                : 0;

        log.trace("🗂️ toItem(): id={}, title={}, start={}, end={}, dDay={}",
                es.getId(), c.getTitle(), start, end, dDay);

        return CalendarResponse.Item.builder()
                .id(es.getId())
                .title(c.getTitle())
                .category(c.getClassification())
                .thumbnailUrl(null)
                .venue(c.getRegion())
                .startDateTime(start != null ? start.atStartOfDay().atOffset(ZoneOffset.ofHours(9)) : null)
                .endDateTime(end   != null ? end  .atStartOfDay().atOffset(ZoneOffset.ofHours(9))   : null)
                .dDay(dDay)
                .bookmarked(false)
                .build();
    }


}
