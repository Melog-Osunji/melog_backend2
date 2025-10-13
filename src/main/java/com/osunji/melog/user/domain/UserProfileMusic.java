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
//                @Index(name = "idx_upm_user_active", columnList = "user_id,is_active")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "title")
    private String title;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    // 생성 팩토리
    public static UserProfileMusic select(User user, String url, String title, String description, String thumbnailUrl) {
        UserProfileMusic upm = new UserProfileMusic();
        upm.user = user;
       upm.youtubeUrl = url;
        upm.title = title;
        upm.description = description;
        upm.thumbnailUrl = thumbnailUrl;
       upm.selectedAt = LocalDateTime.now();
        return upm;
    }

    public void change(String url, String title, String thumbnailUrl, String description) {
        this.youtubeUrl = url;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.description = description;
        this.selectedAt = LocalDateTime.now();
    }
}

