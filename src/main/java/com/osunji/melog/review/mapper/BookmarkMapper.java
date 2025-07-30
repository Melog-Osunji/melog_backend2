package com.osunji.melog.review.mapper;

import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.entity.PostBookmark;  // 북마크 엔티티 (만약 존재한다면)
import org.springframework.stereotype.Component;

@Component
public class BookmarkMapper {

	/**
	 * Bookmark entity → BookmarkData DTO 변환
	 */
	public BookmarkResponse.BookmarkData toBookmarkData(PostBookmark PostBookmark) {
		return BookmarkResponse.BookmarkData.builder()
			.postId(PostBookmark.getPost().getId())
			.title(PostBookmark.getPost().getTitle())
			.createdAt(PostBookmark.getCreatedAt().atStartOfDay())
			.build();
	}

}
