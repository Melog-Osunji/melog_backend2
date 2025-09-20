package com.osunji.melog.feed.dto;

import lombok.Getter;
import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class FeedResponse {
    private List<FeedItem> results;

    @Getter
    @Builder
    public static class FeedItem {
        private PostDto post;
        private UserDto user;
    }

    @Getter
    @Builder
    public static class PostDto {
        private String id;
        private String title;
        private String content;
        private String mediaType;
        private String mediaUrl;
        private List<String> tags;
        private Integer createdAgo;     // "몇시간전작성인지 Integer"
        private Integer likeCount;
        private List<String> hiddenUser;
        private Integer commentCount;
        private BestCommentDto bestComment;
    }

    @Getter
    @Builder
    public static class BestCommentDto {
        private UUID userId;
        private String content;
    }

    @Getter
    @Builder
    public static class UserDto {
        private UUID id;
        private String nickName;
        private String profileImg;
    }
}
