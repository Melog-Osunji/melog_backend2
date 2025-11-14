package com.osunji.melog.harmony.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.osunji.melog.user.domain.User;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "harmony_post_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HarmonyPostComment {

	/**
	 * 댓글 고유 ID (UUID)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	/**
	 * 댓글 작성자 (User UUID와 연결)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * 댓글이 속한 하모니룸 게시물 (HarmonyRoomPosts UUID와 연결)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "harmony_post_id", nullable = false)
	private HarmonyRoomPosts harmonyPost;

	/**
	 * 댓글 내용
	 */
	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	/**
	 * 댓글 작성 일시
	 */
	@Column(nullable = false)
	private LocalDateTime createdAt;

	/**
	 * 부모 댓글 (대댓글인 경우)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	private HarmonyPostComment parentComment;

	/**
	 * 자식 댓글 목록 (이 댓글의 대댓글들)
	 */
	@OneToMany(mappedBy = "parentComment",
		cascade = CascadeType.ALL,
		orphanRemoval = true)
	private List<HarmonyPostComment> childComments = new ArrayList<>();

	/**
	 * 댓글에 좋아요를 누른 사용자 목록
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "harmony_comment_likes",
		joinColumns = @JoinColumn(name = "comment_id"),
		inverseJoinColumns = @JoinColumn(name = "user_id")
	)
	private List<User> likedUsers = new ArrayList<>();

	/**
	 * 일반 댓글 생성자
	 */
	public HarmonyPostComment(User user, HarmonyRoomPosts harmonyPost, String content) {
		this.user = user;
		this.harmonyPost = harmonyPost;
		this.content = content;
		this.createdAt = LocalDateTime.now();
		this.parentComment = null;
		this.likedUsers = new ArrayList<>();
		this.childComments = new ArrayList<>();
	}

	/**
	 * 대댓글 생성자
	 */
	public HarmonyPostComment(User user, HarmonyRoomPosts harmonyPost, String content, HarmonyPostComment parentComment) {
		this.user = user;
		this.harmonyPost = harmonyPost;
		this.content = content;
		this.createdAt = LocalDateTime.now();
		this.parentComment = parentComment;
		this.likedUsers = new ArrayList<>();
		this.childComments = new ArrayList<>();
	}

	// ========== 정적 팩토리 메서드 ==========

	/**
	 * 일반 댓글 생성 메서드
	 */
	public static HarmonyPostComment createComment(User user, HarmonyRoomPosts harmonyPost, String content) {
		return new HarmonyPostComment(user, harmonyPost, content);
	}

	/**
	 * 대댓글 생성 메서드
	 */
	public static HarmonyPostComment createReply(User user, HarmonyRoomPosts harmonyPost, String content, HarmonyPostComment parentComment) {
		return new HarmonyPostComment(user, harmonyPost, content, parentComment);
	}

	// ========== 비즈니스 메서드 ==========

	/**
	 * 좋아요 추가
	 */
	public void addLike(User user) {
		if (!this.likedUsers.contains(user)) {
			this.likedUsers.add(user);
		}
	}

	/**
	 * 좋아요 제거
	 */
	public void removeLike(User user) {
		this.likedUsers.remove(user);
	}

	/**
	 * 좋아요 개수
	 */
	public int getLikeCount() {
		return this.likedUsers.size();
	}

	/**
	 * 사용자가 좋아요를 눌렀는지 확인
	 */
	public boolean isLikedBy(User user) {
		return this.likedUsers.contains(user);
	}

	/**
	 * 대댓글인지 확인
	 */
	public boolean isReply() {
		return this.parentComment != null;
	}

	/**
	 * 자식 댓글(대댓글) 개수
	 */
	public int getChildCommentCount() {
		return this.childComments.size();
	}
}
