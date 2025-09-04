package com.osunji.melog.search.dto.response;
import com.osunji.melog.review.entity.Post;

import com.osunji.melog.user.domain.User;
import lombok.Builder;
import lombok.Data;
import java.util.List;

public class SearchResponse {

	// 31번 통합 검색
	@Data @Builder
	public static class AllSearch {
		private List<String> recommendKeyword;
		private List<String> livePopularSearch;
		private String nowTime;
	}

	// 32번 작곡가
	@Data @Builder
	public static class Composer {
		private List<String> name;
		private List<String> imgLink;
	}

	// 33번 연주가
	@Data @Builder
	public static class Player {
		private String name;
		private List<String> keyword;
	}


	// 34번 장르
	@Data @Builder
	public static class Genre {
		private String genre;
		private List<String> keyword;
	}

	// 35번 시대
	@Data @Builder
	public static class Period {
		private List<String> era;
	}

	// 36번 악기
	@Data @Builder
	public static class Instrument {
		private List<String> instrument;
		private List<String> imgLink;
	}





	// 공통 post결과
	@Data @Builder
	public static class PostResult {
		private Post post;
		private User user;
	}
	// 37번 검색결과(게시글 + 인기미디어)
	@Data @Builder
	public static class SearchResultAll {
		private List<PostResult> results;
		private List<PopularMedia> popularMedia;

		@Data @Builder
		public static class PopularMedia {
			private String userNickname;
			private String userProfileImgLink;
			private String postID;
			private String mediaURL;
			private String mediaType;
			private Integer createdAgo;
		}
	}

	// 38번 검색결과 프로필
	@Data @Builder
	public static class SearchProfile {
		private List<UserProfile> user;

		@Data @Builder
		public static class UserProfile {
			private String userNickname;
			private String profileUrl;
			private String intro;
			private String follow;
		}
	}

	// 39번 검색결과 피드
	@Data @Builder
	public static class SearchFeed {
		private List<PostResult> resultsRecent;
		private List<PostResult> resultPopular;

	}
}
