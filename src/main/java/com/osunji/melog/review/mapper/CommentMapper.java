package com.osunji.melog.review.mapper;

import com.osunji.melog.review.dto.response.CommentResponse;
import com.osunji.melog.review.entity.PostComment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentMapper {

	/** 베스트 댓글 변환 (API 17번) */
	public CommentResponse.Best toBestComment(PostComment bestComment) {
		if (bestComment == null) return null;

		return CommentResponse.Best.builder()
			.id(bestComment.getId().toString())
			.userID(bestComment.getUser().getId().toString())     // ✅ API 명세: "userID"
			.nickname(bestComment.getUser().getNickname())
			.profileUrl(bestComment.getUser().getProfileImageUrl()) // ✅ API 명세: "profileUrl"
			.content(bestComment.getContent())
			.likes(bestComment.getLikeCount())                    // ✅ Entity 메서드 사용
			.build();
	}

	/** 댓글 데이터 변환 (API 16번) */
	public CommentResponse.CommentData toCommentData(PostComment comment) {
		return CommentResponse.CommentData.builder()
			.id(comment.getId().toString())
			.userID(comment.getUser().getId().toString())         // ✅ API 명세: "userID"
			.nickname(comment.getUser().getNickname())
			.profileUrl(comment.getUser().getProfileImageUrl())  // ✅ API 명세: "profileUrl"
			.content(comment.getContent())
			.likes(comment.getLikeCount())                        // ✅ Entity 메서드 사용
			.recomments(toRecommentList(comment.getChildComments())) // ✅ 대댓글 재귀 처리
			.build();
	}

	/** 대댓글 리스트 변환 */
	private List<CommentResponse.RecommentData> toRecommentList(List<PostComment> childComments) {
		if (childComments == null || childComments.isEmpty()) {
			return List.of();
		}
		return childComments.stream()
			.map(this::toRecommentData)
			.collect(Collectors.toList());
	}

	/** 대댓글 데이터 변환 (재귀 구조) */
	private CommentResponse.RecommentData toRecommentData(PostComment comment) {
		return CommentResponse.RecommentData.builder()
			.id(comment.getId().toString())
			.userID(comment.getUser().getId().toString())         // ✅ API 명세: "userID"
			.nickname((comment.getUser().getNickname()))
			.content(comment.getContent())
			.likes(comment.getLikeCount())                        // ✅ Entity 메서드 사용
			.recomments(toRecommentList(comment.getChildComments())) // ✅ 재귀 처리
			.build();
	}
}
