package com.osunji.melog.feed.service;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.osunji.melog.feed.view.FeedItem;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FeedService {

    private static final String POSTS_INDEX = "posts";

    private final ElasticsearchOperations esOps;
    private final UserSignalService signalService;
    private final PostRepository postRepository;

    private final Double tag;       // function weight
    private final Double followee;  // function weight
    private final String scale;     // "7d" 같은 문자열

    public FeedService(
            ElasticsearchOperations esOps,
            UserSignalService signalService,
            PostRepository postRepository,
            @Value("${recommend.boost.tag}") Double tag,
            @Value("${recommend.boost.followee}") Double followee,
            @Value("${recommend.fresh.scale}") String scale
    ) {
        this.esOps = esOps;
        this.signalService = signalService;
        this.postRepository = postRepository;
        this.tag = tag;
        this.followee = followee;
        this.scale = scale;
    }

    public List<FeedItem> recommend(UUID userId, int size, List<String> seenIds) {

        // 0) 유저 시그널 (태그/팔로잉) 수집
        var sig       = signalService.build(userId);
        var tags      = sig.getTopTags();
        var followees = sig.getFolloweeIds();

        log.debug("[FeedService] recommend start: userId={}, size={}, seenIdsSize={}",
                userId, size, seenIds != null ? seenIds.size() : 0);
        log.debug("[FeedService] user signals: tags={}, followees={}", tags, followees);

        // 1) 베이스 쿼리: 추천이므로 일단 전체 문서 대상
        Query baseQuery = MatchAllQuery.of(m -> m)._toQuery();

        // 2) function_score functions 구성
        var functions = new ArrayList<FunctionScore>();

        // 2-1) 태그 부스팅 (필요시 "tags.keyword" 로 변경)
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

        // 2-2) 팔로잉 부스팅
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

        // 2-3) 신선도(작성 시각) 가우시안 데케이
        functions.add(FunctionScore.of(fs -> fs.gauss(g -> g
                .date(d -> d
                        .field("createdAt")
                        .placement(p -> p
                                .origin("now")
                                .scale(Time.of(t -> t.time(scale))) // 예: "7d"
                                .decay(0.5)
                        )
                )
        )));

        // 2-4) 인기(좋아요 수) 가중치
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

        // 4) ES 검색 실행
        int maxResults = Math.max(size * 5, 100);
        NativeQuery nq = new NativeQueryBuilder()
                .withQuery(functionScore)
                .withMaxResults(maxResults)
                .build();

        log.debug("[FeedService] executing ES search: index={}, maxResults={}", POSTS_INDEX, maxResults);

        SearchHits<Document> hits = esOps.search(nq, Document.class, IndexCoordinates.of(POSTS_INDEX));

        log.debug("[FeedService] ES search done. totalHits={}", hits.getTotalHits());
        for (SearchHit<Document> hit : hits) {
            log.debug("[FeedService] ES hit: id={}, score={}", hit.getId(), hit.getScore());
        }

        // 5) id + score만 뽑기 (seenIds 제외, 중복 제거)
        Set<String> seen = seenIds != null ? new HashSet<>(seenIds) : Set.of();
        LinkedHashMap<String, Float> idToScore = new LinkedHashMap<>();

        for (SearchHit<Document> hit : hits) {
            String id = hit.getId();
            if (id == null) continue;
            if (seen.contains(id)) {
                log.debug("[FeedService] skip hit(id={}): already seen", id);
                continue;
            }
            if (idToScore.containsKey(id)) continue;
            idToScore.put(id, hit.getScore());
        }

        log.debug("[FeedService] candidates after seen-filter: size={}", idToScore.size());

        if (idToScore.isEmpty()) {
            log.debug("[FeedService] no candidates, return empty list.");
            return List.of();
        }

        // 6) DB에서 Post 조회
        int candidateSize = Math.max(size * 3, 30);
        List<String> candidateIds = idToScore.keySet().stream()
                .limit(candidateSize)
                .toList();

        List<UUID> uuidList = candidateIds.stream()
                .map(UUID::fromString)
                .toList();

        List<Post> posts = postRepository.findAllById(uuidList);
        log.debug("[FeedService] loaded posts from DB: requestedIds={}, loadedSize={}",
                candidateIds.size(), posts.size());

        Map<UUID, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> ranked = candidateIds.stream()
                .map(id -> postMap.get(UUID.fromString(id)))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(
                        (Post p) -> Optional.ofNullable(idToScore.get(p.getId().toString())).orElse(0f)
                ).reversed())
                .toList();

        log.debug("[FeedService] ranked posts size={}", ranked.size());

        // 7) 다양화 적용
        List<Post> diversified = diversify(
                ranked,
                size,
                p -> p.getUser().getId().toString(),
                this::firstTagOrNull
        );

        log.debug("[FeedService] diversified result size={}", diversified.size());

        // 8) 최종 매핑
        List<FeedItem> result = diversified.stream()
                .map(p -> {
                    Float score = idToScore.getOrDefault(p.getId().toString(), 0f);
                    return FeedItem.builder()
                            .id(p.getId().toString())
                            .title(p.getTitle())
                            .excerpt(snippet(p.getContent()))
                            .tags(extractTagNames(p))
                            .authorId(p.getUser().getId().toString())
                            .likeCount(p.getLikeCount())
                            .createdAt(p.getCreatedAt())
                            .score(score.doubleValue())
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("[FeedService] recommend end: final size={}", result.size());
        return result;
    }

    // ===== helpers =====

    private static String snippet(String s) {
        if (s == null) return "";
        return s.length() <= 140 ? s : s.substring(0, 140) + "…";
    }

    private String firstTagOrNull(Post post) {
        List<String> tags = extractTagNames(post);
        if (tags == null || tags.isEmpty()) return null;
        return tags.get(0);
    }

    private List<String> extractTagNames(Post post) {
        return post.getTags(); // Post.getTags() 가 List<String> 이라고 가정
    }

    @SafeVarargs
    private static <T> List<T> diversify(List<T> sorted, int size,
                                         Function<T, String>... keys) {

        var counts = new ArrayList<HashMap<String, Integer>>();
        for (int i = 0; i < keys.length; i++) counts.add(new HashMap<>());

        var out = new ArrayList<T>();

        for (T item : sorted) {
            boolean ok = true;

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i].apply(item);
                if (key == null) continue;
                if (counts.get(i).getOrDefault(key, 0) >= 2) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                out.add(item);
                for (int i = 0; i < keys.length; i++) {
                    String key = keys[i].apply(item);
                    if (key != null) counts.get(i).merge(key, 1, Integer::sum);
                }
            }
            if (out.size() >= size) break;
        }

        int idx = 0;
        while (out.size() < size && idx < sorted.size()) {
            T s2 = sorted.get(idx++);
            if (!out.contains(s2)) out.add(s2);
        }
        return out;
    }

    /**
     * 현재 로그인 유저 기준으로 숨김 처리된 게시물 제거
     */
    public List<Post> filterHiddenPostsForUser(List<Post> posts, UUID userId) {
        if (posts == null || posts.isEmpty() || userId == null) {
            return posts;
        }

        return posts.stream()
                .filter(post -> !post.isHiddenByUserId(userId))
                .toList();
    }
}
