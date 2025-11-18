package com.osunji.melog.calendar.repository;


import com.osunji.melog.calendar.domain.EventAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventAlarmRepository extends JpaRepository<EventAlarm, UUID> {

    Optional<EventAlarm> findByEventSchedule_Id(UUID eventScheduleId);

    boolean existsByEventSchedule_Id(UUID eventScheduleId);

    int deleteByEventSchedule_Id(UUID eventScheduleId);
}

