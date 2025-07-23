package com.osunji.melog.user;

import java.io.Serializable;
import java.time.LocalDateTime;

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

@Entity
@Table(name = "follows")
@IdClass(Follow.FollowId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

    /**
     * 팔로우 하는 사람 == 나
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id")
    private User follower;

    /**
     * 팔로우 당하는 사람 == 상대
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id")
    private User following;

    /**
     * 팔로우 시작 시각
     */
    @Column(name = "followed_at", nullable = false)
    private LocalDateTime followedAt;

    /**
     * 팔로우 상태
     */
    @Column(nullable = false)
    private String status;

    /**
     * 팔로우 관계 생성자
     */
    public Follow(User follower, User following, String status) {
        this.follower = follower;
        this.following = following;
        this.status = status;
        this.followedAt = LocalDateTime.now();
    }

    public static Follow createFollow(User follower, User following) {
        return new Follow(follower, following, "ACTIVE");
    }

    /**
     * 팔로우 관계의 복합
     */
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class FollowId implements Serializable {
        private String follower;
        private String following;

        public FollowId(String follower, String following) {
            this.follower = follower;
            this.following = following;
        }
    }
}
