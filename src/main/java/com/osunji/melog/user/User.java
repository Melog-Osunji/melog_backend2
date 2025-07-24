package com.osunji.melog.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    /**
     * 사용자 ID (== 이메일)
     */
    @Id
    @Column(name = "user_id")
    private String userId;
    /**
     * 가입 플랫폼
     */
    @Column(nullable = false)
    private String platform;
    /**
     * 사용자 닉네임
     */
    @Column(nullable = false, unique = true)
    private String nickname;

    /**
     * 프로필 이미지 URL
     */
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    /**
     * 한 줄 소개
     */
    @Lob
    private String intro;

    public User(String userId, String platform, String nickname, String profileImageUrl, String intro) {
        this.userId = userId;
        this.platform = platform;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.intro = intro;
    }
    /**
     * 기본 정보로 사용자 생성(null)
     */
    public static User createUser(String email, String platform, String nickname) {
        return new User(email, platform, nickname, null, null);
    }
}