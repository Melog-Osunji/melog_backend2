package com.osunji.melog.feed;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElkSeedService {

    private final ElasticsearchClient es;

    public static final String USERS_INDEX      = "users";
    public static final String USER_LOGS_INDEX  = "user_logs";
    public static final String POSTS_INDEX      = "posts"; // ⬅️ 추가

    // ====== Public APIs ======

    /** u1..u{count} 형태로 유저 도큐먼트 생성 */
    public SeedResult seedUsers(int count, Long seed) throws IOException {
        ensureUsersIndex();
        Random rnd = seed == null ? new Random() : new Random(seed);

        List<UserDoc> docs = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String id = "u" + i;
            docs.add(new UserDoc(
                    id,
                    "user_" + i,
                    List.of(pick(rnd, SAMPLE_TAGS), pick(rnd, SAMPLE_TAGS)),
                    rnd.nextInt(3000),
                    rnd.nextInt(1000),
                    "Hello, I'm " + i,
                    Instant.now().minus(rnd.nextInt(365), ChronoUnit.DAYS)
            ));
        }
        int inserted = bulkIndexUsers(docs);
        return new SeedResult(true, "users=" + inserted);
    }

    /** 최근 30일 랜덤 시간대로 유저 로그 {events}건 생성 (userId는 u1..u{userCount} 중 랜덤) */
    public SeedResult seedUserLogs(int userCount, int events, Long seed) throws IOException {
        ensureUserLogsIndex();
        Random rnd = seed == null ? new Random() : new Random(seed);

        List<UserLogDoc> logs = new ArrayList<>(events);
        for (int i = 0; i < events; i++) {
            String userId = "u" + (1 + rnd.nextInt(Math.max(userCount, 1)));
            String eventType = pick(rnd, EVENT_TYPES);
            Instant ts = Instant.now().minus(rnd.nextInt(30 * 24 * 60), ChronoUnit.MINUTES); // 최근 30일
            String ip = "192.168." + rnd.nextInt(256) + "." + rnd.nextInt(256);
            String ua = pick(rnd, USER_AGENTS);
            Map<String, Object> meta = Map.of("rnd", rnd.nextInt(1_000_000));
            logs.add(new UserLogDoc(userId, eventType, ts, ip, ua, meta));
        }
        int inserted = bulkIndexUserLogs(logs);
        return new SeedResult(true, "user_logs=" + inserted);
    }

    /** (신규) posts {count}건 생성. authorId는 u1..u{users} 중 랜덤 */
    public SeedResult seedPosts(int users, int count, Long seed) throws IOException {
        ensurePostsIndex();
        Random rnd = seed == null ? new Random() : new Random(seed);

        List<PostDoc> posts = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String id = "p" + i; // 고정 아이디. 충돌 피하려면 UUID.randomUUID().toString()
            String authorId = "u" + (1 + rnd.nextInt(Math.max(users, 1)));
            List<String> tags = pickN(rnd, SAMPLE_TAGS, 2 + rnd.nextInt(2)); // 2~3개
            String main = tags.get(0);
            String title = "On " + main + " and " + (tags.size() > 1 ? tags.get(1) : "Music");
            String excerpt = "Sample excerpt about " + String.join(", ", tags) + " (#" + i + ")";
            int likeCount = rnd.nextInt(500);
            Instant createdAt = Instant.now().minus(rnd.nextInt(60 * 24 * 60), ChronoUnit.MINUTES); // 최근 60일

            posts.add(new PostDoc(id, title, excerpt, tags, authorId, likeCount, createdAt));
        }

        int inserted = bulkIndexPosts(posts);
        return new SeedResult(true, "posts=" + inserted);
    }

    // ====== Bulk helpers ======

    private int bulkIndexUsers(List<UserDoc> docs) throws IOException {
        int total = 0;
        for (int from = 0; from < docs.size(); from += 1000) {
            int to = Math.min(from + 1000, docs.size());
            var br = new BulkRequest.Builder().refresh(Refresh.WaitFor);
            for (UserDoc d : docs.subList(from, to)) {
                br.operations(op -> op.index(idx -> idx
                        .index(USERS_INDEX)
                        .id(d.id())
                        .document(d)
                ));
            }
            BulkResponse resp = es.bulk(br.build());
            logBulkErrors("seedUsers", resp);
            total += (to - from);
        }
        return total;
    }

    private int bulkIndexUserLogs(List<UserLogDoc> logs) throws IOException {
        int total = 0;
        for (int from = 0; from < logs.size(); from += 1000) {
            int to = Math.min(from + 1000, logs.size());
            var br = new BulkRequest.Builder().refresh(Refresh.WaitFor);
            for (UserLogDoc d : logs.subList(from, to)) {
                br.operations(op -> op.index(idx -> idx
                        .index(USER_LOGS_INDEX)
                        .document(d)
                ));
            }
            BulkResponse resp = es.bulk(br.build());
            logBulkErrors("seedUserLogs", resp);
            total += (to - from);
        }
        return total;
    }

    private int bulkIndexPosts(List<PostDoc> posts) throws IOException {
        int total = 0;
        for (int from = 0; from < posts.size(); from += 1000) {
            int to = Math.min(from + 1000, posts.size());
            var br = new BulkRequest.Builder().refresh(Refresh.WaitFor);
            for (PostDoc d : posts.subList(from, to)) {
                br.operations(op -> op.index(idx -> idx
                        .index(POSTS_INDEX)
                        .id(d.id()) // id 자동 생성 원하면 제거
                        .document(d)
                ));
            }
            BulkResponse resp = es.bulk(br.build());
            logBulkErrors("seedPosts", resp);
            total += (to - from);
        }
        return total;
    }

    private void logBulkErrors(String tag, BulkResponse resp) {
        if (resp.errors()) {
            for (BulkResponseItem item : resp.items()) {
                if (item.error() != null) {
                    log.warn("[{}] error id={}, reason={}", tag, item.id(), item.error().reason());
                }
            }
        }
    }

    // ====== Index ensure (mapping) ======

    private void ensureUsersIndex() throws IOException {
        var exists = es.indices().exists(ExistsRequest.of(b -> b.index(USERS_INDEX)));
        if (Boolean.TRUE.equals(exists.value())) return;

        es.indices().create(CreateIndexRequest.of(b -> b
                .index(USERS_INDEX)
                .settings(s -> s.numberOfShards("1").numberOfReplicas("0"))
                .mappings(m -> m
                        .properties("id",        p -> p.keyword(k -> k))
                        .properties("name",      p -> p.keyword(k -> k))
                        .properties("tags",      p -> p.keyword(k -> k))
                        .properties("followers", p -> p.integer(i -> i))
                        .properties("following", p -> p.integer(i -> i))
                        .properties("intro",     p -> p.text(t -> t))
                        .properties("joinedAt",  p -> p.date(d -> d))
                )
        ));
    }

    private void ensureUserLogsIndex() throws IOException {
        var exists = es.indices().exists(ExistsRequest.of(b -> b.index(USER_LOGS_INDEX)));
        if (Boolean.TRUE.equals(exists.value())) return;

        es.indices().create(CreateIndexRequest.of(b -> b
                .index(USER_LOGS_INDEX)
                .settings(s -> s.numberOfShards("1").numberOfReplicas("0"))
                .mappings(m -> m
                        .properties("userId",    p -> p.keyword(k -> k))
                        .properties("eventType", p -> p.keyword(k -> k))
                        .properties("eventTime", p -> p.date(d -> d))
                        .properties("ip",        p -> p.keyword(k -> k))
                        .properties("userAgent", p -> p.keyword(k -> k))
                        .properties("meta",      p -> p.object(o -> o))
                )
        ));
    }

    /** ⬅️ 신규: posts 인덱스 보장 (ElkIndexService와 매핑을 동일하게 맞춤) */
    private void ensurePostsIndex() throws IOException {
        var exists = es.indices().exists(ExistsRequest.of(b -> b.index(POSTS_INDEX)));
        if (Boolean.TRUE.equals(exists.value())) return;

        es.indices().create(CreateIndexRequest.of(b -> b
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
        ));
    }

    // ====== DTOs ======

    public record UserDoc(
            String id,
            String name,
            List<String> tags,
            Integer followers,
            Integer following,
            String intro,
            Instant joinedAt
    ) {}

    public record UserLogDoc(
            String userId,
            String eventType,
            Instant eventTime,
            String userAgent,
            String ip,
            Map<String, Object> meta
    ) {}

    /** ⬅️ 신규 */
    public record PostDoc(
            String id,
            String title,
            String excerpt,
            List<String> tags,
            String authorId,
            Integer likeCount,
            Instant createdAt
    ) {}

    public record SeedResult(boolean success, String detail) {}

    // ====== Fixtures ======
    private static final List<String> EVENT_TYPES = List.of("SIGNUP","LOGIN","FOLLOW","LOGOUT","TOKEN_REFRESH");
    private static final List<String> SAMPLE_TAGS = List.of("Beethoven","Mozart","Debussy","Romantic","Baroque","Piano","Violin","Symphony");
    private static final List<String> USER_AGENTS = List.of("curl/8.0","seed-client/1.0","MelogApp/0.1","Mozilla/5.0");

    private static <T> T pick(Random rnd, List<T> arr) { return arr.get(rnd.nextInt(arr.size())); }
    private static <T> List<T> pickN(Random rnd, List<T> arr, int n) {
        n = Math.max(0, Math.min(n, arr.size()));
        LinkedHashSet<T> set = new LinkedHashSet<>();
        while (set.size() < n) set.add(pick(rnd, arr));
        return new ArrayList<>(set);
    }
}

