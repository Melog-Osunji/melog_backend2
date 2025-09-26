package com.osunji.melog.calendar.domain;


import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_event_schedule_user_calendar_date",
                        columnNames = {"user_id", "calendar_id", "event_date"}
                )
        }
)
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class EventSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** 공연 정보 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    /** 유저 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 일정 날짜 (YYYY-MM-DD) */
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;


}
