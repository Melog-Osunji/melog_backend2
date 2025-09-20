package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "harmony_room_members")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HarmonyRoomMembers {

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
	 * 사용자 (FK)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * 역할 (OWNER/ADMIN/MEMBER)
	 */
	@Column(nullable = false, length = 20)
	private String role = "MEMBER";

	/**
	 * 가입일시
	 */
	@Column(nullable = false)
	private LocalDateTime joinedAt;

	// ========== 생성 메서드 ==========

	/**
	 * HarmonyRoomMembers 생성
	 */
	public static HarmonyRoomMembers create(HarmonyRoom harmonyRoom, User user, String role) {
		HarmonyRoomMembers member = new HarmonyRoomMembers();
		member.harmonyRoom = harmonyRoom;
		member.user = user;
		member.role = role != null ? role : "MEMBER";
		member.joinedAt = LocalDateTime.now();
		return member;
	}

	/**
	 * 소유자로 생성
	 */
	public static HarmonyRoomMembers createOwner(HarmonyRoom harmonyRoom, User user) {
		return create(harmonyRoom, user, "OWNER");
	}

	/**
	 * 일반 멤버로 생성
	 */
	public static HarmonyRoomMembers createMember(HarmonyRoom harmonyRoom, User user) {
		return create(harmonyRoom, user, "MEMBER");
	}
}
