package com.osunji.melog.review.mapper;

import com.osunji.melog.review.dto.request.PostRequest;
import com.osunji.melog.review.dto.response.PostResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.user.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Component
public class PostMapper {


	/** ==========PostRequet의 내용========== */
	/** Create                  DTO > Entity */
	public Post toEntity(PostRequest.Create req, User user) {
		return new Post(
				user,
			req.getTitle(),
			req.getContent(),
			req.getMediaType(),
			req.getMediaUrl(),
			req.getTags()
		);
	}

	/** Update                DTO > entitu 부분 값만 */
	public void applyUpdate(Post post, PostRequest.Update req) {
		if(req.getTitle()     != null) post.setTitle(req.getTitle());
		if(req.getContent()   != null) post.setContent(req.getContent());
		if(req.getMediaType() != null) post.setMediaType(req.getMediaType());
		if(req.getMediaUrl()  != null) post.setMediaLink(req.getMediaUrl());
		if(req.getTags()      != null) post.setTagList(req.getTags());
	}


	/** ==========PostResponse의 내용========== */
	private PostResponse.Single convertToSingleResponse(Post post, PostComment bestComment, int commentCount) {
		PostResponse.BestCommentData bestCommentData = null;
		if (bestComment != null) {
			bestCommentData = PostResponse.BestCommentData.builder()
				.nickName(bestComment.getUser().getNickname())
				.content(bestComment.getContent())
				.profileImg(bestComment.getUser().getProfileImageUrl())
				.build();
		}

		PostResponse.PostData postData = PostResponse.PostData.builder()
			.id(post.getId())
			.title(post.getTitle())
			.content(post.getContent())
			.mediaType(post.getMediaType())
			.mediaUrl(post.getMediaLink())
			.tags(post.getTagList())
			.createdAgo(calcHoursAgo(post.getCreatedAt()))
			.likeCount(post.getLikes().size())
			.hiddenUser(post.getHiddenUser().stream().map(User::getId).collect(Collectors.toList()))
			.commentCount(commentCount)
			.bestComment(bestCommentData)
			.build();

		PostResponse.UserData userData = PostResponse.UserData.builder()
			.id(post.getUser().getId())
			.nickName(post.getUser().getNickname())
			.profileImg(post.getUser().getProfileImageUrl())
			.build();

		return PostResponse.Single.builder()
			.post(postData)
			.user(userData)
			.build();
	}


	// 공통 PostData 변환 (Post, BestComment, commentCount → PostData)
	private PostResponse.PostData toPostData(Post post, PostComment best, int commentCount) {
		return PostResponse.PostData.builder()
			.id(post.getId())
			.title(post.getTitle())
			.content(post.getContent())
			.mediaType(post.getMediaType())
			.mediaUrl(post.getMediaLink())
			.tags(post.getTagList())
			.createdAgo(calcHoursAgo(post.getCreatedAt()))
			.likeCount(post.getLikes().size())
			.hiddenUser(post.getHiddenUser().stream().map(User::getId).collect(Collectors.toList()))
			.commentCount(commentCount)
			.bestComment(best != null ? PostResponse.BestCommentData.builder()
				.nickName(best.getUser().getNickname())
				.content(best.getContent())
				.profileImg(best.getUser().getProfileImageUrl())
				.build() : null)
			.build();
	}

	// 공통 UserData 변환 (User → UserData)
	private PostResponse.UserData toUserData(User user) {
		return PostResponse.UserData.builder()
			.id(user.getId())
			.nickName(user.getNickname())
			.profileImg(user.getProfileImageUrl())
			.build();
	}
	public PostResponse.Single toSingle(Post post, PostComment bestComment, int commentCount) {
		return PostResponse.Single.builder()
			.post(toPostData(post, bestComment, commentCount))
			.user(toUserData(post.getUser()))
			.build();
	}
	public FilterPostResponse.FeedPostData toFeedPostData(Post post, PostComment bestComment, int commentCount) {
		FilterPostResponse.PostData postData = FilterPostResponse.PostData.builder()
			.id(post.getId())
			.title(post.getTitle())
			.content(post.getContent())
			.mediaType(post.getMediaType())
			.mediaUrl(post.getMediaLink())
			.tags(post.getTagList())
			.createdAgo(calcHoursAgo(post.getCreatedAt()))
			.likeCount(post.getLikes().size())
			.hiddenUser(post.getHiddenUser().stream().map(User::getId).collect(Collectors.toList()))
			.commentCount(commentCount)
			.bestComment(bestComment == null ? null : FilterPostResponse.BestCommentData.builder()
				.nickName(bestComment.getUser().getNickname())
				.content(bestComment.getContent())
				.profileImg(bestComment.getUser().getProfileImageUrl())
				.build())
			.build();

		FilterPostResponse.UserData userData = FilterPostResponse.UserData.builder()
			.id(post.getUser().getId())
			.nickName(post.getUser().getNickname())
			.profileImg(post.getUser().getProfileImageUrl())
			.build();

		return FilterPostResponse.FeedPostData.builder()
			.post(postData)
			.user(userData)
			.build();
	}

	/**
	 * entity → UserPostData (특정 유저의 모든 게시글)
	 */
	public FilterPostResponse.UserPostData toUserPostData(Post post) {
		return FilterPostResponse.UserPostData.builder()
			.id(post.getId())
			.title(post.getTitle())
			.content(post.getContent())
			.mediaType(post.getMediaType())
			.mediaUrl(post.getMediaLink())
			.tags(post.getTagList())
			.build();
	}

	/* createdAt 기준 현재까지 경과 시간 (시간 단위 int) */
	private int calcHoursAgo(LocalDate createdAt) {
		return (int) ChronoUnit.HOURS.between(createdAt.atStartOfDay(), LocalDate.now().atStartOfDay());
	}

}
