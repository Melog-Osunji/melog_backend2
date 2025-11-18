package com.osunji.melog.calendar.repository;


import com.osunji.melog.calendar.domain.Calendar;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {
    Optional<Calendar> findBySourceAndExternalId(String source, String externalId);


    /**
     * 아직 종료되지 않은 일정만 조회
     * classification이 null이면 전체 조회
     */
    @Query("""
        SELECT c
        FROM Calendar c
        WHERE (:classification IS NULL OR c.classification = :classification)
          AND (c.endDate IS NULL OR c.endDate >= :today)
        ORDER BY c.startDate ASC, c.createdAt DESC
        """)
    List<Calendar> findActive(
            @Param("classification") String classification,
            @Param("today") LocalDate today,
            PageRequest pageable
    );


    /**
     * 오래된 데이터 삭제 (선택)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Calendar c WHERE c.endDate < :threshold")
    void deleteExpired(@Param("threshold") LocalDate threshold);
}

