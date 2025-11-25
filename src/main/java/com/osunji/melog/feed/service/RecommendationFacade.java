package com.osunji.melog.feed.service;

import com.osunji.melog.feed.dto.FeedResponse;
import com.osunji.melog.feed.repository.CommentReader;
import com.osunji.melog.feed.repository.PostReader;
import com.osunji.melog.feed.repository.UserReader;
import com.osunji.melog.feed.view.FeedItem;
import com.osunji.melog.review.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

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
                .map(FeedItem::getId) // String (postId)
                .map(RecommendationFacade::tryParseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<UUID> authorIds = items.stream()
                .map(FeedItem::getAuthorId) // String (userId)
                .filter(Objects::nonNull)
                .map(RecommendationFacade::tryParseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3) 도메인 배치 조회
        // Map<postId, Post 또는 PostDetail>
        var postMap = postReader.batchFindDetails(postIds);
        // Map<userId, UserProfile>
        var userMap = userReader.batchFindProfiles(authorIds);
        // Map<postId, BestComment>
        var bestMap = commentReader.batchFindBestByPostIds(postIds);

        // 4) 숨김 필터 + DTO 매핑
        var result = items.stream()
                // 🔥 현재 로그인 유저(userId) 기준으로 숨김 처리된 게시물은 제외
                .filter(i -> {
                    if (userId == null) {
                        // 비로그인이라면 숨김 개념이 없으니 필터링 X
                        return true;
                    }
                    UUID postId = tryParseUuid(i.getId());
                    if (postId == null) {
                        return false;
                    }

                    Object detail = postMap.get(postId);
                    if (detail == null) {
                        // 도메인 정보가 없으면 안전하게 제외
                        return false;
                    }

                    // PostReader가 Post 엔티티를 그대로 반환한다고 가정
                    if (detail instanceof Post post) {
                        return !post.isHiddenByUserId(userId);
                    }

                    // 만약 PostDetail 같은 DTO라면,
                    // 그 타입에 isHiddenByUserId(UUID) 같은 헬퍼를 추가해도 됨
                    // 예: return !((PostDetail) detail).isHiddenByUserId(userId);

                    return true; // 타입 모르면 일단 통과 (필요시 좁혀서 수정)
                })
                .map(i -> feedMapper.toFeedItemDto(
                        i,
                        postMap.get(tryParseUuid(i.getId())),
                        userMap.get(tryParseUuid(i.getAuthorId())),
                        bestMap.get(tryParseUuid(i.getId()))
                ))
                .toList();

        return FeedResponse.builder()
                .results(result)
                .build();
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
