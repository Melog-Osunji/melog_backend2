package com.osunji.melog.user.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "follow",
    uniqueConstraints = @UniqueConstraint(columnNames = {"follower", "following"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {
    /**
     * 기술적 ID (JPA 요구사항)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    /**
     * 팔로우 하는 사람 == 나
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower")
    private User follower;
    /**
     * 팔로우 당하는 사람 == 상대
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following")
    private User following;

    /**
     * 팔로우 시작 시각
     */
    @Column(nullable = false)
    private LocalDateTime followedAt;

    /**
     * 팔로우 상태
     */
    @Column(nullable = false)
    private Boolean status;

    /**
     * 팔로우 관계 생성자
     */
    public Follow(User follower, User following, Boolean status) {
        this.follower = follower;
        this.following = following;
        this.status = status;
        this.followedAt = LocalDateTime.now();
    }

    public static Follow createFollow(User follower, User following) {
        return new Follow(follower, following,true);
    }

    /** 팔로우 활성화(재팔로우) */
    public void activate(LocalDateTime when) {
        this.status = true;
        this.followedAt = (when != null) ? when : LocalDateTime.now();
    }

    /** 팔로우 비활성화(언팔로우) */
    public void deactivate() {
        this.status = false;
    }

}
