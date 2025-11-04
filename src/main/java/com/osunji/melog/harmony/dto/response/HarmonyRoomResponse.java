package com.osunji.melog.harmony.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class HarmonyRoomResponse {

	/**
	 * 나의 하모니룸 조회 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class MyHarmony {
		private List<HarmonyRoomInfo> myHarmony;           // 내가 생성한 하모니룸
		private List<HarmonyRoomInfo> harmony;             // 내가 멤버인 하모니룸
		private List<HarmonyRoomInfo> bookmarkHarmony;     // 내가 즐겨찾기한 하모니룸

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		public static class HarmonyRoomInfo {
			private String id;
			private String profileImg;
			private String name;
		}
	}

	/**
	 * 최근 미디어 조회 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class RecentMedia {
		private List<RecentMediaInfo> recentMedia;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		public static class RecentMediaInfo {
			private String harmonyRoomId;
			private String userNickname;
			private String userProfileImgLink;
			private String harmonyRoomName;
			private String postID;
			private String mediaUrl;
			private String mediaType;
			private String createdAgo;          // "오늘" 또는 "n일전"
		}
	}

	/**
	 * 추천 하모니룸 조회 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class RecommendHarmony {
		private List<RecommendHarmonyInfo> recommendedRooms;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		public static class RecommendHarmonyInfo {
			private String id;
			private String name;
			private List<String> category;
			private String profileImgLink;
			private String intro;
			private Integer memberNum;
			private List<String> userProfileImgsUrl;    // 랜덤 멤버 프로필 이미지 2개
		}
	}

	/**
	 * 하모니룸 게시글 조회 응답 DTO (SearchResponse와 동일한 구조)
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class HarmonyRoomPosts {
		private String harmonyRoomId;           // ✅ 하모니룸 고유 ID 추가
		private String harmonyRoomName;
		private List<PostResult> recommend;     // 추천 게시글
		private List<PostResult> popular;       // 인기 게시글

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		public static class PostResult {
			private PostDetail post;
			private UserInfo user;

			@Data
			@NoArgsConstructor
			@AllArgsConstructor
			@Builder
			public static class PostDetail {
				private String id;
				private String content;
				private String mediaType;
				private String mediaUrl;
				private List<String> tags;
				private Integer createdAgo;
				private Integer likeCount;
				private List<String> hiddenUser;
				private Integer commentCount;
				private BestComment bestComment;

				@Data
				@NoArgsConstructor
				@AllArgsConstructor
				@Builder
				public static class BestComment {
					private String userId;
					private String content;
				}
			}

			@Data
			@NoArgsConstructor
			@AllArgsConstructor
			@Builder
			public static class UserInfo {
				private String id;
				private String nickName;
				private String profileImg;
			}
		}
	}

	/**
	 * 하모니룸 범용 정보 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Information {
		private String id;
		private String profileImgLink;
		private String name;
		private List<String> category;
		private String intro;
		private Boolean isRunning;              // 내가 생성자인지 여부
		private Boolean isPrivate;
		private LocalDateTime createdAt;
		private List<String> members;           // 멤버 ID 리스트
		private String owner;                   // 소유자 ID
		private Boolean isDirectAssign;
	}

	/**
	 * 하모니룸 상세 정보 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Detail {
		private String id;
		private String profileImgLink;
		private String name;
		private List<String> category;
		private String intro;
		private Integer memberNum;
		private Integer ranking;                // 즐겨찾기 수 기준 랭킹
		private Integer countPosts;             // 누적 피드 개수
		private Boolean isBookmark;             // 내가 즐겨찾기 했는지
		private Boolean isAssign;               // 내가 멤버인지
	}
	@Data
	@Builder
	public static class Simple {
		private String id;
		private String name;
		private String intro;
		private List<String> category;
		private String profileImgLink;
	}
	/**
	 * 멤버 여부 확인 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class IsMember {
		private String harmonyRoomId;           // ✅ 하모니룸 고유 ID 추가
		private String harmonyRoomName;
		private Boolean isMember;
	}
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class BookmarkResult {
		private String harmonyRoomId;           // ✅ 하모니룸 고유 ID 추가
		private String harmonyRoomName;
		private Boolean bookmarked;     // true: 즐겨찾기 추가, false: 제거
		private String message;         // "즐겨찾기에 추가되었습니다" / "즐겨찾기에서 제거되었습니다"
	}
	/**
	 * 가입 승인 대기 유저 리스트 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class WaitingUsers {
		private String harmonyRoomId;           // ✅ 하모니룸 고유 ID 추가
		private String harmonyRoomName;
		private List<WaitingUserInfo> waitingUsers;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		public static class WaitingUserInfo {
			private UserProfile user;

			@Data
			@NoArgsConstructor
			@AllArgsConstructor
			@Builder
			public static class UserProfile {
				private String id;
				private String nickname;
				private String profileImgLink;
				private String intro;
			}
		}
	}

	/**
	 * 딥링크 공유 생성 응답 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Share {
		private String harmonyRoomId;           // ✅ 하모니룸 고유 ID 추가
		private String harmonyRoomName;
		private String deepLink;            // 딥링크 URL
		private String webLink;             // 웹 링크 (앱 없을 때)
		private String storeLink;           // 앱스토어 링크
		private String qrCode;              // QR 코드 URL (선택)
	}

	/**
	 * 하모니룸 이름 검색 결과 DTO
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class SearchByName {
		private List<HarmonyRoomSearchResult> harmonyRooms;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@Builder
		public static class HarmonyRoomSearchResult {
			private String id;                      // ✅ 하모니룸 고유 ID (이미 있음)
			private String name;                    // 하모니룸 이름
			private List<String> category;          // 카테고리
			private String profileImgLink;          // 프로필 이미지
			private String intro;                   // 소개글
			private Integer memberNum;              // 멤버 수
			private Boolean isPrivate;              // 비공개 여부
			private Boolean isDirectAssign;         // 바로 가입 여부
		}
	}
}
