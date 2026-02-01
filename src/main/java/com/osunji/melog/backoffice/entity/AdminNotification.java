package com.osunji.melog.backoffice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AdminNotification(String title, String content, String targetType) {
        this.title = title;
        this.content = content;
        this.targetType = targetType;
    }

    public static AdminNotification create(String title, String content, String targetType) {
        return new AdminNotification(title, content, targetType);
    }
}
