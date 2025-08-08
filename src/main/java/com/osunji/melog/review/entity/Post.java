package com.osunji.melog.review.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
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
import lombok.Setter;

import com.osunji.melog.user.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "post")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    /**
     * 게시물 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 게시물 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    /**
     * 게시물 제목
     */
    @Lob
    private String title;

    /**
     * 게시물 내용
     */
    @Lob
    private String content;

    /**
     * 미디어 타입
     */
    private String mediaType;

    /**
     * 미디어 링크
     */
    private String mediaLink;

    /**
     * 게시물 태그 목록
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /**
     * 게시물 생성 일시
     */
    @Column(nullable = false)
    private LocalDate createdAt;

    /**
     * 좋아요를 누른 사용자 목록
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "like",
        joinColumns = @JoinColumn(name = "postId"),
        inverseJoinColumns = @JoinColumn(name = "userId")
    )
    private List<User> like= new ArrayList<>();
    /*** 이 게시물을 숨김 처리한 사용자 목록
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "hiddenUser",
        joinColumns = @JoinColumn(name = "postId"),
        inverseJoinColumns = @JoinColumn(name = "userId")
    )
    private List<User> hiddenUser = new ArrayList<>();
    /**
     * 게시물 생성자
     */
    public Post(User user, String title, String content, String mediaType, String mediaLink, List<String> tagList) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.mediaType = mediaType;
        this.mediaLink = mediaLink;
        this.tags = tagList != null && !tagList.isEmpty() ? String.join(",", tagList) : ""; // ✅ List를 문자열로 변환
        this.createdAt = LocalDate.now();
        this.like = new ArrayList<>();
        this.hiddenUser = new ArrayList<>();
    }

    /**
     * 기본 게시물 생성 팩토리 메서드
     */
    public static Post createPost(User user, String title, String content) {
        return new Post(user, title, content, null, null, new ArrayList<>());
    }
    /**
     * 태그 문자열을 List<String>으로 변환해서 반환
     */
    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(tags.split(","));
    }

    /**
     * List<String>을 받아서 쉼표로 구분된 문자열로 저장
     */
    public void setTagList(List<String> tagList) {
        this.tags = tagList != null && !tagList.isEmpty() ? String.join(",", tagList) : "";
    }
    public void updatePost(String title, String content, String mediaType, String mediaLink, List<String> tagList) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (mediaType != null) this.mediaType = mediaType;
        if (mediaLink != null) this.mediaLink = mediaLink;
        if (tagList != null) this.setTagList(tagList);
    }

}
