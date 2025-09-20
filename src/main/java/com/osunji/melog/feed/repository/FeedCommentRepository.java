package com.osunji.melog.feed.repository;

import com.osunji.melog.feed.view.CommentCountView;
import com.osunji.melog.review.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<PostComment, UUID> {

    @Query("""
           select c.post.id as postId, count(c) as cnt
           from Comment c
           where c.post.id in :postIds
           group by c.post.id
           """)
    List<CommentCountView> countByPostIds(@Param("postIds") List<UUID> postIds);
}
