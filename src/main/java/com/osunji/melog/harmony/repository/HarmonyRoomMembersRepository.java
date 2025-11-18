package com.osunji.melog.harmony.repository;

import com.osunji.melog.harmony.entity.HarmonyRoomMembers;
import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HarmonyRoomMembersRepository extends JpaRepository<HarmonyRoomMembers, UUID> {

	/**
	 * 하모니룸 멤버 조회
	 */
	List<HarmonyRoomMembers> findByHarmonyRoom(HarmonyRoom harmonyRoom);

	/**
	 * 사용자가 속한 하모니룸 조회d
	 */
	List<HarmonyRoomMembers> findByUser(User user);

	/**
	 * 멤버 여부 확인
	 */
	boolean existsByHarmonyRoomAndUser(HarmonyRoom harmonyRoom, User user);

	/**
	 * 하모니룸 멤버 수 조회
	 */
	@Query("SELECT COUNT(m) FROM HarmonyRoomMembers m WHERE m.harmonyRoom = :harmonyRoom")
	Long countByHarmonyRoom(@Param("harmonyRoom") HarmonyRoom harmonyRoom);

	/**
	 * 사용자의 하모니룸 멤버십 삭제
	 */
	void deleteByHarmonyRoomAndUser(HarmonyRoom harmonyRoom, User user);
}
