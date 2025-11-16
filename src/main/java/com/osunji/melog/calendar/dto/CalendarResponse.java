package com.osunji.melog.calendar.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponse {

    private Meta meta;
    private Calendar calendar;
    private List<Item> schedule;
//    private List<Item> items;


    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Meta {
        private int year;
        private int month;
        private int page;
        private int size;
        private boolean alarm;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Calendar {
        private List<List<Day>> weeks;  // 2차원 배열 형태 (5~6주)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Day {
        private String date;          // "2025-07-01"
        private boolean event;        // 해당 날짜에 일정 존재 여부

        @Builder.Default
        private List<UUID> eventList = Collections.emptyList();
    }


    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {

        private static final long serialVersionUID = 1L;

        private UUID id;
        private String title;
        private String category;
        private String thumbnailUrl;
        private String venue; // 장소
        private OffsetDateTime startDateTime;
        private OffsetDateTime endDateTime;
        private int dDay;
        private boolean bookmarked;
        private UUID eventId;
    }


    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RedisItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private UUID id;
        private String title;
        private String category;
        private String thumbnailUrl;
        private String venue; // 장소
        private OffsetDateTime startDateTime;
        private OffsetDateTime endDateTime;
        private int dDay;
        private boolean bookmarked;
    }
}
