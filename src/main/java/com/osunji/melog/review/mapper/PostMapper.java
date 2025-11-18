package com.osunji.melog.review.mapper;

import com.osunji.melog.review.dto.response.PostResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.user.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;  // ✅ LocalDate → LocalDateTime
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PostMapper {

	/** 단일 게시글 조회 응답 변환 (API 15번) */
	public PostResponse.Single toSingle(Post post, PostComment bestComment, int commentCount,boolean isLike, boolean isBookmark) {
		return PostResponse.Single.builder()
			.post(toPostData(post, bestComment, commentCount, isLike, isBookmark))
			.user(toUserData(post.getUser()))
			.build();
	}

	/** 피드용 게시글 데이터 변환 (API 18,19,20번) */
	public FilterPostResponse.FeedPostData toFeedPostData(Post post, PostComment bestComment, int commentCount, boolean isLike, boolean isBookmark) {
		try {
			log.info("🔍 PostMapper.toFeedPostData 시작");
			log.info("  - Post ID: {}", post.getId());
			log.info("  - Post 제목: {}", post.getTitle());
			log.info("  - Post 내용: '{}'", post.getContent());

			if (bestComment != null) {
				log.info("  - BestComment 내용: '{}'", bestComment.getContent());
				log.info("  - BestComment 사용자 ID: {}", bestComment.getUser().getId());
			}

			FilterPostResponse.PostData postData = FilterPostResponse.PostData.builder()
				.id(post.getId().toString())
				.title(post.getTitle())
				.content(post.getContent())
				.mediaType(post.getMediaType())
				.mediaUrl(post.getMediaUrl())
				.tags(post.getTags())
				.createdAgo(formatCreatedAgo(post.getCreatedAt())) // ✅ calcDaysAgo → calcHoursAgo
				.likeCount(post.getLikeCount())
				.hiddenUser(getHiddenUserNicknames(post))
				.commentCount(commentCount)
				.bestComment(toBestCommentForFeed(bestComment))
				.isLike(isLike)
				.isBookmark(isBookmark)
				.build();

			log.info("✅ PostData 생성 완료");

			FilterPostResponse.UserData userData = FilterPostResponse.UserData.builder()
				.id(post.getUser().getId().toString())
				.nickName(post.getUser().getNickname())
				.profileImg(post.getUser().getProfileImageUrl())
				.build();

			log.info("✅ UserData 생성 완료");

			return FilterPostResponse.FeedPostData.builder()
				.post(postData)
				.user(userData)
				.build();

		} catch (Exception e) {
			log.error("❌ PostMapper.toFeedPostData 오류: {}", e.getMessage(), e);
			e.printStackTrace();
			throw e;
		}
	}

	/** 사용자 게시글 목록용 변환 (API 22번) */
	public FilterPostResponse.UserPostData toUserPostData(Post post) {
		return FilterPostResponse.UserPostData.builder()
			.id(post.getId().toString())
			.title(post.getTitle())
			.content(post.getContent())
			.mediaType(post.getMediaType())
			.mediaUrl(post.getMediaUrl())
			.tags(post.getTags())
			.build();
	}

	// ========== 공통 변환 메서드 ==========

	/** PostResponse용 PostData 변환 */
	private PostResponse.PostData toPostData(Post post, PostComment bestComment, int commentCount,boolean isLike, boolean isBookmark) {
		return PostResponse.PostData.builder()
			.id(post.getId().toString())
			.title(post.getTitle())
			.content(post.getContent())
			.mediaType(post.getMediaType())
			.mediaUrl(post.getMediaUrl())
			.tags(post.getTags())
			.createdAgo(formatCreatedAgo(post.getCreatedAt())) // ✅ 수정
			.likeCount(post.getLikeCount())
			.hiddenUser(getHiddenUserNicknames(post))
			.commentCount(commentCount)
			.bestComment(toBestCommentForPost(bestComment))
			.isLike(isLike)
			.isBookmark(isBookmark)
			.build();
	}

	/** UserData 변환cccc */
	private PostResponse.UserData toUserData(User user) {
		return PostResponse.UserData.builder()
			.id(user.getId().toString())
			.nickName(user.getNickname())
			.profileImg(user.getProfileImageUrl())
			.build();
	}

	/** PostResponse용 BestComment 변환 */
	private PostResponse.BestCommentData toBestCommentForPost(PostComment bestComment) {
		if (bestComment == null) return null;

		return PostResponse.BestCommentData.builder()
			.nickName(bestComment.getUser().getNickname())
			.content(bestComment.getContent())
			.profileImg(bestComment.getUser().getProfileImageUrl())
			.build();
	}

	/** FilterPostResponse용 BestComment 변환 - 안전한 버전 */
	private FilterPostResponse.BestCommentData toBestCommentForFeed(PostComment bestComment) {
		if (bestComment == null) {
			log.info("✅ bestComment is null - 빈 객체 반환");
			return null;
		}

		try {
			log.info("🔍 toBestCommentForFeed 시작");
			log.info("  - 댓글 내용: '{}'", bestComment.getContent());
			log.info("  - 사용자 ID: {}", bestComment.getUser().getId());
			log.info("  - 사용자 닉네임: {}", bestComment.getUser().getNickname());

			String userId = bestComment.getUser().getId().toString();
			String content = bestComment.getContent();
			String profileImg = bestComment.getUser().getProfileImageUrl();

			log.info("✅ 변환할 데이터 준비 완료");
			log.info("  - userId: {}", userId);
			log.info("  - content: '{}'", content);
			log.info("  - profileImg: {}", profileImg);


			FilterPostResponse.BestCommentData result = FilterPostResponse.BestCommentData.builder()
				.userId(userId)
				.content(content)
				.profileImg(profileImg)
				.build();

			log.info("✅ BestCommentData 생성 완료");
			return result;

		} catch (Exception e) {
			log.error("❌ toBestCommentForFeed 오류: {} " ,e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/** 숨김 처리한 사용자들의 닉네임 리스트 반환 */
	private List<String> getHiddenUserNicknames(Post post) {
		try {
			return post.getHiddenUsers().stream()
				.map(User::getNickname)
				.filter(nickname -> nickname != null)
				.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("❌ getHiddenUserNicknames 오류: {}", e.getMessage(), e);
			return List.of();
		}
	}

	/** LocalDateTime 기준 몇 시간 전인지 계산 ✅ */
	private Integer calcHoursAgo(LocalDateTime createdAt) {
		if (createdAt == null) return 0;
		return (int) ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
	}
	private String formatCreatedAgo(LocalDateTime createdAt) {
		if (createdAt == null) return "";

		long hours = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
		if (hours < 1) return "방금 전";
		else if (hours < 24) return hours + "시간 전";

		long days = ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
		if (days == 1) return "하루 전";
		if (days <= 30) return days + "일 전";

		long months = ChronoUnit.MONTHS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
		if (months == 1) return "한 달 전";
		return months + "달 전";
	}
	/** LocalDateTime 기준 며칠 전인지 계산 (선택적 사용) ✅ */
	private Integer calcDaysAgo(LocalDateTime createdAt) {
		if (createdAt == null) return 0;
		return (int) ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
	}
}
