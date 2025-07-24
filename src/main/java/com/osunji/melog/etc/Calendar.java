package com.osunji.melog.etc;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calendars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Calendar {

    /**
     * 공연 정보 고유 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    /**
     * 공연제목
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * 분류
     */
    @Column(name = "classification")
    private String classification;

    /**
     * 지역
     */
    @Column(name = "region")
    private String region;

    /**
     * 공연 시작일
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 공연 종료일
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * 상세 설명
     */
    @Lob
    @Column(name = "description")
    private String description;

    /**
     * 공연 대표 이미지 URL
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * 공연 정보 생성 일시
     */
    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    /**
     * 공연 정보 생성자
     */
    public Calendar(String title, String classification, String region,
        LocalDate startDate, LocalDate endDate, String description, String imageUrl) {
        this.title = title;
        this.classification = classification;
        this.region = region;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDate.now();
    }

    /**
     * 기본 공연 정보 생성 메서드
     */
    public static Calendar createEvent(String title, String classification, String region, LocalDate startDate) {
        return new Calendar(title, classification, region, startDate, null, null, null);
    }

    /**
     * 상세 공연 정보 생성  메서드
     */
    public static Calendar createDetailedEvent(String title, String classification, String region,
        LocalDate startDate, LocalDate endDate, String description, String imageUrl) {
        return new Calendar(title, classification, region, startDate, endDate, description, imageUrl);
    }

}
