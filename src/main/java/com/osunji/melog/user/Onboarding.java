package com.osunji.melog.user;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "onboarding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Onboarding {

    /**
     * 온보딩 ID (User ID와 동일)
     */
    @Id
    @Column(name = "user_id")
    private String userId;

    /**
     * 연관된 사용자
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 선호 작곡가 목록
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "composers",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "composer")
    private List<String> composers;

    /**
     * 선호 시대 목록
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "eras",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "era")
    private List<String> eras;

    /**
     * 선호 악기 목록
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "instruments",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "instrument")
    private List<String> instruments;



    public Onboarding(User user, List<String> composers, List<String> eras, List<String> instruments) {
        this.user = user;
        this.userId = user.getUserId();
        this.composers = composers;
        this.eras = eras;
        this.instruments = instruments;
    }

    public static Onboarding createOnboarding(User user, List<String> composers, List<String> eras, List<String> instruments) {
        return new Onboarding(user, composers, eras, instruments);
    }


}
