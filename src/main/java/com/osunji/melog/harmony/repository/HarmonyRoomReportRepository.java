package com.osunji.melog.harmony.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.osunji.melog.harmony.entity.HarmonyRoomReport;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface HarmonyRoomReportRepository extends JpaRepository<HarmonyRoomReport, UUID> {

	@Query("SELECT COUNT(hr) FROM HarmonyRoomReport hr WHERE hr.harmonyRoom.id = :harmonyRoomId")
	Long countByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);

	@Query("SELECT CASE WHEN COUNT(hr) > 0 THEN true ELSE false END FROM HarmonyRoomReport hr " +
		"WHERE hr.reporter.id = :reporterId AND hr.harmonyRoom.id = :harmonyRoomId")
	boolean existsByReporterIdAndHarmonyRoomId(@Param("reporterId") UUID reporterId,
		@Param("harmonyRoomId") UUID harmonyRoomId);

	@Query("SELECT hr FROM HarmonyRoomReport hr WHERE hr.harmonyRoom.id = :harmonyRoomId ORDER BY hr.reportedAt DESC")
	List<HarmonyRoomReport> findByHarmonyRoomIdOrderByReportedAtDesc(@Param("harmonyRoomId") UUID harmonyRoomId);
}
