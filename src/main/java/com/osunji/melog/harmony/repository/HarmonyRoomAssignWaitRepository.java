package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.entity.HarmonyRoomAssignWait;
import com.osunji.melog.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HarmonyRoomAssignWaitRepository extends JpaRepository<HarmonyRoomAssignWait, UUID> {

	/**
	 * 하모니룸으로 대기 목록 조회
	 */
	Optional<HarmonyRoomAssignWait> findByHarmonyRoom(HarmonyRoom harmonyRoom);

	/**
	 * 하모니룸 ID로 대기 목록 조회xx
	 */
	@Query("SELECT h FROM HarmonyRoomAssignWait h WHERE h.harmonyRoom.id = :harmonyRoomId")
	Optional<HarmonyRoomAssignWait> findByHarmonyRoomId(@Param("harmonyRoomId") UUID harmonyRoomId);

	/**
	 * 사용자가 특정 하모니룸에 가입 신청했는지 확인
	 */
	@Query("SELECT COUNT(w) > 0 FROM HarmonyRoomAssignWait w JOIN w.waitingUsers u " +
		"WHERE w.harmonyRoom = :harmonyRoom AND u = :user")
	boolean existsByHarmonyRoomAndUser(@Param("harmonyRoom") HarmonyRoom harmonyRoom, @Param("user") User user);
}
