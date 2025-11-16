package com.osunji.melog.calendar.service;

import com.osunji.melog.calendar.CultureCategory;
import com.osunji.melog.calendar.domain.Calendar;
import com.osunji.melog.calendar.dto.CalendarResponse;
import com.osunji.melog.calendar.repository.CalendarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
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
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "kcisa:items";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 메인 진입점
     * 1) 캐시(RedisItem) → 2) DB → 3) OpenAPI + DB insert → 다시 DB → 캐시 채움
     */
    @Transactional
    public List<CalendarResponse.Item> getItems(CultureCategory category) {
        String cacheKey = category.name();

        // 1) 캐시 조회 (RedisItem 기준)
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            @SuppressWarnings("unchecked")
            List<CalendarResponse.RedisItem> cached = cache.get(cacheKey, List.class);
            if (cached != null && !cached.isEmpty()) {
                log.debug("[CalendarItemProvider] cache hit: category={}, size={}", category, cached.size());
                // RedisItem → 응답용 Item 변환
                return cached.stream()
                        .map(this::fromRedisItem)
                        .toList();
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

            // Calendar → Item
            List<CalendarResponse.Item> items = fromDb.stream()
                    .map(this::toItemDto)
                    .toList();

            // Item → RedisItem 변환 후 캐시 저장
            List<CalendarResponse.RedisItem> redisItems = items.stream()
                    .map(this::toRedisItem)
                    .toList();

            putCacheAfterCommit(cache, cacheKey, redisItems);
            return items;
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

        List<CalendarResponse.Item> items = afterUpsert.stream()
                .map(this::toItemDto)
                .toList();

        List<CalendarResponse.RedisItem> redisItems = items.stream()
                .map(this::toRedisItem)
                .toList();

        putCacheAfterCommit(cache, cacheKey, redisItems);
        return items;
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
     * Calendar 엔티티 → 응답용 Item 변환
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
                .bookmarked(false)
                .eventId(null)
                .build();
    }

    /**
     * RedisItem → 응답용 Item 변환
     * (eventId는 Redis에 없으니 여기서는 null)
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
                .bookmarked(r.isBookmarked())
                .eventId(null)
                .build();
    }

    /**
     * 응답용 Item → 캐시용 RedisItem 변환
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
                .bookmarked(i.isBookmarked())
                .build();
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
