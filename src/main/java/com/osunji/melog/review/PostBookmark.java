package com.osunji.melog.review;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.osunji.melog.user.User;

@Entity
@Table(name = "postBookmark")
@IdClass(PostBookmark.PostBookmarkId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBookmark {

    /**
     * 북마크한 사용자
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User user;

    /**
     * 북마크된 게시물
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
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

    public static PostBookmark createBookmark(User user, Post post) {
        return new PostBookmark(user, post);
    }
    /**
     * 복합키 클래스
     */
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class PostBookmarkId implements Serializable {
        private String user; // User의 userId 타입과 일치
        private String post; // Post의 id 타입과 일치

        public PostBookmarkId(String user, String post) {
            this.user = user;
            this.post = post;
        }
    }
}
