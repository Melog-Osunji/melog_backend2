package com.osunji.melog.user.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.osunji.melog.user.domain.enums.Platform;
import jakarta.persistence.*;


import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    /**
     * 사용자 ID (고유 UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // UUID 자동 생성
    @Column(columnDefinition = "uuid")
    private UUID id;
    /*
     * 가입 이메일
     */
    @Column( nullable = false)
    private String email;
    /**
     * 가입 플랫폼
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform platform;
    /**
     * 사용자 닉네임
     */
    @Column
    private String nickname;

    /**
     * 프로필 이미지 URL
     */
    @Column
    private String profileImageUrl;

    /**
     * 한 줄 소개
     */
    private String intro;

    @Column
    @NotNull
    private String oidc;

    @Column
    private Boolean active = false;

    //생성자 - 필수 정보로만 이루어짐
    public User(String email, Platform platform) {
        this.email = email;
        this.platform = platform;
    }

    //생성자 - uuid제외 전체 정보로 생성
    public User(String email, Platform platform, String nickname,
        String profileImageUrl, String intro) {
        this.email = email;
        this.platform = platform;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.intro = intro;
    }


    // 메서드 1 - 회원가입용 (최소 정보)
    public static User createNewUser(String oidc, String email, Platform platform) {
        return new User(email, platform);
    }

    // 팩토리 메서드 2 - 닉넴 설정 끝나고 회원가입로직을 짠다면... (전체 정보)
    public static User createUserWithProfile(String email, Platform platform, String nickname,
        String profileImageUrl, String intro) {
        return new User(email, platform, nickname, profileImageUrl, intro);
    }
    // ✅ 프로필 이미지 업데이트 메서드 추가
    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // ✅ 프로필 전체 업데이트 메서드도 추가
    public void updateProfile(String nickname, String intro, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (intro != null) this.intro = intro;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    // ✅ 닉네임만 업데이트
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // ✅ 소개글만 업데이트
    public void updateIntro(String intro) {
        this.intro = intro;
    }
}