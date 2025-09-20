package com.osunji.melog.review.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_bookmarks")  // 테이블명 일관성 개선
@IdClass(PostBookmark.PostBookmarkId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBookmark {

    /**
     * 북마크한 사용자 (UUID)
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 북마크된 게시물 (UUID)
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    /**
     * 북마크 생성 일시
     */
    @Column(nullable = false)
    private LocalDate createdAt;

    /**
     * 북마크 생성자
     */
    public PostBookmark(User user, Post post) {
        this.user = user;
        this.post = post;
        this.createdAt = LocalDate.now();
    }

    /**
     * 북마크 생성 팩토리 메서드
     */
    public static PostBookmark createBookmark(User user, Post post) {
        return new PostBookmark(user, post);
    }

    /**
     * 복합키 클래스 (UUID 기반으로 수정)
     */
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class PostBookmarkId implements Serializable {
        private UUID user; // ✅ User.id가 UUID이므로 UUID로 변경
        private UUID post; // ✅ Post.id가 UUID이므로 UUID로 변경

        public PostBookmarkId(UUID user, UUID post) {
            this.user = user;
            this.post = post;
        }
    }
}
