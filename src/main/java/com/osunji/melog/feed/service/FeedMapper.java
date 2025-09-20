package com.osunji.melog.feed.service;

import com.osunji.melog.feed.dto.FeedResponse;

import com.osunji.melog.feed.repository.CommentReader;
import com.osunji.melog.feed.repository.PostReader;
import com.osunji.melog.feed.repository.UserReader;
import com.osunji.melog.feed.view.FeedItem;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class FeedMapper {

    public FeedResponse.FeedItem toFeedItemDto(
            FeedItem base,         // ES 추천 후보 (id, title, excerpt …)
            PostReader.PostDetail post,                 // 도메인 게시글 상세
            UserReader.UserProfile author,              // 작성자 프로필
            CommentReader.BestComment bestComment       // 베댓
    ) {
        // 작성 시각 → 몇 시간 전
        Integer createdAgoHours = null;
        LocalDateTime createdAt = Optional.ofNullable(post)
                .map(PostReader.PostDetail::createdAt)
                .orElse(base.getCreatedAt());
        if (createdAt != null) {
            long hours = Duration.between(createdAt, LocalDateTime.now()).toHours();
            createdAgoHours = (int) Math.max(0, Math.min(hours, Integer.MAX_VALUE));
        }

        // 베댓 매핑
        var bestDto = (bestComment == null) ? null : FeedResponse.BestCommentDto.builder()
                .userId(bestComment.userId())
                .content(bestComment.content())
                .build();

        // 게시글 매핑
        var postDto = FeedResponse.PostDto.builder()
                .id(base.getId())
                .title(base.getTitle())
                .content(base.getExcerpt())
                .mediaType(post != null ? post.mediaType() : null)
                .mediaUrl(post != null ? post.mediaUrl() : null)
                .tags(post != null ? post.tags() : base.getTags())
                .createdAgo(createdAgoHours)
                .likeCount(post != null ? post.likeCount() : base.getLikeCount())
                .hiddenUser(null) //  추후 반영(설정 부분 개발 시에)
                .commentCount(post != null ? post.commentCount() : null)
                .bestComment(bestDto)
                .build();

        // 작성자 매핑
        var userDto = (author == null) ? null : FeedResponse.UserDto.builder()
                .id(author.id())
                .nickName(author.nickName())
                .profileImg(author.profileImg())
                .build();

        return FeedResponse.FeedItem.builder()
                .post(postDto)
                .user(userDto)
                .build();
    }
}
