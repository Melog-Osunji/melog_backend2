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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following")
    private User following;

    @Column(nullable = false)
    private LocalDateTime followedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowStatus status;

    public Follow(User follower, User following, FollowStatus status) {
        this.follower = follower;
        this.following = following;
        this.status = status;
        this.followedAt = LocalDateTime.now();
    }

    public static Follow createFollow(User follower, User following) {
        return new Follow(follower, following, FollowStatus.ACCEPTED);
    }

    public static Follow createFollow(User follower, User following, FollowStatus status) {
        return new Follow(follower, following, status);
    }

    public void activate(LocalDateTime when, FollowStatus status) {
        this.status = status;
        this.followedAt = (when != null) ? when : LocalDateTime.now();
    }

    public void deactivate() {
        this.status = FollowStatus.UNFOLLOW;
    }

    public boolean isUnfollowed() {
        return this.status == null || this.status == FollowStatus.UNFOLLOW;
    }

    /** 차단 */
    public void blockByMe() {
        this.status = FollowStatus.BLOCKED;
    }

    /** 차단 해제 시 BLOCKED → UNFOLLOW 로 되돌리는 역할 */
    public void unblockByMe() {
        if (this.status == FollowStatus.BLOCKED) {
            this.status = FollowStatus.UNFOLLOW;
        }
    }

}
