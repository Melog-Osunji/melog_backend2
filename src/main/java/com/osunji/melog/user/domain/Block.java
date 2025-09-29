package com.osunji.melog.user.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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


@Entity
@Table(
        name = "block",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 차단을 하는 사람 (나) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    /** 차단 당하는 사람 (상대) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    /** 차단 시각 */
    @Column(nullable = false)
    private LocalDateTime blockedAt;

    // 생성자
    private Block(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.blockedAt = LocalDateTime.now();
    }

    // 팩토리 메서드
    public static Block of(User blocker, User blocked) {
        return new Block(blocker, blocked);
    }
    public static Block create(User blocker, User blocked, LocalDateTime now) {
        Block b = new Block();
        b.blocker = blocker;
        b.blocked  = blocked;
        b.blockedAt = now;
        return b;
    }
}

