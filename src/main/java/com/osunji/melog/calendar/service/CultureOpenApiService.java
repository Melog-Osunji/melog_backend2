package com.osunji.melog.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.osunji.melog.calendar.CultureCategory;
import com.osunji.melog.calendar.dto.CalendarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.Duration;

import static io.micrometer.common.util.StringUtils.truncate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CultureOpenApiService {

    private final WebClient cultureWebClient; // baseUrl은 https://api.kcisa.kr
    @Value("${openapi.kcisa.service-key}")
    private String serviceKey;

    private static final String CNV060 = "/openapi/CNV_060/request";
    private static final ZoneId   KST   = ZoneId.of("Asia/Seoul");
    private static final String TITLE_ALL = " ";


    /** KCISA CNV_060에서 최대 20개 요청 → Item 매핑 → 최근 10일 시작일 필터 */
    public List<CalendarResponse.Item> fetchItems(CultureCategory category) {
        LocalDate today = LocalDate.now(KST);
        LocalDate from  = today.minusDays(10);

        List<JsonNode> buckets = new ArrayList<>();

        if (category == CultureCategory.ALL) {
            // ALL이면 enum 순회 (ALL 제외) + title=공백 1자
            for (CultureCategory c : CultureCategory.values()) {
                if (c == CultureCategory.ALL) continue;
                String dtype = c.dtype().orElse(null);
                if (dtype == null || dtype.isBlank()) continue;

                // 1차: 공백 1자로 호출 → 실패(403/timeout 등) 시 폴백 키워드
                JsonNode root = callCnV060WithFallback(dtype, TITLE_ALL,
                        (c == CultureCategory.EXHIBITION) ? "전시" : "공연");
                if (root != null) buckets.add(root);
                log.info("응답: {}", root);

            }
        } else {
            String dtype = category.dtype().orElse(null);
            if (dtype == null || dtype.isBlank()) return List.of();

            JsonNode root = callCnV060WithFallback(dtype, TITLE_ALL,
                    (category == CultureCategory.EXHIBITION) ? "전시" : "공연");
            log.info("응답: {}", root);
            if (root != null) buckets.add(root);
        }

        ArrayNode allItems = mergeAllDataArrays(buckets);
        if (allItems == null || allItems.isEmpty()) return List.of();

        Map<UUID, CalendarResponse.Item> uniq = new LinkedHashMap<>();
        for (JsonNode n : allItems) {
            CalendarResponse.Item it = mapNodeToItem(n, category);

            // 끝난(= 종료일이 오늘보다 이전) 것만 제외
            var start = it.getStartDateTime();
            var end   = (it.getEndDateTime() != null) ? it.getEndDateTime() : start;
            if (end == null) continue; // 날짜 정보가 전혀 없으면 스킵

            if (end.toLocalDate().isBefore(today)) {
                continue; // 과거에 완전히 끝난 일정 → 제외
            }

            uniq.putIfAbsent(it.getId(), it); // 중복 제거
            if (uniq.size() >= 20) break;
        }
        return new ArrayList<>(uniq.values());
    }

    /** CNV_060 실제 호출: dtype/ title(2자+) 필수, 타임아웃/에러처리 + 로깅 */
    private JsonNode callCnV060(String dtype, String titleKeyword) {
        // title은 2자 이상 필요 → 한 글자면 즉시 폴백이 일어나게끔 로그로 표시
        if (titleKeyword == null || titleKeyword.trim().length() < 2) {
            log.warn("⚠️ titleKeyword가 2자 미만입니다. KCISA가 204/빈응답을 줄 수 있음: '{}'", titleKeyword);
        }

        try {
            // 원시 응답 바디(String)로 먼저 수신 후, ObjectMapper로 JsonNode 파싱
            String body = cultureWebClient.get()
                    .uri(b -> {
                        var uri = b.path(CNV060)
                                .queryParam("serviceKey", serviceKey)
                                .queryParam("numOfRows", 20)
                                .queryParam("pageNo", 1)
                                .queryParam("dtype", dtype)           // ★ 필수
                                .queryParam("title", titleKeyword)    // ★ 필수(2자 이상)
                                .build();
                        log.debug("🌐 KCISA 요청 URI -> {}", uri);
                        return uri;
                    })
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(
                            st -> st.value() >= 400,
                            resp -> resp.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(errBody -> {
                                        log.error("❌ KCISA HTTP {} 에러, body={}", resp.statusCode(), truncate(errBody, 2000));
                                        return new IllegalStateException("KCISA HTTP " + resp.statusCode());
                                    })
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(12))
                    .doOnSubscribe(s -> log.debug("➡️ KCISA 호출 시작: dtype='{}', title='{}'", dtype, titleKeyword))
                    .doOnError(ex -> log.warn("⚠️ KCISA 호출 오류: dtype={}, title={}, err={}", dtype, titleKeyword, ex.toString()))
                    .onErrorResume(ex -> Mono.empty())
                    .block();

            if (body == null) {
                log.warn("⚠️ KCISA 응답 바디가 null입니다. (dtype='{}', title='{}')", dtype, titleKeyword);
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);

            // 응답 최상위 구조 요약 로그

            // items.item 위치 실제 확인
            ArrayNode items = findArray(root, "response", "body", "items", "item");
            if (items == null || items.isEmpty()) {
                log.warn("⚠️ items.item 배열을 찾지 못했거나 비어 있습니다. (dtype='{}', title='{}')", dtype, titleKeyword);
            } else {
                log.info("✅ items.item 크기: {}", items.size());
            }
            return root;

        } catch (Exception e) {
            log.warn("KCISA CNV_060 call unexpected error: {}, dtype={}, title={}", e.toString(), dtype, titleKeyword);
            return null;
        }
    }


    /** 여러 응답에서 data 배열만 합치기 */
    private static ArrayNode mergeAllDataArrays(List<JsonNode> roots) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode merged = mapper.createArrayNode();
        for (JsonNode root : roots) {
            // 실제 응답 구조: response -> body -> items -> item
            ArrayNode items = findArray(root, "response","body","items","item");
            if (items != null) {
                for (JsonNode n : items) merged.add(n);
            }
        }
        return merged;
    }


    /** CNV_060 응답 → Item 매핑 */
    private CalendarResponse.Item mapNodeToItem(JsonNode n, CultureCategory reqCat) {
        String title    = text(n, "title");
        String type     = text(n, "type");         // 분야(연극/뮤지컬/…)
        String period   = text(n, "period", "eventPeriod"); // “yyyy.MM.dd ~ yyyy.MM.dd”
        String eventSite= text(n, "eventSite");    // 장소
        String image    = text(n, "imageObject");  // 썸네일 URL
        String url      = text(n, "url");          // 상세 URL(있으면 UUID 생성 근거로 사용)

        // period → 시작/종료일
        LocalDate[] range = parsePeriod(period);
        LocalDate s = range[0];
        LocalDate e = (range[1] != null ? range[1] : range[0]);

        // 카테고리: 요청값 우선(ALL이면 응답 type 사용, 없으면 '기타')
        String categoryLabel = (reqCat != CultureCategory.ALL)
                ? reqCat.getLabel()
                : (isBlank(type) ? "기타" : type);

        UUID id = makeStableUUID(url, title, s, eventSite); // URL 우선 안정 UUID
        int dDay = (s == null) ? 0 : (int) ChronoUnit.DAYS.between(LocalDate.now(KST), s);

        return CalendarResponse.Item.builder()
                .id(id)
                .title(defaultIfBlank(title, "(제목없음)"))
                .category(categoryLabel)
                .thumbnailUrl(blankToNull(image))
                .venue(blankToNull(eventSite))
                .startDateTime(toOffset(s))
                .endDateTime(toOffset(e))
                .dDay(dDay)
                .bookmarked(false) // 북마크는 별도 주입
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

    /** “yyyy.MM.dd ~ yyyy.MM.dd” / “yyyy-MM-dd ~ yyyy-MM-dd” / “yyyyMMdd~yyyyMMdd” 등 파싱 */
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
                DateTimeFormatter.ISO_LOCAL_DATE,          // yyyy-MM-dd
                DateTimeFormatter.BASIC_ISO_DATE,          // yyyyMMdd
                DateTimeFormatter.ofPattern("yyyy-MM")     // yyyy-MM (1일로 파싱)
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

    private JsonNode callCnV060WithFallback(String dtype, String preferredTitle, String fallbackTitle) {
        JsonNode r = callCnV060(dtype, preferredTitle);
        if (r != null) return r;
        // 첫 호출 실패(403, 5xx, timeout 등) 시 폴백
        return callCnV060(dtype, fallbackTitle);
    }
}
