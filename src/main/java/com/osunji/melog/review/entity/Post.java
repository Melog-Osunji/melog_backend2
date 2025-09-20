package com.osunji.melog.review.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.osunji.melog.review.dto.request.PostRequest;
import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    /**
     * 게시물 고유 ID (UUID 자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /**
     * 작성자 (User UUID와 연결)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 게시물 제목
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 게시물 내용
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 미디어 타입 (IMAGE, VIDEO, TEXT, etc.)
     */
    private String mediaType;

    /**
     * 미디어 URL
     */
    private String mediaUrl;

    /**
     * 태그 리스트
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private List<String> tags = new ArrayList<>();

    /**
     * 생성 날짜
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 좋아요를 누른 사용자들 (User.id = UUID)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_likes",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> likes = new ArrayList<>();

    /**
     * 이 글을 숨김 처리한 사용자들 (User.id = UUID)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_hidden_users",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> hiddenUsers = new ArrayList<>();

    // ========== 생성 메서드 ==========

    /**
     * Post 생성 (기본)
     */
    public static Post create(User user, String title, String content,
        String mediaType, String mediaUrl, List<String> tags) {
        Post post = new Post();
        post.user = user;
        post.title = title;
        post.content = content;
        post.mediaType = mediaType;
        post.mediaUrl = mediaUrl;
        post.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        post.createdAt = LocalDateTime.now();
        post.likes = new ArrayList<>();
        post.hiddenUsers = new ArrayList<>();
        return post;
    }

    /**
     * PostRequest.Create로부터 생성
     */
    public static Post create(User user, PostRequest.Create request) {
        return create(
            user,
            request.getTitle(),
            request.getContent(),
            request.getMediaType(),
            request.getMediaUrl(),
            request.getTags()
        );
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 게시물 수정
     */
    public void update(PostRequest.Update request) {
        if (request.getTitle() != null) this.title = request.getTitle();
        if (request.getContent() != null) this.content = request.getContent();
        if (request.getMediaType() != null) this.mediaType = request.getMediaType();
        if (request.getMediaUrl() != null) this.mediaUrl = request.getMediaUrl();
        if (request.getTags() != null) this.tags = new ArrayList<>(request.getTags());
    }

    /**
     * 좋아요 추가
     */
    public void addLike(User user) {
        if (!this.likes.contains(user)) {
            this.likes.add(user);
        }
    }

    /**
     * 좋아요 제거
     */
    public void removeLike(User user) {
        this.likes.remove(user);
    }

    /**
     * 좋아요 개수
     */
    public int getLikeCount() {
        return this.likes.size();
    }

    /**
     * 사용자가 좋아요를 눌렀는지 확인
     */
    public boolean isLikedBy(User user) {
        return this.likes.contains(user);
    }

    /**
     * 숨김 처리 추가
     */
    public void addHiddenUser(User user) {
        if (!this.hiddenUsers.contains(user)) {
            this.hiddenUsers.add(user);
        }
    }

    /**
     * 숨김 처리 제거
     */
    public void removeHiddenUser(User user) {
        this.hiddenUsers.remove(user);
    }

    /**
     * 사용자가 숨김 처리했는지 확인
     */
    public boolean isHiddenBy(User user) {
        return this.hiddenUsers.contains(user);
    }

    /**
     * 좋아요한 사용자들의 닉네임 리스트 (Response용)
     */
    public List<String> getLikedUserNicknames() {
        return this.likes.stream()
            .map(User::getNickname)
            .filter(nickname -> nickname != null)
            .toList();
    }

    /**
     * 숨김 처리한 사용자들의 닉네임 리스트 (Response용)
     */
    public List<String> getHiddenUserNicknames() {
        return this.hiddenUsers.stream()
            .map(User::getNickname)
            .filter(nickname -> nickname != null)
            .toList();
    }
}
