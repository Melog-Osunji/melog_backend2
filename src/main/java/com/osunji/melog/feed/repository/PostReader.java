package com.osunji.melog.feed.repository;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public interface PostReader {
    // 필요한 것만 담은 도메인 DTO
    record PostDetail(UUID id, String mediaType, String mediaUrl,
                      List<String> tags, Integer likeCount, Integer commentCount,
                      LocalDateTime createdAt) {}

    Map<UUID, PostDetail> batchFindDetails(List<UUID> postIds);
}
