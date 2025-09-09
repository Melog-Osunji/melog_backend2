package com.osunji.melog.feed;


import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Getter @Builder
public class FeedItem {
    private final String id;
    private final String title;
    private final String excerpt;
    private final List<String> tags;
    private final String authorId;
    private final Integer likeCount;
    private final LocalDateTime createdAt;
    private final double score;
}
