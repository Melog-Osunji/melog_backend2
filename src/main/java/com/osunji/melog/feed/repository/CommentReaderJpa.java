package com.osunji.melog.feed.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CommentReaderJpa implements CommentReader {

    private final PostCommentRepository postCommentRepository;

    @Override
    public Map<UUID, BestComment> batchFindBestByPostIds(List<UUID> postIds) {
        // TODO: PostCommentRepository로 실제 베댓 조회 구현
        return Collections.emptyMap();
    }
}
