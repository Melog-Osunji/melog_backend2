package com.osunji.melog.calendar.service;

import com.osunji.melog.calendar.domain.Calendar;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventScheduleService {

    private final CalendarRepository calendarRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final EventAlarmRepository eventAlarmRepository;
    private final EntityManager em;

    @Value("${calendar.alarm.default-time:09:00}")
    private String defaultAlarmTime;

    /**
     * 1️⃣ calendar 존재 여부 확인
     * 2️⃣ 없으면 저장
     * 3️⃣ 일정 토글/알림 토글
     */
    @Transactional
    public ApiMessage<EventScheduleResponse> saveOrDeleteSchedule(UUID userId, @Valid EventScheduleRequest req) {
        UUID calendarId = req.getEventId();
        LocalDate eventDate = req.getEventDate();

        if (calendarId == null || eventDate == null) {
            log.warn("⚠️ 잘못된 요청: userId={}, eventId={}, eventDate={}", userId, calendarId, eventDate);
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "eventId와 eventDate는 필수입니다.");
        }

        log.debug("📝 일정 저장/삭제 요청 수신: userId={}, eventId={}, eventDate={}, schedule={}, alarm={}, alarmTime={}",
                userId, calendarId, eventDate, req.isSchedule(), req.isAlarm(), req.getAlarmTime());

        // ✅ 1) 캘린더 확보(없으면 생성) — 동시성/유니크 처리 포함
        Calendar calendar = ensureCalendar(req);

        boolean wantSave  = req.isSchedule();
        boolean wantAlarm = req.isAlarm();

        // 2) 기존 일정 조회
        var existingOpt = eventScheduleRepository
                .findByUser_IdAndCalendar_IdAndEventDate(userId, calendar.getId(), eventDate);

        EventSchedule scheduleEntity = existingOpt.orElse(null);

        // 이미 일정이 있는데 또 저장 요청이면 409
        if (wantSave && scheduleEntity != null) {
            log.warn("🚫 중복 일정 요청: userId={}, calendarId={}, eventDate={}", userId, calendar.getId(), eventDate);
            return ApiMessage.fail(HttpStatus.CONFLICT.value(), "이미 해당 날짜에 일정이 저장되어 있습니다.");
        }

        if (wantSave) {
            var userRef = em.getReference(com.osunji.melog.user.domain.User.class, userId);

            scheduleEntity = EventSchedule.builder()
                    .calendar(calendar)
                    .user(userRef)
                    .eventDate(eventDate)
                    .build();

            try {
                eventScheduleRepository.saveAndFlush(scheduleEntity);
                log.info("🟢 일정 저장 완료: scheduleId={}, userId={}, calendarId={}, eventDate={}",
                        scheduleEntity.getId(), userId, calendar.getId(), eventDate);
            } catch (DataIntegrityViolationException e) {
                log.warn("🔁 일정 저장 동시성 충돌 -> 재조회: userId={}, calendarId={}, eventDate={}",
                        userId, calendar.getId(), eventDate, e);
                scheduleEntity = eventScheduleRepository
                        .findByUser_IdAndCalendar_IdAndEventDate(userId, calendar.getId(), eventDate)
                        .orElseThrow(() -> {
                            log.error("❌ 일정 재조회 실패(동시성)");
                            return new IllegalStateException("일정 동시성 저장 실패");
                        });
            }

            // 🔔 알림 처리
            if (wantAlarm) {
                var alarmTime = parseAlarmTimeOrDefault(req.getAlarmTime(), defaultAlarmTime);
                var alarmOpt = eventAlarmRepository.findByEventSchedule_Id(scheduleEntity.getId());
                if (alarmOpt.isPresent()) {
                    var alarm = alarmOpt.get();
                    alarm.setEnabled(true);
                    alarm.setAlarmTime(alarmTime);
                    alarm.setStatus(EventAlarm.Status.PENDING);
                    log.debug("🔔 알림 업데이트: scheduleId={}, time={}", scheduleEntity.getId(), alarmTime);
                } else {
                    eventAlarmRepository.save(EventAlarm.builder()
                            .eventSchedule(scheduleEntity)
                            .enabled(true)
                            .alarmTime(alarmTime)
                            .status(EventAlarm.Status.PENDING)
                            .build());
                    log.debug("🔔 알림 신규 저장: scheduleId={}, time={}", scheduleEntity.getId(), alarmTime);
                }
            } else {
                int deleted = eventAlarmRepository.deleteByEventSchedule_Id(scheduleEntity.getId());
                log.debug("🔕 알림 해제: scheduleId={}, deletedRows={}", scheduleEntity.getId(), deleted);
            }

        } else {
            // 삭제 요청
            if (scheduleEntity != null) {
                int deletedAlarms = eventAlarmRepository.deleteByEventSchedule_Id(scheduleEntity.getId());
                eventScheduleRepository.delete(scheduleEntity);
                log.info("🗑️ 일정 삭제 완료: scheduleId={}, deletedAlarms={}", scheduleEntity.getId(), deletedAlarms);
            } else {
                log.debug("ℹ️ 삭제 요청이지만 저장된 일정 없음: userId={}, calendarId={}, eventDate={}",
                        userId, calendar.getId(), eventDate);
            }
        }

        // 3) 최종 상태 응답
        var finalOpt = eventScheduleRepository
                .findByUser_IdAndCalendar_IdAndEventDate(userId, calendar.getId(), eventDate);

        boolean finalSchedule = finalOpt.isPresent();
        boolean finalAlarm = false;
        String finalAlarmTime = null;

        if (finalSchedule) {
            var es = finalOpt.get();
            var alarmOpt = eventAlarmRepository.findByEventSchedule_Id(es.getId());
            if (alarmOpt.isPresent() && alarmOpt.get().isEnabled()) {
                finalAlarm = true;
                finalAlarmTime = alarmOpt.get().getAlarmTime().toString().substring(0, 5);
            }
        }

        var body = EventScheduleResponse.builder()
                .eventId(calendar.getId())
                .eventDate(eventDate)
                .schedule(finalSchedule)
                .alarm(finalAlarm)
                .alarmTime(finalAlarm ? finalAlarmTime : null)
                .build();

        String msg = finalSchedule
                ? (finalAlarm ? "일정 저장 및 알림 설정 완료" : "일정 저장 완료")
                : "일정 삭제 완료";

        log.debug("📤 응답: userId={}, calendarId={}, eventDate={}, schedule={}, alarm={}, alarmTime={}, msg='{}'",
                userId, calendar.getId(), eventDate, finalSchedule, finalAlarm, finalAlarmTime, msg);

        return ApiMessage.success(HttpStatus.OK.value(), msg, body);
    }

//    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private LocalTime parseAlarmTimeOrDefault(String maybeTime, String def) {
        try {
            if (maybeTime != null && !maybeTime.isBlank()) {
                return LocalTime.parse(maybeTime);
            }
        } catch (Exception e) {
            log.warn("⏰ 알림 시각 파싱 실패: input='{}' -> 기본값 '{}' 사용", maybeTime, def, e);
        }
        return LocalTime.parse(def);
    }

    /**
     * Calendar가 존재하지 않으면 새로 생성.
     * KCISA 기반 외부 공연 데이터 또는 기본 placeholder로 삽입.
     */
    private Calendar ensureCalendar(EventScheduleRequest req) {
        UUID calendarId = req.getEventId();

        return calendarRepository.findById(calendarId)
                .orElseGet(() -> {
                    Calendar toSave;

                    // 외부 공연 데이터 (KCISA)
                    if ("KCISA_CNV_060".equalsIgnoreCase(req.getSource())) {
                        toSave = Calendar.fromKcisa(
                                calendarId,
                                req.getExternalId(),
                                req.getDetailUrl(),
                                req.getTitle(),
                                req.getRegion(),
                                req.getStartDate(),
                                req.getEndDate(),
                                req.getDescription(),
                                req.getImageUrl()
                        );
                    } else {
                        // 일반 유저 일정
                        toSave = new Calendar(
                                calendarId,
                                req.getSource() != null ? req.getSource() : "USER",
                                req.getExternalId() != null ? req.getExternalId() : calendarId.toString(),
                                req.getDetailUrl(),
                                req.getTitle() != null ? req.getTitle() : "새 일정",
                                req.getClassification(),
                                req.getRegion(),
                                req.getStartDate() != null ? req.getStartDate() : LocalDate.now(),
                                req.getEndDate(),
                                req.getDescription(),
                                req.getImageUrl()
                        );
                    }

                    try {
                        var saved = calendarRepository.saveAndFlush(toSave);
                        log.info("✅ ensureCalendar: 생성 성공 id={}", saved.getId());
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        log.warn("🔁 ensureCalendar 동시성 충돌 -> 재조회: source={}, externalId={}",
                                toSave.getSource(), toSave.getExternalId(), e);
                        // 유니크 제약으로 이미 삽입된 경우
                        return calendarRepository
                                .findBySourceAndExternalId(toSave.getSource(), toSave.getExternalId())
                                .orElseThrow(() -> {
                                    log.error("❌ ensureCalendar 재조회 실패");
                                    return new IllegalStateException("Calendar 동시성 생성 실패");
                                });
                    }
                });
    }
}
