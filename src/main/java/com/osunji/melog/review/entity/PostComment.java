package com.osunji.melog.review.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_comments")  // 테이블명 일관성 개선
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostComment {

    /**
     * 댓글 고유 ID (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /**
     * 댓글 작성자 (User UUID와 연결)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 댓글이 속한 게시물 (Post UUID와 연결)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /**
     * 댓글 내용
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 댓글 작성 일시
     */
    @Column(nullable = false)
    private LocalDate createdAt;

    /**
     * 부모 댓글 (대댓글인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private PostComment parentComment;

    /**
     * 자식 댓글 목록 (이 댓글의 대댓글들)
     */
    @OneToMany(mappedBy = "parentComment",
        cascade = CascadeType.ALL,     // ✅ 부모 삭제 시 자식도 삭제
        orphanRemoval = true)          // ✅ 고아 객체 자동 제거
    private List<PostComment> childComments = new ArrayList<>();
    /**
     * 댓글에 좋아요를 누른 사용자 목록
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "comment_likes",
        joinColumns = @JoinColumn(name = "comment_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> likedUsers = new ArrayList<>();

    /**
     * 일반 댓글 생성자
     */
    public PostComment(User user, Post post, String content) {
        this.user = user;
        this.post = post;
        this.content = content;
        this.createdAt = LocalDate.now();
        this.parentComment = null;
        this.likedUsers = new ArrayList<>();
        this.childComments = new ArrayList<>();
    }

    /**
     * 대댓글 생성자
     */
    public PostComment(User user, Post post, String content, PostComment parentComment) {
        this.user = user;
        this.post = post;
        this.content = content;
        this.createdAt = LocalDate.now();
        this.parentComment = parentComment;
        this.likedUsers = new ArrayList<>();
        this.childComments = new ArrayList<>();
    }

    // ========== 정적 팩토리 메서드 ==========

    /**
     * 일반 댓글 생성 메서드
     */
    public static PostComment createComment(User user, Post post, String content) {
        return new PostComment(user, post, content);
    }

    /**
     * 대댓글 생성 메서드
     */
    public static PostComment createReply(User user, Post post, String content, PostComment parentComment) {
        return new PostComment(user, post, content, parentComment);
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 좋아요 추가
     */
    public void addLike(User user) {
        if (!this.likedUsers.contains(user)) {
            this.likedUsers.add(user);
        }
    }

    /**
     * 좋아요 제거
     */
    public void removeLike(User user) {
        this.likedUsers.remove(user);
    }

    /**
     * 좋아요 개수
     */
    public int getLikeCount() {
        return this.likedUsers.size();
    }

    /**
     * 사용자가 좋아요를 눌렀는지 확인
     */
    public boolean isLikedBy(User user) {
        return this.likedUsers.contains(user);
    }

    /**
     * 대댓글인지 확인
     */
    public boolean isReply() {
        return this.parentComment != null;
    }

    /**
     * 자식 댓글(대댓글) 개수
     */
    public int getChildCommentCount() {
        return this.childComments.size();
    }
}
