package com.osunji.melog.calendar.service;

import com.osunji.melog.calendar.CultureCategory;
import com.osunji.melog.calendar.domain.Calendar;
import com.osunji.melog.calendar.domain.EventSchedule;
import com.osunji.melog.calendar.dto.CalendarResponse;
import com.osunji.melog.calendar.repository.CalendarRepository;
import com.osunji.melog.calendar.repository.EventScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarItemProvider {

    private final CultureOpenApiClient openApiClient;   // KCISA OpenAPI 호출 전담
    private final CalendarRepository calendarRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "kcisa:items";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 메인 진입점
     * 1) 캐시(RedisItem - 전역 공용) → 2) DB → 3) OpenAPI + DB insert → 다시 DB → 캐시 채움
     * userId가 주어지면, 해당 유저가 저장한 일정 기준으로
     *  - bookmarked = true
     *  - eventId = EventSchedule.id
     * 를 채워서 반환한다.
     */
    @Transactional
    public List<CalendarResponse.Item> getItems(CultureCategory category, @Nullable UUID userId) {
        String cacheKey = category.name();

        // 1) 캐시 조회 (RedisItem 기준, 유저와 무관한 전역 캐시)
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            @SuppressWarnings("unchecked")
            List<CalendarResponse.RedisItem> cached = cache.get(cacheKey, List.class);
            if (cached != null && !cached.isEmpty()) {
                log.debug("[CalendarItemProvider] cache hit: category={}, size={}", category, cached.size());

                // RedisItem → 기본 Item 리스트 (bookmarked=false, eventId=null)
                List<CalendarResponse.Item> baseItems = cached.stream()
                        .map(this::fromRedisItem)
                        .toList();

                // 유저가 있으면 bookmark/eventId 반영
                return applyBookmarkAndEventId(baseItems, userId);
            }
        }

        LocalDate today = LocalDate.now(KST);
        String classificationFilter = (category == CultureCategory.ALL ? null : category.getLabel());

        // 2) DB에서 아직 안 끝난 일정 조회
        var fromDb = calendarRepository.findActive(
                classificationFilter,
                today,
                PageRequest.of(0, 20)
        );

        if (!fromDb.isEmpty()) {
            log.debug("[CalendarItemProvider] db hit: category={}, size={}", category, fromDb.size());

            // Calendar → 기본 Item 리스트
            List<CalendarResponse.Item> baseItems = fromDb.stream()
                    .map(this::toItemDto)
                    .toList();

            // 전역 캐시에는 유저 정보 없는 RedisItem으로 저장
            List<CalendarResponse.RedisItem> redisItems = baseItems.stream()
                    .map(this::toRedisItem)
                    .toList();
            putCacheAfterCommit(cache, cacheKey, redisItems);

            // 응답에는 유저별 bookmark/eventId 반영
            return applyBookmarkAndEventId(baseItems, userId);
        }

        // 3) DB에도 없으면 → OpenAPI 호출
        log.debug("[CalendarItemProvider] db empty → call OpenAPI: category={}", category);
        List<CalendarResponse.Item> fresh = openApiClient.fetchItems(category);
        if (fresh.isEmpty()) {
            log.warn("[CalendarItemProvider] OpenAPI도 빈 결과: category={}", category);
            return List.of();
        }

        // 4) 새로 들어온 아이템들을 DB에 insert (이미 있는 id는 건너뜀)
        upsertNewCalendars(fresh);

        // 5) 다시 DB에서 “정식 소스”로 조회해서 응답 구성
        var afterUpsert = calendarRepository.findActive(
                classificationFilter,
                today,
                PageRequest.of(0, 20)
        );

        List<CalendarResponse.Item> baseItems = afterUpsert.stream()
                .map(this::toItemDto)
                .toList();

        List<CalendarResponse.RedisItem> redisItems = baseItems.stream()
                .map(this::toRedisItem)
                .toList();
        putCacheAfterCommit(cache, cacheKey, redisItems);

        return applyBookmarkAndEventId(baseItems, userId);
    }

    /**
     * OpenAPI에서 받은 Item 목록 중 DB에 없는 ID만 신규 insert.
     * (기존 row는 변경하지 않는다 — 일정은 대부분 정적이므로)
     */
    protected void upsertNewCalendars(List<CalendarResponse.Item> items) {
        if (items.isEmpty()) return;

        // 이미 존재하는 id 미리 조회
        List<UUID> ids = items.stream()
                .map(CalendarResponse.Item::getId)
                .toList();

        Map<UUID, Calendar> existingMap = calendarRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Calendar::getId, c -> c));

        List<Calendar> toInsert = new ArrayList<>();
        for (CalendarResponse.Item it : items) {
            if (existingMap.containsKey(it.getId())) {
                // 이미 한 번 저장된 일정이면 스킵
                continue;
            }

            LocalDate startDate = (it.getStartDateTime() != null)
                    ? it.getStartDateTime().toLocalDate()
                    : null;
            LocalDate endDate = (it.getEndDateTime() != null)
                    ? it.getEndDateTime().toLocalDate()
                    : startDate;

            // externalId는 안정성이 있는 값이면 충분 → 여기서는 id 문자열로 사용
            String externalId = it.getId().toString();

            Calendar calendar = new Calendar(
                    it.getId(),
                    "KCISA_CNV_060",         // source
                    externalId,              // external_id
                    null,                    // detail_url (필요 시 CultureOpenApiClient에서 추가)
                    it.getTitle(),
                    it.getCategory(),        // classification
                    it.getVenue(),           // region(=장소/지역)
                    startDate,
                    endDate,
                    null,                    // description
                    it.getThumbnailUrl()     // image_url
            );

            toInsert.add(calendar);
        }

        if (!toInsert.isEmpty()) {
            calendarRepository.saveAll(toInsert);
            log.debug("[CalendarItemProvider] inserted {} new calendars", toInsert.size());
        }
    }

    /**
     * Calendar 엔티티 → 응답용 Item 변환 (bookmarked=false, eventId=null 기준)
     */
    private CalendarResponse.Item toItemDto(Calendar c) {
        LocalDate start = c.getStartDate();
        LocalDate end   = (c.getEndDate() != null ? c.getEndDate() : c.getStartDate());

        OffsetDateTime startOd = (start != null)
                ? start.atStartOfDay(ZoneOffset.ofHours(9)).toOffsetDateTime()
                : null;
        OffsetDateTime endOd = (end != null)
                ? end.atStartOfDay(ZoneOffset.ofHours(9)).toOffsetDateTime()
                : null;

        int dDay = 0;
        if (start != null) {
            dDay = (int) ChronoUnit.DAYS.between(
                    LocalDate.now(KST),
                    start
            );
        }

        String categoryLabel = (c.getClassification() != null && !c.getClassification().isBlank())
                ? c.getClassification()
                : "기타";

        return CalendarResponse.Item.builder()
                .id(c.getId())
                .title(c.getTitle())
                .category(categoryLabel)
                .thumbnailUrl(c.getImageUrl())
                .venue(c.getRegion())
                .startDateTime(startOd)
                .endDateTime(endOd)
                .dDay(dDay)
                .bookmarked(false)  // 기본값: 저장 안 한 상태
                .eventId(null)      // 기본값: 연관 EventSchedule 없음
                .build();
    }

    /**
     * RedisItem → 기본 Item 변환 (bookmarked=false, eventId=null 기준)
     */
    private CalendarResponse.Item fromRedisItem(CalendarResponse.RedisItem r) {
        return CalendarResponse.Item.builder()
                .id(r.getId())
                .title(r.getTitle())
                .category(r.getCategory())
                .thumbnailUrl(r.getThumbnailUrl())
                .venue(r.getVenue())
                .startDateTime(r.getStartDateTime())
                .endDateTime(r.getEndDateTime())
                .dDay(r.getDDay())
                .bookmarked(false)   // 캐시는 유저 정보가 없으니 항상 false로 두고,
                .eventId(null)       // 아래 applyBookmarkAndEventId에서 유저 기준으로 다시 세팅
                .build();
    }

    /**
     * 응답용 Item → 캐시용 RedisItem 변환 (유저 정보 없는 전역 데이터)
     */
    private CalendarResponse.RedisItem toRedisItem(CalendarResponse.Item i) {
        return CalendarResponse.RedisItem.builder()
                .id(i.getId())
                .title(i.getTitle())
                .category(i.getCategory())
                .thumbnailUrl(i.getThumbnailUrl())
                .venue(i.getVenue())
                .startDateTime(i.getStartDateTime())
                .endDateTime(i.getEndDateTime())
                .dDay(i.getDDay())
                .bookmarked(false) // 전역 캐시는 bookmark를 쓰지 않음 (항상 false)
                .build();
    }

    /**
     * 유저가 저장한 일정(EventSchedule) 기준으로
     *  - bookmarked = true/false
     *  - eventId    = EventSchedule.id
     * 를 반영한 새 Item 리스트를 만들어 반환한다.
     *
     * userId == null 이면 그대로 반환.
     */
    private List<CalendarResponse.Item> applyBookmarkAndEventId(List<CalendarResponse.Item> baseItems,
                                                                @Nullable UUID userId) {
        if (userId == null || baseItems.isEmpty()) {
            return baseItems;
        }

        // 현재 응답에 포함된 calendar id들
        List<UUID> calendarIds = baseItems.stream()
                .map(CalendarResponse.Item::getId)
                .filter(Objects::nonNull)
                .toList();

        if (calendarIds.isEmpty()) {
            return baseItems;
        }

        // 해당 유저가 저장한 EventSchedule 목록 조회
        List<EventSchedule> schedules = eventScheduleRepository
                .findByUser_IdAndCalendar_IdIn(userId, calendarIds);

        // calendar.id → EventSchedule 매핑 (여러 개일 경우 첫 번째만 사용)
        Map<UUID, EventSchedule> scheduleByCalendarId = schedules.stream()
                .collect(Collectors.toMap(
                        es -> es.getCalendar().getId(),
                        es -> es,
                        (a, b) -> a  // 중복 시 첫 번째 유지
                ));

        // 기존 Item을 복사해서 bookmarked / eventId만 채워서 반환
        return baseItems.stream()
                .map(item -> {
                    EventSchedule es = scheduleByCalendarId.get(item.getId());
                    boolean bookmarked = (es != null);
                    UUID eventId = (es != null ? es.getId() : null);

                    return CalendarResponse.Item.builder()
                            .id(item.getId())
                            .title(item.getTitle())
                            .category(item.getCategory())
                            .thumbnailUrl(item.getThumbnailUrl())
                            .venue(item.getVenue())
                            .startDateTime(item.getStartDateTime())
                            .endDateTime(item.getEndDateTime())
                            .dDay(item.getDDay())
                            .bookmarked(bookmarked)
                            .eventId(eventId)
                            .build();
                })
                .toList();
    }

    /**
     * 트랜잭션 커밋 이후에만 캐시에 값 넣기 (RedisItem 리스트)
     */
    private void putCacheAfterCommit(Cache cache, String cacheKey, List<CalendarResponse.RedisItem> redisItems) {
        if (cache == null) return;
        if (redisItems == null || redisItems.isEmpty()) return;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache.put(cacheKey, redisItems);
                    log.debug("[CalendarItemProvider] cache put after commit: key={}, size={}", cacheKey, redisItems.size());
                }
            });
        } else {
            cache.put(cacheKey, redisItems);
            log.debug("[CalendarItemProvider] cache put (no tx): key={}, size={}", cacheKey, redisItems.size());
        }
    }
}
