package com.osunji.melog.user;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
     * 온보딩 ID (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String onboardingId;

    /**
     * 연관된 사용자 (FK) / userId = 온보딩 테이블에서의 userID컬럼명
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId",unique = true)
    private User user;

    /**
     * 선호 작곡가 목록
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "composers",
        joinColumns = @JoinColumn(name = "onboardingId")
    )
    private List<String> composers;

    /**
     * 선호 시대 목록
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "periods",
        joinColumns = @JoinColumn(name = "onboardingId")
    )
    private List<String> periods;

    /**
     * 선호 악기 목록
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "instruments",
        joinColumns = @JoinColumn(name = "onboardingId")
    )
    private List<String> instruments;



    public Onboarding(User user, List<String> composers, List<String> periods, List<String> instruments) {
        this.user = user;
        this.composers = composers;
        this.periods = periods;
        this.instruments = instruments;
    }

    public static Onboarding createOnboarding(User user, List<String> composers, List<String> periods, List<String> instruments) {
        return new Onboarding(user, composers, periods, instruments);
    }


}
