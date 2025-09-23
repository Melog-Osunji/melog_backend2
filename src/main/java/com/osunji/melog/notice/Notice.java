package com.osunji.melog.notice;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(nullable = false)
	private Boolean isImportant;        // 중요 공지사항 여부


	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column
	private LocalDateTime updatedAt;

	@Column(length = 50)
	private String category;            // 카테고리 (업데이트, 이벤트, 점검 등)

	@Column(length = 500)
	private String imageUrl;            // 첨부 이미지 URL (선택사항)

	public static Notice create(String title, String content, Boolean isImportant,
		String category, String imageUrl, LocalDateTime expiredAt) {
		Notice notice = new Notice();
		notice.title = title;
		notice.content = content;
		notice.isImportant = isImportant != null ? isImportant : false;
		notice.category = category;
		notice.imageUrl = imageUrl;
		notice.createdAt = LocalDateTime.now();
		return notice;
	}

	public void update(String title, String content, Boolean isImportant,
		String category, String imageUrl, LocalDateTime expiredAt) {
		if (title != null) this.title = title;
		if (content != null) this.content = content;
		if (isImportant != null) this.isImportant = isImportant;
		if (category != null) this.category = category;
		if (imageUrl != null) this.imageUrl = imageUrl;
		this.updatedAt = LocalDateTime.now();
	}

	public void deactivate() {
		this.updatedAt = LocalDateTime.now();
	}
}
