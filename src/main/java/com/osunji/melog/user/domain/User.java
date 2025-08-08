package com.osunji.melog.user.domain;

import com.osunji.melog.user.domain.enums.Platform;
import jakarta.persistence.*;


import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "user")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    /**
     * 사용자 ID (고유 UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column
    private String id;
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
    @Lob
    private String intro;

    @Column
    @NotNull
    private String oidc;


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

}