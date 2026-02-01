package com.osunji.melog.backoffice.service;

import com.osunji.melog.backoffice.dto.AdminPageResponse;
import com.osunji.melog.backoffice.dto.AdminQueryInfo;
import com.osunji.melog.calendar.domain.Calendar;
import com.osunji.melog.calendar.domain.EventSchedule;
import com.osunji.melog.calendar.repository.CalendarRepository;
import com.osunji.melog.calendar.repository.EventScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCalendarService {

    private final CalendarRepository calendarRepository;
    private final EventScheduleRepository eventScheduleRepository;

    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
            Map.entry("THEATER", "연극"),
            Map.entry("MUSICAL", "뮤지컬"),
            Map.entry("OPERA", "오페라"),
            Map.entry("MUSIC", "음악"),
            Map.entry("CONCERT", "콘서트"),
            Map.entry("GUGAK", "국악"),
            Map.entry("DANCE", "무용"),
            Map.entry("EXHIBITION", "전시"),
            Map.entry("ETC", "기타")
    );

    @Transactional(readOnly = true)
    public Map<String, Object> getCalendarEvents(String type, String fromDate, String toDate, int page) {
        int size = 10;
        PageRequest pageable = PageRequest.of(page, size);

        List<Calendar> allCalendars = calendarRepository.findAll();

        // Filter by category
        if (type != null && !type.isBlank() && !"ALL".equals(type)) {
            String classification = CATEGORY_MAP.get(type);
            if (classification != null) {
                allCalendars = allCalendars.stream()
                        .filter(c -> classification.equals(c.getClassification()))
                        .collect(Collectors.toList());
            }
        }

        // Filter by date range
        if (fromDate != null && toDate != null && !fromDate.isBlank() && !toDate.isBlank()) {
            LocalDate from = LocalDate.parse(fromDate);
            LocalDate to = LocalDate.parse(toDate);
            allCalendars = allCalendars.stream()
                    .filter(c -> {
                        LocalDate start = c.getStartDate();
                        LocalDate end = c.getEndDate() != null ? c.getEndDate() : start;
                        return !end.isBefore(from) && !start.isAfter(to);
                    })
                    .collect(Collectors.toList());
        }

        // Sort by start date
        allCalendars.sort(Comparator.comparing(Calendar::getStartDate));

        // Get savedCount per calendar (EventSchedule count)
        List<EventSchedule> allSchedules = eventScheduleRepository.findAll();
        Map<UUID, Long> savedCounts = allSchedules.stream()
                .collect(Collectors.groupingBy(
                        es -> es.getCalendar().getId(),
                        Collectors.counting()
                ));

        int totalElements = allCalendars.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Calendar> pagedCalendars = allCalendars.subList(fromIndex, toIndex);

        List<Map<String, Object>> events = pagedCalendars.stream()
                .map(c -> {
                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("eventId", c.getId().toString());
                    event.put("title", c.getTitle());
                    event.put("category", resolveCategory(c.getClassification()));
                    event.put("thumbnailUrl", c.getImageUrl());
                    event.put("venue", c.getRegion());
                    event.put("startDate", c.getStartDate().toString());
                    event.put("endDate", c.getEndDate() != null ? c.getEndDate().toString() : null);
                    event.put("savedCount", savedCounts.getOrDefault(c.getId(), 0L));
                    return event;
                })
                .collect(Collectors.toList());

        AdminPageResponse pageResponse = AdminPageResponse.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();

        AdminQueryInfo queryInfo = AdminQueryInfo.builder()
                .category(type != null ? type : "ALL")
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("events", events);
        result.put("page", pageResponse);
        result.put("query", queryInfo);
        return result;
    }

    private String resolveCategory(String classification) {
        if (classification == null) return "ETC";
        return CATEGORY_MAP.entrySet().stream()
                .filter(e -> e.getValue().equals(classification))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("ETC");
    }
}
