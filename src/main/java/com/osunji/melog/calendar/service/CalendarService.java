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

    public ApiMessage<CalendarResponse> calendarMain(UUID userId, Integer year, Integer month) {
        // year/month 없으면 오늘 기준
        LocalDate today = LocalDate.now(KST);
        int y = (year  != null) ? year  : today.getYear();
        int m = (month != null) ? month : today.getMonthValue();

        // 1) 달력 그리드 from/to 계산
        LocalDate firstOfMonth = LocalDate.of(y, m, 1);
        LocalDate lastOfMonth  = firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
        DayOfWeek weekEnd = DayOfWeek.of(((WEEK_START.getValue() + 5) % 7) + 1); // start+6

        LocalDate fromDate = firstOfMonth.with(TemporalAdjusters.previousOrSame(WEEK_START));
        LocalDate toDate   = lastOfMonth.with(TemporalAdjusters.nextOrSame(weekEnd));

        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days < 35) toDate = fromDate.plusDays(34);

        // 2) 내 일정 조회 (from/to와 겹치는 것만)
        List<EventSchedule> schedules =
                eventScheduleRepository.findByUser_IdAndEventDateBetween(userId, fromDate, toDate);

        // 3) items 매핑
        List<CalendarResponse.Item> items = schedules.stream()
                .map(this::toItem)
                .toList();

        // 3-1) 날짜별 event 집계
        Map<LocalDate, List<UUID>> eventsByDate = new HashMap<>();
        for (EventSchedule es : schedules) {
            var c = es.getCalendar();
            LocalDate start = c.getStartDate();
            LocalDate end   = (c.getEndDate() != null) ? c.getEndDate() : start;

            LocalDate s = (start.isBefore(fromDate)) ? fromDate : start;
            LocalDate e = (end.isAfter(toDate))     ? toDate   : end;

            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                eventsByDate.computeIfAbsent(d, __ -> new ArrayList<>()).add(es.getId());
            }
        }

        // 4) 그리드 생성
        List<List<CalendarResponse.Day>> weeks = buildWeeksGrid(fromDate, toDate, eventsByDate);

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
                .items(items)
                .build();

        return ApiMessage.success(200, "캘린더 전송 성공", body);
    }

    private List<List<CalendarResponse.Day>> buildWeeksGrid(
            LocalDate fromDate, LocalDate toDate, Map<LocalDate, List<UUID>> eventsByDate
    ) {
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
            weeks.add(week);
            cursor = cursor.plusWeeks(1);
        }
        return weeks;
    }

    private CalendarResponse.Item toItem(EventSchedule es) {
        var c = es.getCalendar();
        LocalDate start = c.getStartDate();
        LocalDate end   = (c.getEndDate() != null) ? c.getEndDate() : start;

        int dDay = (start != null)
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(KST), start)
                : 0;

        return CalendarResponse.Item.builder()
                .id(es.getId())
                .title(c.getTitle())
                .category(c.getClassification())
                .thumbnailUrl(null)            // 필요시 도메인 필드로 교체
                .venue(c.getRegion())          // region만 있는 경우 우선 사용
                .startDateTime(start != null ? start.atStartOfDay().atOffset(ZoneOffset.ofHours(9)) : null)
                .endDateTime(end   != null ? end  .atStartOfDay().atOffset(ZoneOffset.ofHours(9))   : null)
                .dDay(dDay)
                .bookmarked(false)             // 별도 주입 지점에서 true 처리
                .build();
    }
}
