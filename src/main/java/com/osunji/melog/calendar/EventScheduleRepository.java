package com.osunji.melog.calendar;


import com.osunji.melog.calendar.domain.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, UUID> {

    @Query("""
        select es
        from EventSchedule es
        join fetch es.calendar c
        where es.user.id = :userId
          and c.endDate >= :fromDate  
          and c.startDate <= :toDate  
    """)
    List<EventSchedule> findByUserAndOverlappingRange(UUID userId, LocalDate fromDate, LocalDate toDate);
}

