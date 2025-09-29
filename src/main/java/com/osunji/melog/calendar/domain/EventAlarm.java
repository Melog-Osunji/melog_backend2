package com.osunji.melog.calendar.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_alarm",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_alarm_schedule",
                        columnNames = {"event_schedule_id"}
                )
        }
)
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class EventAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** 일정 1건당 알림 1개 */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_schedule_id", nullable = false, unique = true)
    private EventSchedule eventSchedule;

    /** 활성/비활성 */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** 알림 시간 (기본 09:00) */
    @Column(name = "alarm_time", nullable = false)
    private LocalTime alarmTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Status { PENDING, SENT, CANCELLED }

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (alarmTime == null) alarmTime = LocalTime.of(9, 0);
        if (status == null) status = enabled ? Status.PENDING : Status.CANCELLED;
    }

    @PreUpdate
    void preUpdate() { updatedAt = OffsetDateTime.now(); }
}
