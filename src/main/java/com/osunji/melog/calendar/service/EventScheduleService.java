package com.osunji.melog.calendar.service;

import com.osunji.melog.calendar.domain.EventAlarm;
import com.osunji.melog.calendar.domain.EventSchedule;
import com.osunji.melog.calendar.dto.EventScheduleRequest;
import com.osunji.melog.calendar.dto.EventScheduleResponse;
import com.osunji.melog.calendar.repository.CalendarRepository;
import com.osunji.melog.calendar.repository.EventAlarmRepository;
import com.osunji.melog.calendar.repository.EventScheduleRepository;
import com.osunji.melog.global.dto.ApiMessage;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class EventScheduleService {

    private final CalendarRepository calendarRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final EventAlarmRepository eventAlarmRepository;
    private final EntityManager em; // ← 주입

    @Value("${calendar.alarm.default-time:09:00}")
    private String defaultAlarmTime;

    @Transactional
    public ApiMessage<EventScheduleResponse> saveOrDeleteSchedule(UUID userId, @Valid EventScheduleRequest req) {
        // 1) 널/기본 형식 검증 (문자열 파싱 없음)
        UUID calendarId = req.getEventId();
        LocalDate eventDate = req.getEventDate();

        if (calendarId == null || eventDate == null) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "eventId와 eventDate는 필수입니다.");
        }

        // 2) 공연 존재 확인
        var calendar = calendarRepository.findById(calendarId).orElse(null);
        if (calendar == null) {
            return ApiMessage.fail(HttpStatus.NOT_FOUND.value(), "해당 공연이 존재하지 않습니다.");
        }

        boolean wantSave  = req.isSchedule(); // boolean이면 isXxx()
        boolean wantAlarm = req.isAlarm();

        // 3) 현재 일정 조회
        var existingOpt = eventScheduleRepository
                .findByUser_IdAndCalendar_IdAndEventDate(userId, calendarId, eventDate);

        EventSchedule scheduleEntity = existingOpt.orElse(null);

        if (wantSave) {
            if (scheduleEntity == null) {
                // User 엔티티 프록시 참조 (User.builder() 불필요)
                var userRef = em.getReference(com.osunji.melog.user.domain.User.class, userId);

                scheduleEntity = EventSchedule.builder()
                        .calendar(calendar)
                        .user(userRef)
                        .eventDate(eventDate)
                        .build();

                try {
                    eventScheduleRepository.saveAndFlush(scheduleEntity);
                } catch (DataIntegrityViolationException ignore) {
                    // 동시성으로 이미 생성된 경우 재조회
                    scheduleEntity = eventScheduleRepository
                            .findByUser_IdAndCalendar_IdAndEventDate(userId, calendarId, eventDate)
                            .orElseThrow();
                }
            }

            // 알림 처리
            if (wantAlarm) {
                var alarmTime = parseAlarmTimeOrDefault(req.getAlarmTime(), defaultAlarmTime);

                var alarmOpt = eventAlarmRepository.findByEventSchedule_Id(scheduleEntity.getId());
                if (alarmOpt.isPresent()) {
                    var alarm = alarmOpt.get();
                    alarm.setEnabled(true);
                    alarm.setAlarmTime(alarmTime);
                    alarm.setStatus(EventAlarm.Status.PENDING);
                } else {
                    eventAlarmRepository.save(EventAlarm.builder()
                            .eventSchedule(scheduleEntity)
                            .enabled(true)
                            .alarmTime(alarmTime)
                            .status(EventAlarm.Status.PENDING)
                            .build());
                }
            } else {
                // 알림 off → 단순화: 삭제
                eventAlarmRepository.deleteByEventSchedule_Id(scheduleEntity.getId());
            }

        } else {
            // 삭제 요청: 알림 먼저 정리 후 일정 삭제 (멱등)
            if (scheduleEntity != null) {
                eventAlarmRepository.deleteByEventSchedule_Id(scheduleEntity.getId());
                eventScheduleRepository.delete(scheduleEntity);
            }
        }

        // 4) 최종 상태로 응답
        var finalOpt = eventScheduleRepository
                .findByUser_IdAndCalendar_IdAndEventDate(userId, calendarId, eventDate);

        boolean finalSchedule = finalOpt.isPresent();
        boolean finalAlarm = false;
        String  finalAlarmTime = null;

        if (finalSchedule) {
            var es = finalOpt.get();
            var alarmOpt = eventAlarmRepository.findByEventSchedule_Id(es.getId());
            if (alarmOpt.isPresent() && alarmOpt.get().isEnabled()) {
                finalAlarm = true;
                finalAlarmTime = alarmOpt.get().getAlarmTime().toString().substring(0,5); // "HH:mm"
            }
        }

        var body = EventScheduleResponse.builder()
                .eventId(calendarId)
                .eventDate(eventDate)
                .schedule(finalSchedule)
                .alarm(finalAlarm)
                .alarmTime(finalAlarm ? finalAlarmTime : null)
                .build();

        String msg = finalSchedule
                ? (finalAlarm ? "일정 저장 및 알림 설정 완료" : "일정 저장 완료")
                : "일정 삭제 완료";

        return ApiMessage.success(HttpStatus.OK.value(), msg, body);
    }

    private LocalTime parseAlarmTimeOrDefault(String maybeTime, String def) {
        try {
            if (maybeTime != null && !maybeTime.isBlank()) {
                return LocalTime.parse(maybeTime);       // "HH:mm" or "HH:mm:ss"
            }
        } catch (Exception ignore) {}
        return LocalTime.parse(def);                      // 기본 "09:00"
    }
}
