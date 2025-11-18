package com.osunji.melog.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.osunji.melog.calendar.CultureCategory;
import com.osunji.melog.calendar.dto.CalendarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static io.micrometer.common.util.StringUtils.truncate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CultureOpenApiClient {

    private final WebClient cultureWebClient; // baseUrl: https://api.kcisa.kr
    @Value("${openapi.kcisa.service-key}")
    private String serviceKey;

    private static final String CNV060 = "/openapi/CNV_060/request";
    private static final ZoneId   KST   = ZoneId.of("Asia/Seoul");
    private static final String TITLE_ALL = " ";

    /**
     * KCISA CNV_060 호출 → Item 매핑 (과거 종료 일정 제외, 최대 20)
     * 저장/캐시는 하지 않는다. 오직 호출/가공만 담당.
     */
    public List<CalendarResponse.Item> fetchItems(CultureCategory category) {
        LocalDate today = LocalDate.now(KST);

        List<JsonNode> buckets = new ArrayList<>();

        if (category == CultureCategory.ALL) {
            for (CultureCategory c : CultureCategory.values()) {
                if (c == CultureCategory.ALL) continue;
                String dtype = c.dtype().orElse(null);
                if (dtype == null || dtype.isBlank()) continue;

                JsonNode root = callCnV060WithFallback(dtype, TITLE_ALL,
                        (c == CultureCategory.EXHIBITION) ? "전시" : "공연");
                if (root != null) buckets.add(root);
            }
        } else {
            String dtype = category.dtype().orElse(null);
            if (dtype == null || dtype.isBlank()) return List.of();

            JsonNode root = callCnV060WithFallback(dtype, TITLE_ALL,
                    (category == CultureCategory.EXHIBITION) ? "전시" : "공연");
            if (root != null) buckets.add(root);
        }

        ArrayNode allItems = mergeAllDataArrays(buckets);
        if (allItems == null || allItems.isEmpty()) return List.of();

        Map<UUID, CalendarResponse.Item> uniq = new LinkedHashMap<>();
        for (JsonNode n : allItems) {
            CalendarResponse.Item it = mapNodeToItem(n, category);

            var start = it.getStartDateTime();
            var end   = (it.getEndDateTime() != null) ? it.getEndDateTime() : start;
            if (end == null) continue;

            if (end.toLocalDate().isBefore(today)) {
                continue; // 과거 종료 일정 제외
            }

            uniq.putIfAbsent(it.getId(), it);
            if (uniq.size() >= 20) break;
        }
        return new ArrayList<>(uniq.values());
    }

    // ====== 내부 구현 (호출 + 매핑 유틸) ======

    private JsonNode callCnV060(String dtype, String titleKeyword) {
        if (titleKeyword == null || titleKeyword.trim().length() < 2) {
            log.warn("titleKeyword가 2자 미만: '{}'", titleKeyword);
        }

        try {
            String body = cultureWebClient.get()
                    .uri(b -> b.path(CNV060)
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("numOfRows", 20)
                            .queryParam("pageNo", 1)
                            .queryParam("dtype", dtype)           // 필수
                            .queryParam("title", titleKeyword)    // 필수(2자 이상)
                            .build()
                    )
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(
                            st -> st.value() >= 400,
                            resp -> resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(errBody -> {
                                        log.error("KCISA HTTP {} 에러, body={}", resp.statusCode(), truncate(errBody, 2000));
                                        return new IllegalStateException("KCISA HTTP " + resp.statusCode());
                                    })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(12))
                    .doOnSubscribe(s -> log.debug("KCISA 호출 시작: dtype='{}', title='{}'", dtype, titleKeyword))
                    .doOnError(ex -> log.warn("KCISA 호출 오류: dtype={}, title={}, err={}", dtype, titleKeyword, ex.toString()))
                    .onErrorResume(ex -> Mono.empty())
                    .block();

            if (body == null) {
                log.warn("KCISA 응답 바디 null (dtype='{}', title='{}')", dtype, titleKeyword);
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);

            ArrayNode items = findArray(root, "response", "body", "items", "item");
            if (items == null || items.isEmpty()) {
                log.warn("items.item 없음/빈 배열 (dtype='{}', title='{}')", dtype, titleKeyword);
            } else {
                log.info("items.item 크기: {}", items.size());
            }
            return root;

        } catch (Exception e) {
            log.warn("KCISA CNV_060 unexpected error: {}, dtype={}, title={}", e.toString(), dtype, titleKeyword);
            return null;
        }
    }

    private JsonNode callCnV060WithFallback(String dtype, String preferredTitle, String fallbackTitle) {
        JsonNode r = callCnV060(dtype, preferredTitle);
        if (r != null) return r;
        return callCnV060(dtype, fallbackTitle);
    }

    private static ArrayNode mergeAllDataArrays(List<JsonNode> roots) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode merged = mapper.createArrayNode();
        for (JsonNode root : roots) {
            ArrayNode items = findArray(root, "response","body","items","item");
            if (items != null) {
                for (JsonNode n : items) merged.add(n);
            }
        }
        return merged;
    }

    /** KCISA 응답 → Item 매핑 */
    private CalendarResponse.Item mapNodeToItem(JsonNode n, CultureCategory reqCat) {
        String title    = text(n, "title");
        String type     = text(n, "type");                   // 분야
        String period   = text(n, "period", "eventPeriod");  // yyyy.MM.dd ~ yyyy.MM.dd
        String eventSite= text(n, "eventSite");              // 장소
        String image    = text(n, "imageObject");            // 썸네일
        String url      = text(n, "url");                    // 상세 URL

        LocalDate[] range = parsePeriod(period);
        LocalDate s = range[0];
        LocalDate e = (range[1] != null ? range[1] : range[0]);

        String categoryLabel = (reqCat != CultureCategory.ALL)
                ? reqCat.getLabel()
                : (isBlank(type) ? "기타" : type);

        UUID id = makeStableUUID(url, title, s, eventSite);
        int dDay = (s == null) ? 0 : (int) ChronoUnit.DAYS.between(LocalDate.now(KST), s);

        return CalendarResponse.Item.builder()
                .id(id)
                .title(defaultIfBlank(title, "(제목없음)"))
                .category(categoryLabel)
                .thumbnailUrl(blankToNull(image))
                .venue(blankToNull(eventSite))
//                .detailUrl(blankToNull(url))                  // ← 누락되기 쉬워 추가
                .startDateTime(toOffset(s))
                .endDateTime(toOffset(e))
                .dDay(dDay)
                .bookmarked(false)
                .build();
    }

    // ====== 유틸 ======
    private static ArrayNode findArray(JsonNode root, String... keys) {
        if (root == null) return null;
        for (String k : keys) {
            JsonNode node = root.path(k);
            if (node.isArray()) return (ArrayNode) node;
        }
        var it = root.fieldNames();
        while (it.hasNext()) {
            ArrayNode arr = findArray(root.path(it.next()), keys);
            if (arr != null) return arr;
        }
        return null;
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.path(k);
            if (v.isTextual()) return v.asText();
        }
        return null;
    }

    private static boolean isBlank(String s){ return s==null || s.isBlank(); }
    private static String defaultIfBlank(String s,String d){ return isBlank(s)?d:s; }
    private static String blankToNull(String s){ return isBlank(s)?null:s; }

    private static OffsetDateTime toOffset(LocalDate d) {
        return (d == null) ? null : d.atStartOfDay().atOffset(ZoneOffset.ofHours(9));
    }

    private static LocalDate[] parsePeriod(String raw) {
        if (raw == null) return new LocalDate[]{null, null};
        String norm = raw.replace(" ", "")
                .replace(".", "-")
                .replace("/", "-");
        String[] parts = norm.split("~");
        LocalDate start = parseDateFlexible(parts[0]);
        LocalDate end   = (parts.length > 1) ? parseDateFlexible(parts[1]) : null;
        return new LocalDate[]{start, end};
    }

    private static LocalDate parseDateFlexible(String r) {
        if (r == null) return null;
        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.BASIC_ISO_DATE,
                DateTimeFormatter.ofPattern("yyyy-MM")
        );
        for (DateTimeFormatter f : fmts) {
            try { return LocalDate.parse(r, f); } catch (Exception ignore) {}
        }
        String digits = r.replaceAll("\\D", "");
        if (digits.length()==8) {
            try { return LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE);} catch(Exception ignore){}
        }
        return null;
    }

    private static UUID makeStableUUID(String url, String title, LocalDate s, String place) {
        String basis = !isBlank(url) ? "kcisa:"+url
                : (defaultIfBlank(title,"") + "|" + (s!=null?s:"") + "|" + defaultIfBlank(place,""));
        return UUID.nameUUIDFromBytes(basis.getBytes(StandardCharsets.UTF_8));
    }
}
