package com.osunji.melog.calendar.repository;


import com.osunji.melog.calendar.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {  // id 타입 UUID 가정
}

