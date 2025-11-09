package com.osunji.melog.user.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
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
@Table(name = "agreement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agreement {

    /**
     * 사용자 ID (uuid fk)
     */
    @Id
    private UUID userId;

    /**
     * User와 연결
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "userId")
    private User user;

    /**
     * 마케팅 이용약관 동의 여부
     */
    @Column(nullable = false)
    private Boolean marketing;

    /**
     * 약관 동의 일시
     */
    @Column(nullable = false)
    private LocalDate createdAt;

    /**
     * 약관 동의 정보 생성자
     */
    public Agreement(User user, Boolean marketing, LocalDate createdAt) {
        this.user = user;
        this.userId = user.getId();
        this.marketing = marketing;
        this.createdAt = createdAt;
    }


    public static Agreement createAgreement(User user, Boolean marketing) {
        return new Agreement(user, marketing, LocalDate.now());
    }

    // 변경되었을 시 true 반환
    public boolean updateMarketing(boolean next) {
        if (this.marketing == next) return false;
        this.marketing = next;
        return true;
    }


}
