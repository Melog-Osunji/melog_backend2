package com.osunji.melog.review.service;

import java.util.List;
import java.util.UUID;

import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostBookmark;
import com.osunji.melog.review.repository.BookmarkRepository;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

	private final BookmarkRepository bookmarkRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final AuthHelper authHelper;

	/** 게시글 북마크 생성 (API 27번) */
	public ApiMessage addBookmark(String postIdStr, String authHeader) {
		try {
			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 2. 사용자 및 게시글 조회
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			UUID postId = UUID.fromString(postIdStr);
			Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			// 3. 중복 북마크 체크
			if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
				return ApiMessage.fail(409, "이미 북마크한 게시글입니다.");
			}

			// 4. 북마크 생성 및 저장
			PostBookmark bookmark = PostBookmark.createBookmark(user, post);
			bookmarkRepository.save(bookmark);

			return ApiMessage.success(201, "북마크가 성공적으로 추가되었습니다.", null);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "북마크 추가 실패: " + e.getMessage());
		}
	}

	/** 게시글 북마크 삭제 (API 25번) */
	public ApiMessage removeBookmark(String postIdStr, String authHeader) {
		try {
			// 1. 토큰에서 userId 추출
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			// 2. UUID 변환
			UUID postId = UUID.fromString(postIdStr);

			// 3. 북마크 존재 여부 확인
			if (!bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
				return ApiMessage.fail(404, "북마크 정보가 존재하지 않습니다.");
			}

			// 4. 북마크 삭제
			bookmarkRepository.deleteByUserIdAndPostId(userId, postId);

			return ApiMessage.success(200, "북마크가 성공적으로 제거되었습니다.", null);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "북마크 삭제 실패: " + e.getMessage());
		}
	}

	/** 사용자 북마크 목록 조회 (API 23번) */
	@Transactional(readOnly = true)
	public ApiMessage<BookmarkResponse.ListAll> getBookmarksByUser(String userIdStr) {
		try {
			UUID userId = UUID.fromString(userIdStr);
			List<PostBookmark> bookmarks = bookmarkRepository.findBookmarkAllByuserId(userId);

			List<BookmarkResponse.BookmarkData> results = bookmarks.stream()
				.map(pb -> BookmarkResponse.BookmarkData.builder()
					.postId(pb.getPost().getId().toString())
					.title(pb.getPost().getTitle())
					.createdAt(pb.getCreatedAt().atStartOfDay()) // LocalDate → LocalDateTime
					.build())
				.toList();

			BookmarkResponse.ListAll response = BookmarkResponse.ListAll.builder()
				.results(results)
				.build();

			return ApiMessage.success(200, "북마크 목록 조회 성공", response);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다.");
		} catch (Exception e) {
			return ApiMessage.fail(500, "북마크 목록 조회 실패: " + e.getMessage());
		}
	}
}
