package com.osunji.melog.calendar.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "calendars",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_calendar_source_external", columnNames = {"source", "external_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(name = "detail_url", length = 1000)
    private String detailUrl;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "classification", length = 100)
    private String classification;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    // ✅ region 파라미터 추가하고 필드에 대입
    public Calendar(String source,
                    String externalId,
                    String detailUrl,
                    String title,
                    String classification,
                    String region,            // ← 추가
                    LocalDate startDate,
                    LocalDate endDate,
                    String description,
                    String imageUrl) {
        this.source = source;
        this.externalId = externalId;
        this.detailUrl = detailUrl;
        this.title = title;
        this.classification = classification;
        this.region = region;          // ← 이제 정상 대입
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDate.now();
    }

    // ✅ 순서 맞춰서 region 전달
    public static Calendar fromKcisa(String externalId,
                                     String detailUrl,
                                     String title,
                                     String region,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     String description,
                                     String imageUrl) {
        return new Calendar(
                "KCISA_CNV_060",
                externalId,
                detailUrl,
                title,
                /* classification= */ null, // 원한다면 분류값 채워도 됨
                region,
                startDate,
                endDate,
                description,
                imageUrl
        );
    }
}
