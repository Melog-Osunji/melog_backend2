package com.osunji.melog.user.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profile_music",
        indexes = {
                @Index(name = "idx_upm_user", columnList = "user_id"),
                @Index(name = "idx_upm_user_active", columnList = "user_id,is_active")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "uuid")
    private User user;

    @Column(name = "youtube_video_id", length = 32)
    private String youtubeVideoId; // 선택

    @Column(name = "youtube_url", length = 512)
    private String youtubeUrl;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl; // 선택

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    // 생성 팩토리
    public static UserProfileMusic select(User user, String videoId, String url, String title, String thumbnailUrl) {
        UserProfileMusic upm = new UserProfileMusic();
        upm.user = user;
        upm.youtubeVideoId = videoId;
        upm.youtubeUrl = url;
        upm.title = title;
        upm.thumbnailUrl = thumbnailUrl;
        upm.isActive = true;
        upm.selectedAt = LocalDateTime.now();
        return upm;
    }

    public void deactivate() { this.isActive = false; }
}

