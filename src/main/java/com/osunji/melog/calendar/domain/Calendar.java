package com.osunji.melog.calendar.domain;

import java.time.LocalDate;

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
    private String id; // 가능하면 UUID 타입 권장: private UUID id;


    @Column(name = "source", nullable = false, length = 40)
    private String source;           // ex) KCISA_CNV_060

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;       // ex) 59125

    @Column(name = "detail_url", length = 1000)
    private String detailUrl;        // ex) ...cultureView.jsp?pSeq=59125

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

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    // 기존 생성자 확장
    public Calendar(String source, String externalId, String detailUrl,
                    String title, String classification,
                    LocalDate startDate, LocalDate endDate, String description, String imageUrl) {
        this.source = source;
        this.externalId = externalId;
        this.detailUrl = detailUrl;
        this.title = title;
        this.classification = classification;
        this.region = region;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDate.now();
    }

    // KCISA 전용 팩토리 (편의)
    public static Calendar fromKcisa(String externalId, String detailUrl,
                                     String title, String region,
                                     LocalDate startDate, LocalDate endDate,
                                     String description, String imageUrl) {
        return new Calendar(
                "KCISA_CNV_060",
                externalId,
                detailUrl,
                title,
                region,
                startDate,
                endDate,
                description,
                imageUrl
        );
    }
}
