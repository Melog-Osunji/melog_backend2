package com.osunji.melog.user.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.osunji.melog.user.domain.enums.FollowStatus;
import jakarta.persistence.*;

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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowStatus status;

    /**
     * 팔로우 관계 생성자
     */
    public Follow(User follower, User following, FollowStatus status) {
        this.follower = follower;
        this.following = following;
        this.status = status;
        this.followedAt = LocalDateTime.now();
    }

    public static Follow createFollow(User follower, User following) {
        return new Follow(follower, following,FollowStatus.ACCEPTED);
    }

    /** 팔로우 활성화(재팔로우) */
    public void activate(LocalDateTime when) {
        this.status = FollowStatus.ACCEPTED;
        this.followedAt = (when != null) ? when : LocalDateTime.now();
    }

    /** 팔로우 비활성화(언팔로우) */
    public void deactivate() {
        this.status = FollowStatus.UNFOLLOW;
    }

}
