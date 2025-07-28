package com.osunji.melog.review;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.osunji.melog.user.User;

@Entity
@Table(name = "postComment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostComment {

    /**
     * 댓글 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 댓글 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( nullable = false)
    private User user;

    /**
     * 댓글이 속한 게시물
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( nullable = false)
    private Post post;

    /**
     * 댓글 내용
     */
    @Column( nullable = false)
    private String content;

    /**
     * 댓글 작성 일시
     */
    @Column( nullable = false)
    private LocalDate createdAt;

    /**
     * 부모 댓글 (대댓글인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private PostComment parentComment;

    /**
     * 자식 댓글 목록 (이 댓글의 대댓글들)
     */
    @OneToMany(mappedBy = "parentComment")
    private List<PostComment> childComments = new ArrayList<>();

    /**
     * 댓글에 좋아요를 누른 사용자 목록
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "commentLike",
        joinColumns = @JoinColumn(name = "commentId"),
        inverseJoinColumns = @JoinColumn(name = "userId")
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

}
