package com.osunji.melog.user.domain;

import java.time.LocalDate;
import java.util.Objects;
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

    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id") // FK 컬럼명 명확히
    private User user;

    @Column(nullable = false)
    private Boolean marketing;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();

    private Agreement(User user, Boolean marketing) {
        this.user = user;
        this.marketing = marketing;
    }

    public static Agreement createAgreement(User user, Boolean marketing) {
        return new Agreement(user, marketing);
    }

    public boolean updateMarketing(boolean next) {
        if (Objects.equals(this.marketing, next)) return false;
        this.marketing = next;
        return true;
    }
}

