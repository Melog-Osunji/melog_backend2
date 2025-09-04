package com.osunji.melog.review.mapper;

import com.osunji.melog.review.dto.request.CommentRequest;
import com.osunji.melog.review.dto.response.CommentResponse;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.user.domain.User;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {

	/**
	 * create DTO + User + Post + (부모 댓글) → PostComment entity
	 */
	public PostComment toEntity(CommentRequest.Create req, User user, Post post, PostComment parentComment) {
		if (parentComment == null) {
			return PostComment.createComment(user, post, req.getContent());
		} else {
			return PostComment.createReply(user, post, req.getContent(), parentComment);
		}
	}

	/**
	 * bestComment entity → Best DTO
	 */
	public CommentResponse.Best toBestComment(PostComment bestComment) {
		if (bestComment == null) {
			return null;
		}

		return CommentResponse.Best.builder()
			.nickname(bestComment.getUser().getNickname())
			.profileUrl(bestComment.getUser().getProfileImageUrl())
			.content(bestComment.getContent())
			.likes(bestComment.getLikedUsers().size())
			.build();
	}

	/**
	 * CommentData entity → CommentData DTO 변환
	 */
	public CommentResponse.CommentData toCommentData(PostComment comment) {
		return CommentResponse.CommentData.builder()
			.nickname(comment.getUser().getNickname())
			.profileUrl(comment.getUser().getProfileImageUrl())
			.content(comment.getContent())
			.likes(comment.getLikedUsers().size())
			.recomments(toRecommentList(comment.getChildComments()))
			.build();
	}

	/**
	 * ReCommentData entity → RecommentData dto
	 */
	private List<CommentResponse.RecommentData> toRecommentList(List<PostComment> childComments) {
		if (childComments == null || childComments.isEmpty()) {
			return List.of();
		}
		return childComments.stream()
			.map(this::toRecommentData)
			.collect(Collectors.toList());
	}

	/**
	 * RecommentData entity → RecommentData DTO
	 */
	private CommentResponse.RecommentData toRecommentData(PostComment comment) {
		return CommentResponse.RecommentData.builder()
			.nickname(comment.getUser().getNickname())
			.content(comment.getContent())
			.likes(comment.getLikedUsers().size())
			.recomments(toRecommentList(comment.getChildComments()))
			.build();
	}
}
