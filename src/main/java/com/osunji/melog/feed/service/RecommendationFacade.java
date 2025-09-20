package com.osunji.melog.feed;

import com.osunji.melog.feed.dto.FeedResponse;
import com.osunji.melog.feed.repository.CommentReader;
import com.osunji.melog.feed.repository.PostReader;
import com.osunji.melog.feed.repository.UserReader;
import com.osunji.melog.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationFacade {

    private final FeedService feedService;     // ES 추천 후보
    private final PostReader postReader;       // 도메인(게시글 상세 배치)
    private final UserReader userReader;       // 도메인(작성자 프로필 배치)
    private final CommentReader commentReader; // 도메인(베댓 배치)
    private final FeedMapper feedMapper;       // DTO 매핑

    public FeedResponse recommend(UUID userId, int size, List<String> seen) {
        // 1) ES에서 1차 후보 뽑기
        var items = feedService.recommend(userId, size, (seen == null) ? List.of() : seen);

        // 2) 배치 키 수집 (String -> UUID 안전 변환)
        List<UUID> postIds = items.stream()
                .map(com.osunji.melog.feed.FeedItem::getId) // String (postId)
                .map(RecommendationFacade::tryParseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> authorIds = items.stream()
                .map(com.osunji.melog.feed.FeedItem::getAuthorId) // String (userId)
                .filter(Objects::nonNull)
                .map(RecommendationFacade::tryParseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3) 도메인 배치 조회
        // Map<postId, PostDetail>
        var postMap = postReader.batchFindDetails(postIds);
        // Map<userId, UserProfile>
        var userMap = userReader.batchFindProfiles(authorIds);
        // Map<postId, BestComment>
        var bestMap = commentReader.batchFindBestByPostIds(postIds);

        // 4) DTO 매핑
        var result = items.stream()
                .map(i -> feedMapper.toFeedItemDto(
                        i,
                        postMap.get(tryParseUuid(i.getId())),
                        userMap.get(tryParseUuid(i.getAuthorId())),
                        bestMap.get(tryParseUuid(i.getId()))
                ))
                .toList();

        return FeedResponse.builder().results(result).build();
    }

    /** UUID 안전 변환 (잘못된 포맷은 null 반환) */
    private static UUID tryParseUuid(String s) {
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
