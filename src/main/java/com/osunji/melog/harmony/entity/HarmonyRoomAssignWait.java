package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "harmony_room_assign_wait")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HarmonyRoomAssignWait {

	/**
	 * 고유 ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	/**
	 * 하모니룸 (FK)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "harmony_room_id", nullable = false)
	private HarmonyRoom harmonyRoom;

	/**
	 * 가입 신청한 사용자들
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "harmony_room_waiting_users",
		joinColumns = @JoinColumn(name = "assign_wait_id"),
		inverseJoinColumns = @JoinColumn(name = "user_id")
	)
	private List<User> waitingUsers = new ArrayList<>();

	/**
	 * 생성 시간
	 */
	@Column(nullable = false)
	private LocalDateTime createdAt;

	// ========== 생성 메서드 ==========

	/**
	 * HarmonyRoomAssignWait 생성
	 */
	public static HarmonyRoomAssignWait create(HarmonyRoom harmonyRoom) {
		HarmonyRoomAssignWait assignWait = new HarmonyRoomAssignWait();
		assignWait.harmonyRoom = harmonyRoom;
		assignWait.waitingUsers = new ArrayList<>();
		assignWait.createdAt = LocalDateTime.now();
		return assignWait;
	}

	// ========== 비즈니스 메서드 ==========

	/**
	 * 대기자 추가
	 */
	public void addWaitingUser(User user) {
		if (!this.waitingUsers.contains(user)) {
			this.waitingUsers.add(user);
		}
	}

	/**
	 * 대기자 제거
	 */
	public void removeWaitingUser(User user) {
		this.waitingUsers.remove(user);
	}

	/**
	 * 사용자가 대기 중인지 확인
	 */
	public boolean isWaiting(User user) {
		return this.waitingUsers.contains(user);
	}
}
