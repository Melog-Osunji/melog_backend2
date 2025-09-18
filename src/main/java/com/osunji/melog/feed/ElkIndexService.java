package com.osunji.melog.feed;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.transport.TransportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElkIndexService {

    private final ElasticsearchClient es;
    private final AtomicBoolean ensuring = new AtomicBoolean(false);

    private static final String POSTS_INDEX        = "posts";
    private static final String SEARCH_LOGS_INDEX  = "search_logs"; // ✅ 추가

    public EnsureResult ensureAll() {
        if (!ensuring.compareAndSet(false, true)) {
            return EnsureResult.busy("another ensure is running");
        }
        try {
            // 인증/연결 체크
            infoOrThrow();

            // ✅ 두 인덱스 모두 보장
            String postsStatus = ensurePostsIndex();
            String searchLogsStatus = ensureSearchLogsIndex();

            String detail = postsStatus + ", " + searchLogsStatus;
            return EnsureResult.ok(detail);

        } catch (ElasticsearchException ese) {
            var status = (ese.response() != null) ? ese.response().status() : -1;
            var type   = (ese.response() != null && ese.response().error() != null) ? ese.response().error().type() : "unknown";
            var reason = (ese.response() != null && ese.response().error() != null) ? ese.response().error().reason() : ese.getMessage();
            log.warn("[ElkEnsure] failed: status={}, type={}, reason={}", status, type, reason);
            return EnsureResult.fail("status=%d, type=%s, reason=%s".formatted(status, type, reason));
        } catch (TransportException te) {
            log.warn("[ElkEnsure] failed (transport): {}", te.getMessage());
            return EnsureResult.fail(te.getMessage());
        } catch (Exception e) {
            log.warn("[ElkEnsure] failed: {}", e.toString());
            return EnsureResult.fail(e.getMessage());
        } finally {
            ensuring.set(false);
        }
    }

    private String ensurePostsIndex() throws IOException, ElasticsearchException {
        if (indexExists(POSTS_INDEX)) return "exists:" + POSTS_INDEX;

        CreateIndexResponse resp = es.indices().create(b -> b
                .index(POSTS_INDEX)
                .settings(s -> s.numberOfShards("1").numberOfReplicas("0"))
                .mappings(m -> m
                        .properties("id",        p -> p.keyword(k -> k))
                        .properties("title",     p -> p.text(t -> t))
                        .properties("excerpt",   p -> p.text(t -> t))
                        .properties("tags",      p -> p.keyword(k -> k))
                        .properties("authorId",  p -> p.keyword(k -> k))
                        .properties("likeCount", p -> p.integer(i -> i))
                        .properties("createdAt", p -> p.date(d -> d))
                )
        );
        return "created:" + POSTS_INDEX + " ack=" + resp.acknowledged();
    }

    // ✅ search_logs 생성: query.keyword 서브필드 포함 (termsAgg("query.keyword") 대비)
    private String ensureSearchLogsIndex() throws IOException, ElasticsearchException {
        if (indexExists(SEARCH_LOGS_INDEX)) return "exists:" + SEARCH_LOGS_INDEX;

        CreateIndexResponse resp = es.indices().create(b -> b
                .index(SEARCH_LOGS_INDEX)
                .settings(s -> s.numberOfShards("1").numberOfReplicas("0"))
                .mappings(m -> m
                        .properties("id",       p -> p.keyword(k -> k))
                        .properties("userId",   p -> p.keyword(k -> k))
                        // multi-field: query.keyword 를 위해 fields 등록
                        .properties("query",    p -> p.text(t -> t
                                .fields("keyword", f -> f.keyword(k -> k))
                        ))
                        .properties("eventTime", p -> p.date(d -> d
                                .format("strict_date_optional_time||epoch_millis")
                        ))
                )
        );
        return "created:" + SEARCH_LOGS_INDEX + " ack=" + resp.acknowledged();
    }

    private boolean indexExists(String index) throws IOException, ElasticsearchException {
        try {
            GetIndexResponse r = es.indices().get(GetIndexRequest.of(b -> b.index(index)));
            return r != null;
        } catch (ElasticsearchException e) {
            if (e.response() != null && e.response().status() == 404) return false;
            throw e;
        }
        // 📌 대안(권장): exists API 사용
        // var exists = es.indices().exists(b -> b.index(index));
        // return Boolean.TRUE.equals(exists.value());
    }

    /** GET / (info) — 인증 실패면 여기서 예외 발생 */
    private void infoOrThrow() throws IOException, ElasticsearchException {
        var info = es.info();
        if (info == null || info.version() == null) {
            throw new IllegalStateException("elasticsearch info failed");
        }
    }

    public record EnsureResult(boolean success, String detail) {
        public static EnsureResult ok(String d)   { return new EnsureResult(true, d); }
        public static EnsureResult fail(String d) { return new EnsureResult(false, d); }
        public static EnsureResult busy(String d) { return new EnsureResult(false, d); }
    }
}
