package com.osunji.melog.review;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.osunji.melog.user.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "posts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    /**
     * 게시물 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    /**
     * 게시물 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 게시물 제목
     */
    @Lob
    @Column(name = "title")
    private String title;

    /**
     * 게시물 내용
     */
    @Lob
    @Column(name = "content")
    private String content;

    /**
     * 미디어 타입
     */
    @Column(name = "media_type")
    private String mediaType;

    /**
     * 미디어 링크
     */
    @Column(name = "media_link")
    private String mediaLink;

    /**
     * 게시물 태그 목록
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    /**
     * 게시물 생성 일시
     */
    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    /**
     * 좋아요를 누른 사용자 목록
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_likes",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> likedUsers = new ArrayList<>();

    /**
     * 게시물 생성자
     */
    public Post(User user, String title, String content, String mediaType, String mediaLink, List<String> tags) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.mediaType = mediaType;
        this.mediaLink = mediaLink;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.createdAt = LocalDate.now();
    }

    /**
     * 기본 게시물 생성 팩토리 메서드
     */
    public static Post createPost(User user, String title, String content) {
        return new Post(user, title, content, null, null, new ArrayList<>());
    }

}
