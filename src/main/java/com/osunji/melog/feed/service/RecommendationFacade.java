package com.osunji.melog.feed.service;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.review.mapper.PostMapper;
import com.osunji.melog.review.repository.BookmarkRepository;
import com.osunji.melog.review.repository.CommentRepository;
import com.osunji.melog.review.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationFacade {

    private final FeedService feedService;     // ES 추천 후보
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostMapper postMapper;

    public ApiMessage<FilterPostResponse.FeedList> recommend(UUID userId, int size, List<String> seen) {
        try {
            // 1) ES에서 1차 후보 뽑기
            List<Post> items = feedService.recommend(userId, size, (seen == null) ? List.of() : seen);

            // 2) DTO 형식에 맞춰서 매핑하기
            FilterPostResponse.FeedList response = buildFeedList(items, userId);
            return ApiMessage.success(200, "추천 피드 조회 성공", response);
        } catch (Exception e) {
            log.error("💥 추천 피드 조회 오류: {}", e.getMessage(), e);
            return ApiMessage.fail(500, "추천 피드 조회 실패: " + e.getMessage());
        }
    }

    // 인기 피드 DTO 변환 부분 재사용
    private FilterPostResponse.FeedList buildFeedList(List<Post> posts, UUID userId) {
        log.info("📋 DTO 변환 시작... (size={})", posts.size());

        List<FilterPostResponse.FeedPostData> feedPostList = new ArrayList<>();

        for (Post post : posts) {
            try {
                int likeCount = (post.getLikes() != null) ? post.getLikes().size() : 0;
                log.info("변환 중: {} - {} (좋아요 {}개)", post.getId(), post.getTitle(), likeCount);

                // ✅ 안전한 댓글 조회
                Optional<PostComment> bestCommentOpt = Optional.empty();
                int commentCount = 0;

                try {
                    bestCommentOpt = commentRepository.findBestComment(post.getId());
                    commentCount = commentRepository.countCommentByPostId(post.getId());
                } catch (Exception e) {
                    log.error("    ⚠️ 댓글 조회 실패: {}", e.getMessage());
                }

                PostComment bestComment = bestCommentOpt.orElse(null);

                boolean isLike = false;
                boolean isBookmark = false;
                if (userId != null) {
                    isLike = postRepository.existsLikeByUserIdAndPostId(userId, post.getId());
                    isBookmark = bookmarkRepository.existsByUserIdAndPostId(userId, post.getId());
                }

                FilterPostResponse.FeedPostData feedData =
                        postMapper.toFeedPostData(post, bestComment, commentCount, isLike, isBookmark);

                feedPostList.add(feedData);

                log.info("    ✅ 변환 완료");
            } catch (Exception e) {
                log.error("    ❌ DTO 변환 오류: {}", e.getMessage(), e);
                // 실패한 게시글은 스킵
            }
        }

        log.info("✅ DTO 변환 완료: {}개", feedPostList.size());

        return FilterPostResponse.FeedList.builder()
                .results(feedPostList)
                .build();
    }

}
