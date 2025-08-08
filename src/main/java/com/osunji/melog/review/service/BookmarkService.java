package com.osunji.melog.review.service;

import java.util.List;

import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostBookmark;
import com.osunji.melog.review.repository.BookmarkRepository;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.UserRepository;
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

	//---------------북마크 CRUD-----------------//

	/**      게시글북마크 create (API 27번)   path(postID)header(token)       */
	public ApiMessage<Void> addBookmark(String postId, String authHeader) {
		// 1. 토큰으로 유저 id 겟
		String userId = null;
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (Exception e) {
			return ApiMessage.fail(401, e.getMessage());
		}
		// 2. 유저 존재 확인
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
		// 3. 해당 게시글 존재 확인
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

		// 이미 북마크되어 있지 않은 경우만 생성
		if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
			return ApiMessage.fail(409, "이미 북마크한 게시글입니다.");
		}
		PostBookmark bookmark = new PostBookmark(user, post);

		bookmarkRepository.save(bookmark);

		return ApiMessage.success(200, "북마크가 성공적으로 추가되었습니다.", null);
	}


	/**      게시글북마크 delete (API 25번)   path(postID)header(token)       */
	public ApiMessage<Void> removeBookmark(String postId, String authHeader) {
		// 1. 토큰으로 유저 id 겟
		String userId = null;
		try {
			userId = authHelper.authHelper(authHeader);
		} catch (Exception e) {
			return ApiMessage.fail(401, e.getMessage());
		}
		// 2. 유저 존재 확인
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
		// 3. 해당 게시글 존재 확인
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));


		// 존재해야만 삭제
		if (!bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
			return ApiMessage.fail(404, "북마크 정보가 존재하지 않습니다.");
		}
		bookmarkRepository.deleteByUserIdAndPostId(userId, postId);

		return ApiMessage.success(200, "북마크가 성공적으로 제거되었습니다.", null);
	}



	/**      게시글북마크 get (API 23번)   path(userId)       */
	public ApiMessage<BookmarkResponse.ListAll> getBookmarksByUser(String userId) {
		List<PostBookmark> bookmarks = bookmarkRepository.findBookmarkAllByuserId(userId);

		List<BookmarkResponse.BookmarkData> results = bookmarks.stream()
			.map(pb -> BookmarkResponse.BookmarkData.builder()
				.postId(pb.getPost().getId())
				.title(pb.getPost().getTitle())
				.createdAt(pb.getCreatedAt().atStartOfDay())
				.build())
			.toList();

		BookmarkResponse.ListAll response = BookmarkResponse.ListAll.builder()
			.results(results)
			.build();

		return ApiMessage.success(200, "북마크 목록 조회 성공", response);
	}



}
