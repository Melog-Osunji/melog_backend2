package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.osunji.melog.user.domain.User;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "harmony_room_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HarmonyRoomPosts {

	/**
	 * 고유 ID 필수/자동생성
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	/**
	 * 하모니룸 FK 필수
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "harmony_room_id", nullable = false)
	@JsonIgnore
	private HarmonyRoom harmonyRoom;

	/**
	 * 작성자 FK 필수
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * 게시글 내용 필수
	 */
	@Column(columnDefinition = "text", nullable = false)
	private String content;

	/**
	 * 미디어 타입 필수x
	 */
	@Column(name = "media_type")
	private String mediaType;

	/**
	 * 미디어 URL 필수x
	 */
	@Column(name = "media_url")
	private String mediaUrl;

	/**
	 * 태그 필수x
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "tags", columnDefinition = "json")
	private List<String> tags;

	/**
	 * 생성 시간 필수/자동 dddd
	 */
	@Column(name = "created_at", nullable = false)
	@Builder.Default
	private LocalDateTime createdAt = LocalDateTime.now();

	//  좋아요 관계
	@OneToMany(mappedBy = "harmonyPost", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<HarmonyPostLike> likes = new HashSet<>();

	//  댓글 관계
	@OneToMany(mappedBy = "harmonyPost", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<HarmonyPostComment> comments = new ArrayList<>();

	// 북마크 관계
	@OneToMany(mappedBy = "harmonyPost", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<HarmonyPostBookmark> bookmarks = new HashSet<>();
	// ========== 정적 팩토리 메서드 ==========
	/**
	 * create 텍스트만
	 */
	public static HarmonyRoomPosts createTextPost(HarmonyRoom harmonyRoom, User user, String content) {
		return HarmonyRoomPosts.builder()
			.harmonyRoom(harmonyRoom)
			.user(user)
			.content(content)
			.build();  // 나머지는 모두 null
	}

	/**
	 * creaet 이미지와 함께
	 */
	public static HarmonyRoomPosts createImagePost(HarmonyRoom harmonyRoom, User user,
		String content, String imageUrl) {
		return HarmonyRoomPosts.builder()
			.harmonyRoom(harmonyRoom)
			.user(user)
			.content(content)
			.mediaType("IMAGE")
			.mediaUrl(imageUrl)
			.build();
	}

	/**
	 * creaet비디오
	 */
	public static HarmonyRoomPosts createVideoPost(HarmonyRoom harmonyRoom, User user,
		String content, String videoUrl) {
		return HarmonyRoomPosts.builder()
			.harmonyRoom(harmonyRoom)
			.user(user)
			.content(content)
			.mediaType("VIDEO")
			.mediaUrl(videoUrl)
			.build();
	}

	// ========== 비즈니스 메서드 ==========

	/**
	 * tag 추가
	 */
	public void addTag(String tag) {
		if (tag == null || tag.trim().isEmpty()) return;

		if (this.tags == null) {
			this.tags = new ArrayList<>();
		}

		if (!this.tags.contains(tag)) {
			this.tags.add(tag.trim());
		}
	}

	/**
	 * tag 제거
	 */
	public void removeTag(String tag) {
		if (this.tags != null) {
			this.tags.remove(tag);
		}
	}

	/**
	 * 내용 수정
	 */
	public void updateContent(String newContent) {
		if (newContent != null && !newContent.trim().isEmpty()) {
			this.content = newContent;
		}
	}

	/**
	 * 미디어 정보 수정
	 */
	public void updateMedia(String mediaType, String mediaUrl) {
		this.mediaType = mediaType;
		this.mediaUrl = mediaUrl;
	}

	/**
	 * 미디어 제거
	 */
	public void removeMedia() {
		this.mediaType = null;
		this.mediaUrl = null;
	}

	/**
	 * 작성자 확인
	 */
	public boolean isAuthor(User user) {
		return this.user != null && user != null &&
			this.user.getId().equals(user.getId());
	}

	/**
	 * 미디어 파일이 있는지 확인
	 */
	public boolean hasMedia() {
		return this.mediaType != null && this.mediaUrl != null &&
			!this.mediaUrl.trim().isEmpty();
	}

	/**
	 * 태그가 있는지 확인
	 */
	public boolean hasTags() {
		return this.tags != null && !this.tags.isEmpty();
	}

	/**
	 * 특정 태그 포함 여부
	 */
	public boolean hasTag(String tag) {
		return this.tags != null && this.tags.contains(tag);
	}
}
