package com.osunji.melog.feed.service;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.osunji.melog.elk.entity.PostIndex;
import com.osunji.melog.feed.FeedItem;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// ==== Elasticsearch Java API v8 (co.elastic.clients) – 쿼리/빌더는 전부 이 패키지로! ====
import co.elastic.clients.elasticsearch._types.FieldValue;

@Service
public class FeedService {
    private static final String POSTS_INDEX = "posts";

    private final ElasticsearchOperations esOps;
    private final UserSignalService signalService;

    // 가중치/스케일 설정
    private final Double tag;         // function weight
    private final Double followee;    // function weight
    private final String scale;       // ← 데케이 스케일은 "7d" 같은 문자열!

    public FeedService(
            ElasticsearchOperations esOps,
            UserSignalService signalService,
            @Value("${recommend.boost.tag}") Double tag,
            @Value("${recommend.boost.followee}") Double followee,  // ← 키 수정!
            @Value("${recommend.fresh.scale}") String scale         // ← 타입: String ("7d" 등)
    ) {
        this.esOps = esOps;
        this.signalService = signalService;
        this.tag = tag;
        this.followee = followee;
        this.scale = scale;
    }

    public List<FeedItem> recommend(String userId, int size, List<String> seenIds) {
        var sig       = signalService.build(userId);
        var tags      = sig.getTopTags();
        var followees = sig.getFolloweeIds();

        // 1) 베이스 쿼리
        Query baseQuery =
                (tags != null && !tags.isEmpty())
                        ? MultiMatchQuery.of(m -> m
                        .query(String.join(" ", tags))
                        .fields("title^3", "content")
                )._toQuery()
                        : MatchAllQuery.of(m -> m)._toQuery();

        // 2) function_score functions
        var functions = new ArrayList<FunctionScore>();

        // 태그 부스팅
        if (tags != null && !tags.isEmpty()) {
            Query tagsFilter = TermsQuery.of(t -> t
                    .field("tags")
                    .terms(v -> v.value(tags.stream().map(FieldValue::of).toList()))
            )._toQuery();

            functions.add(FunctionScore.of(fs -> fs
                    .filter(tagsFilter)
                    .weight(tag)
            ));
        }

        // 팔로잉 부스팅
        if (followees != null && !followees.isEmpty()) {
            Query followeesFilter = TermsQuery.of(t -> t
                    .field("userId")
                    .terms(v -> v.value(followees.stream().map(FieldValue::of).toList()))
            )._toQuery();

            functions.add(FunctionScore.of(fs -> fs
                    .filter(followeesFilter)
                    .weight(followee)
            ));
        }

// 신선도(작성시각) 가우시안 데케이
        functions.add(FunctionScore.of(fs -> fs.gauss(g -> g
                .date(d -> d                               // ← DecayFunction.Builder → date(...)
                        .field("createdAt")                    // ← DateDecayFunction.Builder.field(...)
                        .placement(p -> p                      // ← DateDecayFunction.Builder.placement(...)
                                .origin("now")                     // origin: String
                                .scale(Time.of(t -> t.time(scale)))// scale: Time (예: scale="7d")
                                // .offset(Time.of(t -> t.time("0d")))
                                .decay(0.5)
                        )
                )
        )));

        // 인기(좋아요 수) 가산
        functions.add(FunctionScore.of(fs -> fs
                .fieldValueFactor(fvf -> fvf
                        .field("likeCount")
                        .modifier(FieldValueFactorModifier.Log1p)
                        .factor(1.2)
                        .missing(0.0)
                )
        ));

        // 3) function_score 조립
        Query functionScore = FunctionScoreQuery.of(f -> f
                .query(baseQuery)
                .functions(functions)
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Sum)
        )._toQuery();

        // 4) 실행
        NativeQuery nq = new NativeQueryBuilder()
                .withQuery(functionScore)
                .withMaxResults(Math.max(size * 5, 100))  // 1차 후보 넉넉히
                .build();

        var hits = esOps.search(nq, PostIndex.class, IndexCoordinates.of(POSTS_INDEX))
                .getSearchHits();

        // 5) 서버단 정제: seenIds 제거 + 다양화
        var dedup = new HashMap<String, Pair>();
        for (SearchHit<PostIndex> h : hits) {
            var p = h.getContent();
            if (p == null) continue;
            if (seenIds != null && seenIds.contains(p.getId())) continue;
            dedup.putIfAbsent(p.getId(), new Pair(p, h.getScore()));
        }

        var ranked = dedup.values().stream()
                .sorted(Comparator.comparingDouble((Pair p) -> p.score).reversed())
                .limit((long) size * 3)
                .toList();

        var diversified = diversify(ranked, size,
                pair -> pair.post.getUserId(),            // 동일 작성자 제한
                pair -> firstOrNull(pair.post.getTags())  // 대표 태그 제한
        );

        return diversified.stream().map(p -> FeedItem.builder()
                .id(p.post.getId())
                .title(p.post.getTitle())
                .excerpt(snippet(p.post.getContent()))
                .tags(p.post.getTags())
                .authorId(p.post.getUserId())
                .likeCount(p.post.getLikeCount())
                .createdAt(p.post.getCreatedAt())
                .score(p.score)
                .build()
        ).collect(Collectors.toList());
    }

    // ===== helpers =====
    private static String snippet(String s) {
        if (s == null) return "";
        return s.length() <= 140 ? s : s.substring(0, 140) + "…";
    }

    private static String firstOrNull(List<String> xs) {
        return (xs == null || xs.isEmpty()) ? null : xs.get(0);
    }

    @SafeVarargs
    private static List<Pair> diversify(List<Pair> sorted, int size,
                                        java.util.function.Function<Pair, String>... keys) {
        var counts = new ArrayList<HashMap<String, Integer>>();
        for (int i = 0; i < keys.length; i++) counts.add(new HashMap<>());

        var out = new ArrayList<Pair>();
        for (var s : sorted) {
            boolean ok = true;
            for (int i = 0; i < keys.length; i++) {
                var key = keys[i].apply(s);
                if (key == null) continue;
                if (counts.get(i).getOrDefault(key, 0) >= 2) { ok = false; break; } // 키별 최대 2개
            }
            if (ok) {
                out.add(s);
                for (int i = 0; i < keys.length; i++) {
                    var key = keys[i].apply(s);
                    if (key != null) counts.get(i).merge(key, 1, Integer::sum);
                }
            }
            if (out.size() >= size) break;
        }
        int idx = 0;
        while (out.size() < size && idx < sorted.size()) {
            var s2 = sorted.get(idx++);
            if (!out.contains(s2)) out.add(s2);
        }
        return out;
    }

    @AllArgsConstructor
    private static class Pair {
        PostIndex post;
        double score;
    }
}
