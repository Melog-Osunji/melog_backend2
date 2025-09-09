package com.osunji.melog.feed;



import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.ScoreMode;
import co.elastic.clients.json.JsonData;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.osunji.melog.elk.entity.PostIndex;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import org.springframework.data.elasticsearch.core.SearchHit;


@Service
@RequiredArgsConstructor
public class FeedService {

    private static final String POSTS_INDEX = "posts";

    // 부스트 가중치(초기값) — 필요시 환경변수/설정으로 뺄 수 있음
    private static final double BOOST_TAG       = 2.0;
    private static final double BOOST_FOLLOWEE  = 3.0;
    private static final String FRESH_SCALE     = "7d"; // 최신성 감쇠 스케일

    private final ElasticsearchOperations esOps;
    private final UserSignalService signalService;

    public List<FeedItem> recommend(String userId, int size, List<String> seenIds) {
        var sig       = signalService.build(userId);
        var tags      = sig.getTopTags();
        var followees = sig.getFolloweeIds();

        // 1) 베이스 쿼리
        Query baseQuery = (tags != null && !tags.isEmpty())
                ? MultiMatchQuery.of(m -> m.query(String.join(" ", tags))
                .fields("title^3", "content"))._toQuery()
                : MatchAllQuery.of(m -> m)._toQuery();

        // 2) function_score functions
        var functions = new ArrayList<FunctionScore>();

        if (tags != null && !tags.isEmpty()) {
            Query tagsFilter = TermsQuery.of(t -> t.field("tags")
                    .terms(v -> v.value(tags.stream().map(FieldValue::of).toList()))
            )._toQuery();
            functions.add(FunctionScore.of(fs -> fs.filter(tagsFilter).weight(BOOST_TAG)));
        }

        if (followees != null && !followees.isEmpty()) {
            Query followeesFilter = TermsQuery.of(t -> t.field("userId")
                    .terms(v -> v.value(followees.stream().map(FieldValue::of).toList()))
            )._toQuery();
            functions.add(FunctionScore.of(fs -> fs.filter(followeesFilter).weight(BOOST_FOLLOWEE)));
        }

        functions.add(FunctionScore.of(fs -> fs.gauss(g -> g
                .field("createdAt")
                .placement(DecayPlacement.of(dp -> dp
                        .origin(JsonData.of("now"))   // 문자열은 JsonData로
                        .scale(JsonData.of(FRESH_SCALE)) // 예: "7d"
                        .decay(0.5)
                ))
        )));

        // 인기 가산 (likeCount)
        functions.add(FunctionScore.of(fs -> fs
                .fieldValueFactor(fvf -> fvf.field("likeCount")
                        .modifier(FieldValueFactorModifier.Log1p)
                        .factor(1.2)
                        .missing(0.0))
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

        // 5) 서버단 정제: seenIds 제거 + 다양화(동일 작성자/대표태그 과다 억제)
        var dedup = new HashMap<String, Pair>();
        for (SearchHit<PostIndex> h : hits) {
            var p = h.getContent();
            if (seenIds != null && seenIds.contains(p.getId())) continue;
            dedup.putIfAbsent(p.getId(), new Pair(p, h.getScore()));
        }

        var ranked = dedup.values().stream()
                .sorted(Comparator.comparingDouble((Pair p) -> p.score).reversed())
                .limit(size * 3L)
                .toList();

        var diversified = diversify(ranked, size,
                pair -> pair.post.getUserId(),                           // 동일 작성자 제한
                pair -> firstOrNull(pair.post.getTags())                 // 대표 태그 제한
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
                .build()).toList();
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
