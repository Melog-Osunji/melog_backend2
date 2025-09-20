package com.osunji.melog.feed.repository;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CommentReader {
    record BestComment(UUID postId, UUID userId, String content) {}
    Map<UUID, BestComment> batchFindBestByPostIds(List<UUID> postIds);
}