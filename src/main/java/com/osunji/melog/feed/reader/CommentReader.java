package com.osunji.melog.feed.reader;



import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CommentReader {
    record BestComment(UUID userId, String content) {}
    Map<UUID, BestComment> batchFindBestByPostIds(List<UUID> postIds);
}
