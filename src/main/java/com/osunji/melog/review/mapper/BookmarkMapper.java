package com.osunji.melog.review.mapper;

import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.entity.PostBookmark;
import org.springframework.stereotype.Component;

@Component
public class BookmarkMapper {

	/** 북마크 엔티티 → BookmarkDataccc DTO 변환 (API 23번) */
	public BookmarkResponse.BookmarkData toBookmarkData(PostBookmark postBookmark) {
		return BookmarkResponse.BookmarkData.builder()
			.postId(postBookmark.getPost().getId().toString())    // ✅ UUID → String
			.title(postBookmark.getPost().getTitle())
			.createdAt(postBookmark.getCreatedAt().atStartOfDay()) // ✅ LocalDate → LocalDateTime
			.build();
	}
}
