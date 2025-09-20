package com.osunji.melog.feed.repository;

import com.osunji.melog.feed.view.CommentCountView;
import com.osunji.melog.review.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface FeedCommentRepository extends JpaRepository<PostComment, UUID> {

    interface CountView {
        UUID getPostId();
        long getCnt();
    }

    @Query("""
           select pc.post.id as postId, count(pc) as cnt
           from PostComment pc
           where pc.post.id in :postIds
           group by pc.post.id
           """)
    List<CountView> countByPostIds(@Param("postIds") List<UUID> postIds);
}
