package com.osunji.melog.feed.reader;

import com.osunji.melog.feed.repository.PostCommentRepository;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PostReader implements com.osunji.melog.feed.repository.PostReader {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;

    @Transactional(readOnly = true)
    @Override
    public Map<UUID, com.osunji.melog.feed.repository.PostReader.PostDetail> batchFindDetails(List<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) return Collections.emptyMap();

        // 1) 게시글 배치 조회
        List<Post> posts = postRepository.findAllByIdIn(postIds);

        // 2) 댓글 수 배치 집계
        Map<UUID, Integer> commentCountMap = postCommentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentRepository.CountView::getPostId,
                        v -> safeToInt(v.getCnt())
                ));

        // 3) 매핑: 좋아요 수는 컬렉션 size, 태그는 JSON 컬럼 그대로
        return posts.stream().collect(Collectors.toMap(
                Post::getId,
                p -> new com.osunji.melog.feed.repository.PostReader.PostDetail(
                        p.getId(),
                        p.getMediaType(),                      // String
                        p.getMediaUrl(),
                        Optional.ofNullable(p.getTags()).orElseGet(List::of),
                        safeSize(p.getLikes()),                // List<User>
                        commentCountMap.getOrDefault(p.getId(), 0),
                        p.getCreatedAt()                       // LocalDateTime
                )
        ));
    }

    private static int safeSize(Collection<?> c) {
        return (c == null) ? 0 : c.size();
    }
    private static int safeToInt(long v) {
        return (v > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) v;
    }
}
