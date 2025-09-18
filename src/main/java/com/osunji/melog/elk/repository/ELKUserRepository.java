package com.osunji.melog.elk.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ELKUserRepository {

    private final ElasticsearchClient elasticsearchClient;

    private static final String INDEX_USER_LOGS = "user_logs";
    private static final String F_USER_ID = "userId";
    private static final String F_EVENT_TYPE = "eventType";
    private static final String F_EVENT_TIME = "eventTime";
    private static final String F_IP = "ip";
    private static final String F_UA = "userAgent";
    private static final String F_META = "metaJson";

    // =========================== 기록(실제 기능) ===========================

    /**
     * 유저 이벤트 로그 기록
     */
    public void logUserEvent(String userId, String eventType, String ip, String userAgent, String metaJson) {
        try {
            ensureIndexExists(INDEX_USER_LOGS);

            Map<String, Object> doc = new HashMap<>();
            doc.put(F_USER_ID, userId);
            doc.put(F_EVENT_TYPE, eventType);
            doc.put(F_EVENT_TIME, LocalDateTime.now());
            doc.put(F_IP, ip);
            doc.put(F_UA, userAgent);
            doc.put(F_META, (metaJson == null || metaJson.isBlank()) ? "{}" : metaJson);

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(INDEX_USER_LOGS)
                    .document(doc)
            );

            elasticsearchClient.index(request);
            log.info("유저 이벤트 로그 저장 완료: userId={}, eventType={}", userId, eventType);
        } catch (Exception e) {
            log.error("유저 이벤트 로그 기록 실패", e);
        }
    }

    // =========================== 조회/집계 ===========================

    /**
     * 최근 N일 간 DAU(일간 활성 사용자 수) 시계열 (yyyy-MM-dd -> dau)
     */
    public LinkedHashMap<String, Long> getDauSeries(int days) {
        try {
            ensureIndexExists(INDEX_USER_LOGS);

            // 날짜 범위는 문자열("now-<days>d")로 넘겨 타입 이슈 제거
            SearchRequest req = SearchRequest.of(s -> s
                    .index(INDEX_USER_LOGS)
                    .size(0)
                    .query(q -> q.range(r -> r
                            .date(d -> d
                                    .field(F_EVENT_TIME)
                                    .gte("now-" + days + "d")       // Date math 문자열 그대로
                            )
                    ))
                    .aggregations("daily", a -> a
                            .dateHistogram(dh -> dh
                                    .field(F_EVENT_TIME)
                                    .calendarInterval(CalendarInterval.Day)
                            )
                            .aggregations("dau", sub -> sub.cardinality(c -> c.field(F_USER_ID)))
                    )
            );

            SearchResponse<Void> res = elasticsearchClient.search(req, Void.class);

            List<co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket> buckets =
                    res.aggregations()
                            .get("daily")
                            .dateHistogram()
                            .buckets()
                            .array();

            LinkedHashMap<String, Long> series = new LinkedHashMap<>();
            // keyAsString 예: 2025-09-12T00:00:00.000Z
            for (co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket b : buckets) {
                String keyStr = b.keyAsString();
                if (keyStr == null) continue;
                String day = keyStr.length() >= 10 ? keyStr.substring(0, 10) : keyStr; // yyyy-MM-dd

                Long dau = Optional.ofNullable(
                        b.aggregations()
                                .get("dau")
                                .cardinality()
                                .value()
                ).orElse(0L);

                series.put(day, dau);
            }

            // 날짜 오름차순 정렬 보장
            LinkedHashMap<String, Long> sorted = series.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            return sorted;
        } catch (Exception e) {
            log.error("DAU 시계열 조회 실패", e);
            return new LinkedHashMap<>();
        }
    }


    /**
     * 최근 가입자 목록 (최근 N일, 최신순, 최대 size)
     * eventType = SIGNUP 기준
     */
    public List<String> getRecentSignups(int days, int size) {
        try {
            ensureIndexExists(INDEX_USER_LOGS);

            SearchRequest req = SearchRequest.of(s -> s
                    .index(INDEX_USER_LOGS)
                    .size(size)
                    .sort(sort -> sort.field(f -> f.field(F_EVENT_TIME).order(SortOrder.Desc)))
                    .query(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field(F_EVENT_TYPE).value("SIGNUP")))
                            .must(m -> m.range(r -> r
                                    .date(d -> d
                                            .field(F_EVENT_TIME)
                                            .gte("now-" + days + "d")
                                    )
                            ))
                    ))
                    .source(src -> src.filter(f -> f.includes(List.of(F_USER_ID))))
            );

            @SuppressWarnings("unchecked")
            SearchResponse<Map<String, Object>> res =
                    elasticsearchClient.search(req, (Class<Map<String, Object>>)(Class<?>)Map.class);

            return res.hits().hits().stream()
                    .map(h -> {
                        Map<String, Object> src = h.source();
                        return src == null ? null : (String) src.get(F_USER_ID);
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("최근 가입자 조회 실패", e);
            return Collections.emptyList();
        }
    }


    /**
     * 특정 유저의 최근 이벤트 타입들 (최근 N일, 최신순)
     */
    public List<String> getUserRecentEvents(String userId, int days, int size) {
        try {
            ensureIndexExists(INDEX_USER_LOGS);

            SearchRequest req = SearchRequest.of(s -> s
                    .index(INDEX_USER_LOGS)
                    .size(size)
                    .sort(sort -> sort.field(f -> f.field(F_EVENT_TIME).order(SortOrder.Desc)))
                    .query(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field(F_USER_ID).value(userId)))
                            .must(m -> m.range(r -> r
                                    .date(d -> d
                                            .field(F_EVENT_TIME)        // ← time(...)이 아니라 date(...).field(...)
                                            .gte("now-" + days + "d")
                                    )
                            ))
                    ))
                    .source(src -> src.filter(f -> f.includes(List.of(F_EVENT_TYPE))))
            );

            @SuppressWarnings("unchecked")
            SearchResponse<Map<String, Object>> res =
                    elasticsearchClient.search(req, (Class<Map<String, Object>>)(Class<?>)Map.class);

            return res.hits().hits().stream()
                    .map(h -> {
                        Map<String, Object> src = h.source();
                        return src == null ? null : (String) src.get(F_EVENT_TYPE);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("유저 최근 이벤트 조회 실패(userId={})", userId, e);
            return Collections.emptyList();
        }
    }

    // =========================== 헬퍼 ===========================

    private void ensureIndexExists(String indexName) {
        try {
            GetIndexRequest get = GetIndexRequest.of(g -> g.index(indexName));
            elasticsearchClient.indices().get(get);
        } catch (Exception e) {
            createIndex(indexName);
        }
    }

    private void createIndex(String indexName) {
        try {
            if (!INDEX_USER_LOGS.equals(indexName)) return;

            CreateIndexRequest create = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties(F_USER_ID,    p -> p.keyword(k -> k))
                            .properties(F_EVENT_TYPE, p -> p.keyword(k -> k))
                            .properties(F_EVENT_TIME, p -> p.date(d -> d))
                            .properties(F_IP,         p -> p.keyword(k -> k))
                            .properties(F_UA,         p -> p.keyword(k -> k.ignoreAbove(256)))
                            // metaJson은 문자열로 저장(검색 인덱싱 제외)
                            .properties(F_META,       p -> p.text(t -> t.index(false)))
                    )
            );
            elasticsearchClient.indices().create(create);
            log.info("{} 인덱스 생성 완료", INDEX_USER_LOGS);
        } catch (Exception e) {
            log.error("{} 인덱스 생성 실패", INDEX_USER_LOGS, e);
        }
    }
}
