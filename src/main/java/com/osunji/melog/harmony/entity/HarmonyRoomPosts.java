package com.osunji.melog.harmony.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.osunji.melog.review.entity.Post;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "harmony_room_posts")  // ✅ 테이블명 수정 (언더스코어)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HarmonyRoomPosts {

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
	 * 게시글 ID 리스트
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "post_ids", columnDefinition = "json")
	private List<String> postIds = new ArrayList<>();

	/**
	 * 추가된 시간
	 */
	@Column(nullable = false)
	private LocalDateTime createdAt;

	// ========== 생성 메서드 ==========

	/**
	 * HarmonyRoomPosts 생성
	 */
	public static HarmonyRoomPosts create(HarmonyRoom harmonyRoom) {
		HarmonyRoomPosts harmonyRoomPosts = new HarmonyRoomPosts();
		harmonyRoomPosts.harmonyRoom = harmonyRoom;
		harmonyRoomPosts.postIds = new ArrayList<>();
		harmonyRoomPosts.createdAt = LocalDateTime.now();
		return harmonyRoomPosts;
	}

	// ========== 비즈니스 메서드 ==========

	/**
	 * 게시글 추가
	 */
	public void addPost(Post post) {
		String postId = post.getId().toString();
		if (!this.postIds.contains(postId)) {
			this.postIds.add(postId);
		}
	}

	/**
	 * 게시글 제거
	 */
	public void removePost(Post post) {
		this.postIds.remove(post.getId().toString());
	}
}
