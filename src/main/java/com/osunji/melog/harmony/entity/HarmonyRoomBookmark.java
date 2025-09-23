package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.osunji.melog.user.domain.User;
@Entity
@Table(name = "harmony_bookmarks",
	uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "harmony_room_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HarmonyRoomBookmark {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "harmony_room_id", nullable = false)
	private HarmonyRoom harmonyRoom;

	@Column(nullable = false)
	private LocalDateTime bookmarkedAt;

	public static HarmonyRoomBookmark create(User user, HarmonyRoom harmonyRoom) {
		HarmonyRoomBookmark bookmark = new HarmonyRoomBookmark();
		bookmark.user = user;
		bookmark.harmonyRoom = harmonyRoom;
		bookmark.bookmarkedAt = LocalDateTime.now();
		return bookmark;
	}
}
