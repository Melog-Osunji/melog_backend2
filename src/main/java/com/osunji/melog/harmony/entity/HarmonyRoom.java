package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "harmony_rooms")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HarmonyRoom {

	/**
	 * 하모니룸 고유 ID (UUID 자동 생성)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	/**
	 * 하모니룸 이름
	 */
	@Column(nullable = false, length = 100)
	private String name;

	/**
	 * 카테고리 리스트
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "category", columnDefinition = "json")
	private List<String> category = new ArrayList<>();

	/**
	 * 소개글
	 */
	@Column(columnDefinition = "TEXT")
	private String intro;

	/**
	 * 생성자 (User UUID와 연결)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	/**
	 * 생성 날짜/시간
	 */
	@Column(nullable = false)
	private LocalDateTime createdAt;

	/**
	 * 비공개 여부 (기본 false)
	 */
	@Column(nullable = false)
	private Boolean isPrivate = false;

	/**
	 * 북마크 수 (기본 0)
	 */
	@Column(nullable = false)
	private Integer bookMarkNum = 0;

	/**
	 * 바로 승인 여부 (기본 false)
	 */
	@Column(nullable = false)
	private Boolean isDirectAssign = false;

	/**
	 * 프로필 이미지 URL
	 */
	private String profileImageUrl;

	// ========== 생성 메서드 ==========

	/**
	 * HarmonyRoom 생성
	 */
	public static HarmonyRoom create(User owner, String name, List<String> category, String intro, String profileImageUrl) {
		HarmonyRoom harmonyRoom = new HarmonyRoom();
		harmonyRoom.owner = owner;
		harmonyRoom.name = name;
		harmonyRoom.category = category != null ? new ArrayList<>(category) : new ArrayList<>();
		harmonyRoom.intro = intro;
		harmonyRoom.profileImageUrl = profileImageUrl;
		harmonyRoom.createdAt = LocalDateTime.now();
		harmonyRoom.isPrivate = false;
		harmonyRoom.bookMarkNum = 0;
		harmonyRoom.isDirectAssign = false;
		return harmonyRoom;
	}

	// ========== 비즈니스 메서드 ==========

	/**
	 * 하모니룸 정보 수정 - null이 아닌 값만 업데이트
	 */
	public void update(String name, List<String> category, String intro, String profileImageUrl, Boolean isDirectAssign,Boolean isPrivate) {
		if (name != null && !name.trim().isEmpty()) {
			this.name = name;
		}
		if (category != null) {
			this.category = new ArrayList<>(category);
		}
		if (intro != null) {
			this.intro = intro;
		}
		if (profileImageUrl != null) {
			this.profileImageUrl = profileImageUrl;
		}
		if (isDirectAssign != null) {
			this.isDirectAssign = isDirectAssign;
		}
		if (isPrivate != null){
			this.isPrivate = isPrivate;
		}
	}

	/**
	 * 북마크 수 증가
	 */
	public void increaseBookmark() {
		this.bookMarkNum++;
	}

	/**
	 * 북마크 수 감소
	 */
	public void decreaseBookmark() {
		if (this.bookMarkNum > 0) {
			this.bookMarkNum--;
		}
	}
	// ✅ 프로필 이미지 업데이트 메Q서드 추가
	public void updateProfileImage(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	// ✅ 하모니룸 정보 업데이트 메서드
	public void updateInfo(String name, String intro, List<String> category, Boolean isPrivate, Boolean isDirectAssign) {
		if (name != null) this.name = name;
		if (intro != null) this.intro = intro;
		if (category != null) this.category = category;
		if (isPrivate != null) this.isPrivate = isPrivate;
		if (isDirectAssign != null) this.isDirectAssign = isDirectAssign;
	}

	// ✅ 북마크 수 증가/감소
	public void incrementBookmark() {
		this.bookMarkNum++;
	}

	public void decrementBookmark() {
		if (this.bookMarkNum > 0) {
			this.bookMarkNum--;
		}
	}
	/**
	 * 소유자 확인
	 */
	public boolean isOwner(User user) {
		return this.owner.equals(user);
	}

	/**
	 * 소유자 확인 (UUID로)
	 */
	public boolean isOwner(UUID userId) {
		return this.owner.getId().equals(userId);
	}
}
