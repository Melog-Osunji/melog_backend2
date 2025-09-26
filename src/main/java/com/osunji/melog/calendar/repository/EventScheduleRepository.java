package com.osunji.melog.calendar.repository;


import com.osunji.melog.calendar.domain.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventScheduleRepository extends JpaRepository<EventSchedule, UUID> {

    // 단일 날짜 존재/삭제 (이미 사용 중)
    Optional<EventSchedule> findByUser_IdAndCalendar_IdAndEventDate(UUID userId, UUID calendarId, LocalDate eventDate);
    void deleteByUser_IdAndCalendar_IdAndEventDate(UUID userId, UUID calendarId, LocalDate eventDate);

    // ✅ 기간 조회: [from, to] 사이에 있는 내 일정들
    List<EventSchedule> findByUser_IdAndEventDateBetween(UUID userId, LocalDate from, LocalDate to);

    // 필요 시 캘린더별 기간 조회
    List<EventSchedule> findByUser_IdAndCalendar_IdAndEventDateBetween(UUID userId, UUID calendarId, LocalDate from, LocalDate to);
}


