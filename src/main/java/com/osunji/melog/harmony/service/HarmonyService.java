package com.osunji.melog.harmony.service;
import com.osunji.melog.review.service.PostService;
import com.osunji.melog.global.common.AuthHelper;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.harmony.dto.request.HarmonyRoomRequest;
import com.osunji.melog.harmony.dto.response.HarmonyRoomResponse;
import com.osunji.melog.harmony.entity.*;
import com.osunji.melog.harmony.repository.*;
import com.osunji.melog.review.dto.request.PostRequest;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.repository.UserRepository;
import com.osunji.melog.review.repository.CommentRepository;
import com.osunji.melog.elk.service.HarmonyReportLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HarmonyService {
	private final CommentRepository commentRepository;
	private final HarmonyRoomRepository harmonyRoomRepository;
	private final HarmonyRoomPostsRepository harmonyRoomPostsRepository;
	private final HarmonyRoomAssignWaitRepository harmonyRoomAssignWaitRepository;
	private final HarmonyRoomMembersRepository harmonyRoomMembersRepository;
	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final HarmonyReportLogService harmonyReportLogService;
	private final HarmonyRoomBookmarkRepository harmonyRoomBookmarkRepository;
	private final HarmonyRoomReportRepository harmonyRoomReportRepository;
	private final AuthHelper authHelper;
	private final HarmonyCommentRepository harmonyCommentRepository;
	/**
	 * 1. 하모니룸 생성
	 */
	public void createHarmonyRoom(HarmonyRoomRequest.Create request, String authHeader) {
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 1 =  하모니룸 object 생성 및   db Save
		HarmonyRoom harmonyRoom = HarmonyRoom.create(
			user,
			request.getName(),
			request.getCategory(),
			request.getIntro(),
			request.getProfileImg()
		);
		harmonyRoomRepository.save(harmonyRoom);

		// 2 = 소유자를 멤버로 추가
		HarmonyRoomMembers ownerMember = HarmonyRoomMembers.createOwner(harmonyRoom, user);
		harmonyRoomMembersRepository.save(ownerMember);


		// 4 = 가입 대기 목록 생성
		HarmonyRoomAssignWait assignWait = HarmonyRoomAssignWait.create(harmonyRoom);
		harmonyRoomAssignWaitRepository.save(assignWait);

		log.info("✅ SERVICE LINE 41 : 하모니룸 생성 완료: {} (소유자: {})", harmonyRoom.getName(), user.getNickname());
	}

	/**
	 * 2. 나의 하모니룸 조회 (즐겨찾기 기능 완성)
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.MyHarmony getMyHarmony(String authHeader) {
		// 0 = 토큰으로 유저 인식 및 유저 체크
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 1 = 내가 생성한 하모니룸
		List<HarmonyRoom> myHarmonyRooms = harmonyRoomRepository.findByOwnerOrderByNameAsc(user);
		List<HarmonyRoomResponse.MyHarmony.HarmonyRoomInfo> myHarmony = myHarmonyRooms.stream()
			.map(room -> HarmonyRoomResponse.MyHarmony.HarmonyRoomInfo.builder()
				.id(room.getId().toString())
				.profileImg(room.getProfileImageUrl())
				.name(room.getName())
				.build())
			.collect(Collectors.toList());

		// 2 = 내가 멤버인 하모니룸 (소유자 제외)
		List<HarmonyRoomMembers> membershipList = harmonyRoomMembersRepository.findByUser(user);
		List<HarmonyRoomResponse.MyHarmony.HarmonyRoomInfo> harmony = membershipList.stream()
			.filter(membership -> !"OWNER".equals(membership.getRole()))
			.map(membership -> membership.getHarmonyRoom())
			.sorted(Comparator.comparing(HarmonyRoom::getName))
			.map(room -> HarmonyRoomResponse.MyHarmony.HarmonyRoomInfo.builder()
				.id(room.getId().toString())
				.profileImg(room.getProfileImageUrl())
				.name(room.getName())
				.build())
			.collect(Collectors.toList());

		// ✅ 3 = 내가 즐겨찾기한 하모니룸 (실제 구현)
		List<HarmonyRoomBookmark> bookmarkList = harmonyRoomBookmarkRepository.findByUserId(userId);
		List<HarmonyRoomResponse.MyHarmony.HarmonyRoomInfo> bookmarkHarmony = bookmarkList.stream()
			.map(bookmark -> bookmark.getHarmonyRoom())
			.filter(Objects::nonNull) // null 체크
			.sorted(Comparator.comparing(HarmonyRoom::getName))
			.map(room -> HarmonyRoomResponse.MyHarmony.HarmonyRoomInfo.builder()
				.id(room.getId().toString())
				.profileImg(room.getProfileImageUrl())
				.name(room.getName())
				.build())
			.collect(Collectors.toList());

		log.info("📋 나의 하모니룸 조회 완료: 생성 {}개, 멤버 {}개, 즐겨찾기 {}개",
			myHarmony.size(), harmony.size(), bookmarkHarmony.size());

		return HarmonyRoomResponse.MyHarmony.builder()
			.myHarmony(myHarmony)
			.harmony(harmony)
			.bookmarkHarmony(bookmarkHarmony) // ✅ 실제 즐겨찾기 데이터 반환
			.build();
	}

	/**
	 * 하모니룸 게시글 작성
	 */
	@Transactional
	public void createHarmonyRoomPost(String harmonyId, HarmonyRoomRequest.CreateHarmonyPost request, String authHeader) {
		// 사용자 인증
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 하모니룸 조회
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 하모니룸 멤버 여부 확인
		boolean isMember = harmonyRoomMembersRepository
			.existsByHarmonyRoomAndUser(harmonyRoom, user);

		if (!isMember) {
			throw new SecurityException("하모니룸 멤버만 게시글을 작성할 수 있습니다.");
		}

		// 실제 게시글 생성
		HarmonyRoomPosts post;

		if (request.getMediaUrl() != null && !request.getMediaUrl().trim().isEmpty()) {
			// 미디어가 있는 게시글
			post = HarmonyRoomPosts.builder()
				.harmonyRoom(harmonyRoom)
				.user(user)
				.content(request.getContent())
				.mediaType(request.getMediaType())
				.mediaUrl(request.getMediaUrl())
				.build();
		} else {
			// 텍스트만 있는 게시글
			post = HarmonyRoomPosts.createTextPost(harmonyRoom, user, request.getContent());
		}

		// 태그 추가
		if (request.getTags() != null) {
			request.getTags().forEach(post::addTag);
		}

		// 저장
		HarmonyRoomPosts savedPost = harmonyRoomPostsRepository.save(post);

		log.info("✅ 하모니룸 게시글 작성 완료: roomId={}, userId={}, postId={}, mediaType={}",
			harmonyId, userId, savedPost.getId(), savedPost.getMediaType());
	}

	/**
	 * 3. 최근 업로드 미디어 조회
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.RecentMedia getRecentMedia(String authHeader) {
		// 사용자 인증
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 내가 속한 하모니룸들 조회
		List<HarmonyRoomMembers> membershipList = harmonyRoomMembersRepository.findByUser(user);
		List<HarmonyRoom> myHarmonyRooms = membershipList.stream()
			.map(HarmonyRoomMembers::getHarmonyRoom)
			.collect(Collectors.toList());

		if (myHarmonyRooms.isEmpty()) {
			log.info("📺 속한 하모니룸이 없어서 최근 미디어 없음");
			return HarmonyRoomResponse.RecentMedia.builder()
				.recentMedia(List.of())
				.build();
		}

		// 하모니룸 게시글에서 미디어가 있는 것만 조회
		List<HarmonyRoomPosts> mediaPostsList = harmonyRoomPostsRepository
			.findByHarmonyRoomInAndMediaTypeIsNotNullOrderByCreatedAtDesc(myHarmonyRooms);

		List<HarmonyRoomResponse.RecentMedia.RecentMediaInfo> recentMediaList = new ArrayList<>();

		for (HarmonyRoomPosts post : mediaPostsList) {
			if (post.getMediaType() != null && post.getMediaUrl() != null) {
				String createdAgo = calculateCreatedAgo(post.getCreatedAt());

				recentMediaList.add(HarmonyRoomResponse.RecentMedia.RecentMediaInfo.builder()
					.harmonyRoomId(post.getHarmonyRoom().getId().toString())
					.userNickname(post.getUser().getNickname())
					.userProfileImgLink(post.getUser().getProfileImageUrl())
					.harmonyRoomName(post.getHarmonyRoom().getName())
					.postID(post.getId().toString())
					.mediaUrl(post.getMediaUrl())
					.mediaType(post.getMediaType())
					.createdAgo(createdAgo)
					.build());
			}
		}

		log.info("📺 최근 미디어 조회 완료: {}개", recentMediaList.size());

		return HarmonyRoomResponse.RecentMedia.builder()
			.recentMedia(recentMediaList.stream().limit(10).collect(Collectors.toList()))
			.build();
	}

	/**
	 * 4. 추천 하모니룸 조회
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.RecommendHarmony getRecommendHarmony(String authHeader) {
		// 0 = ㅇㅇ동일
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 1 = 내가 속하지 않은 공개 하모니룸들 조회
		List<HarmonyRoom> publicRooms = harmonyRoomRepository.findPublicHarmonyRoomsForRecommend();
		List<HarmonyRoomMembers> myMemberships = harmonyRoomMembersRepository.findByUser(user);
		Set<UUID> myHarmonyRoomIds = myMemberships.stream()
			.map(membership -> membership.getHarmonyRoom().getId())
			.collect(Collectors.toSet());

		List<HarmonyRoomResponse.RecommendHarmony.RecommendHarmonyInfo> recommendedRooms = publicRooms.stream()
			.filter(room -> !myHarmonyRoomIds.contains(room.getId())) // 내가 속하지 않은 룸만
			.limit(10)
			.map(room -> {
				// 2 = 해당 하모니룸 멤버들 조회
				List<HarmonyRoomMembers> members = harmonyRoomMembersRepository.findByHarmonyRoom(room);
				int memberCount = members.size();

				// 3 = 랜덤으로 멤버 프로필 이미지 2개 선택
				List<String> userProfileImages = members.stream()
					.map(member -> member.getUser().getProfileImageUrl())
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
				Collections.shuffle(userProfileImages);
				List<String> randomProfileImages = userProfileImages.stream()
					.limit(2)
					.collect(Collectors.toList());

				return HarmonyRoomResponse.RecommendHarmony.RecommendHarmonyInfo.builder()
					.id(room.getId().toString())
					.name(room.getName())
					.category(room.getCategory())
					.profileImgLink(room.getProfileImageUrl())
					.intro(room.getIntro())
					.memberNum(memberCount)
					.userProfileImgsUrl(randomProfileImages)
					.build();
			})
			.collect(Collectors.toList());

		log.info("⭐ 추천 하모니룸 조회 완료: {}개", recommendedRooms.size());

		return HarmonyRoomResponse.RecommendHarmony.builder()
			.recommendedRooms(recommendedRooms)
			.build();
	}
	/**
	 * 5. 하모니룸 게시글 조회 (성능 최적화 버전)
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.HarmonyRoomPosts getHarmonyRoomPosts(String harmonyId, String authHeader) {
		String currentUserId = null;
		try {
			if (authHeader != null) {
				UUID userId = authHelper.authHelperAsUUID(authHeader);
				currentUserId = userId.toString();
			}
		} catch (Exception e) {
			log.debug("비로그인 사용자 하모니룸 게시글 조회");
		}

		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 연관 데이터와 함께 조회 (N+1 문제 해결)
		List<HarmonyRoomPosts> harmonyRoomPostsList = harmonyRoomPostsRepository
			.findByHarmonyRoomWithAllAssociations(harmonyRoom);

		if (harmonyRoomPostsList.isEmpty()) {
			log.info("📝 하모니룸 {}에 게시글이 없음", harmonyRoom.getName());
			return HarmonyRoomResponse.HarmonyRoomPosts.builder()
				.harmonyRoomId(harmonyRoom.getId().toString())
				.harmonyRoomName(harmonyRoom.getName())
				.recommend(List.of())
				.popular(List.of())
				.build();
		}

		final String finalCurrentUserId = currentUserId;

		// 베스트 댓글을 미리 한 번에 조회 (배치 처리)
		Map<UUID, HarmonyPostComment> bestCommentsMap = getBestCommentsForPosts(
			harmonyRoomPostsList.stream().map(HarmonyRoomPosts::getId).collect(Collectors.toList())
		);

		// 추천 (최신순)
		List<HarmonyRoomResponse.HarmonyRoomPosts.PostResult> recommend = harmonyRoomPostsList.stream()
			.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
			.map(post -> createHarmonyPostResult(post, finalCurrentUserId, bestCommentsMap.get(post.getId())))
			.collect(Collectors.toList());

		// 인기 (좋아요순)
		List<HarmonyRoomResponse.HarmonyRoomPosts.PostResult> popular = harmonyRoomPostsList.stream()
			.sorted((a, b) -> {
				int likesA = a.getLikes() != null ? a.getLikes().size() : 0;
				int likesB = b.getLikes() != null ? b.getLikes().size() : 0;
				return Integer.compare(likesB, likesA);
			})
			.map(post -> createHarmonyPostResult(post, finalCurrentUserId, bestCommentsMap.get(post.getId())))
			.collect(Collectors.toList());

		return HarmonyRoomResponse.HarmonyRoomPosts.builder()
			.harmonyRoomId(harmonyRoom.getId().toString())
			.harmonyRoomName(harmonyRoom.getName())
			.recommend(recommend)
			.popular(popular)
			.build();
	}

	/**
	 *수정된 createHarmonyPostResult (베스트 댓글을 파라미터로 받음)
	 */
	private HarmonyRoomResponse.HarmonyRoomPosts.PostResult createHarmonyPostResult(
		HarmonyRoomPosts post, String currentUserId, HarmonyPostComment bestComment) {

		// 좋아요 수
		int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;

		// 댓글 수
		int commentCount = post.getComments() != null ? post.getComments().size() : 0;

		// 현재 사용자의 좋아요 여부
		boolean isLiked = false;
		if (currentUserId != null && post.getLikes() != null) {
			isLiked = post.getLikes().stream()
				.anyMatch(like -> like.getUser().getId().toString().equals(currentUserId));
		}

		// 현재 사용자의 북마크 여부
		boolean isBookmarked = false;
		if (currentUserId != null && post.getBookmarks() != null) {
			isBookmarked = post.getBookmarks().stream()
				.anyMatch(bookmark -> bookmark.getUser().getId().toString().equals(currentUserId));
		}

		// 생성 시간 (초 단위)
		Integer createdAgo = calculateCreatedAgoInSeconds(post.getCreatedAt());

		HarmonyRoomResponse.HarmonyRoomPosts.PostResult.PostDetail.BestComment bestCommentDto = null;
		if (bestComment != null) {
			log.debug("🎯 베스트 댓글 발견: postId={}, commentId={}, content={}",
				post.getId(), bestComment.getId(), bestComment.getContent());

			bestCommentDto = HarmonyRoomResponse.HarmonyRoomPosts.PostResult.PostDetail.BestComment.builder()
				.userId(bestComment.getUser().getId().toString())
				.content(bestComment.getContent())
				.build();
		} else {
			log.debug("🔍 베스트 댓글 없음: postId={}", post.getId());
		}

		// PostDetail 생성
		HarmonyRoomResponse.HarmonyRoomPosts.PostResult.PostDetail postDetail =
			HarmonyRoomResponse.HarmonyRoomPosts.PostResult.PostDetail.builder()
				.id(post.getId().toString())
				.content(post.getContent())
				.mediaType(post.getMediaType())
				.mediaUrl(post.getMediaUrl())
				.tags(post.getTags() != null ? post.getTags() : List.of())
				.createdAgo(createdAgo)
				.likeCount(likeCount)
				.hiddenUser(List.of())  // 숨김 사용자 (추후 구현)
				.commentCount(commentCount)
				.bestComment(bestCommentDto)
				.build();

		// UserInfo 생성
		HarmonyRoomResponse.HarmonyRoomPosts.PostResult.UserInfo userInfo =
			HarmonyRoomResponse.HarmonyRoomPosts.PostResult.UserInfo.builder()
				.id(post.getUser().getId().toString())
				.nickName(post.getUser().getNickname())
				.profileImg(post.getUser().getProfileImageUrl())
				.build();

		// PostResult 생성
		return HarmonyRoomResponse.HarmonyRoomPosts.PostResult.builder()
			.post(postDetail)
			.user(userInfo)
			.build();
	}

	/**
	 *여러 게시글의 베스트 댓글을 한 번에 조회 (배치 처리)
	 */
	private Map<UUID, HarmonyPostComment> getBestCommentsForPosts(List<UUID> postIds) {
		if (postIds.isEmpty()) {
			return new HashMap<>();
		}

		// 모든 게시글의 베스트 댓글을 한 번에 조회
		List<HarmonyPostComment> allBestComments = harmonyCommentRepository
			.findBestCommentsForMultiplePosts(postIds);

		// 게시글 ID별로 그룹화
		return allBestComments.stream()
			.collect(Collectors.toMap(
				comment -> comment.getHarmonyPost().getId(),
				comment -> comment,
				(existing, replacement) -> existing  // 중복시 첫 번째 댓글 유지 (이미 정렬되어 있음)
			));
	}

	/**
	 * 생성 시간을 초 단위로 계산
	 */
	private Integer calculateCreatedAgoInSeconds(LocalDateTime createdAt) {
		if (createdAt == null) return 0;

		LocalDateTime now = LocalDateTime.now();
		return (int) java.time.Duration.between(createdAt, now).getSeconds();
	}

	/**
	 * 6. 하모니룸 범용 정보 조회
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.Information getHarmonyRoomInformation(String harmonyId, String authHeader) {
		// 0 = ㅇㅇ유저 그거
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
		// 1 = 하모니름 찾기
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 3 = 멤버 목록 조회
		List<HarmonyRoomMembers> members = harmonyRoomMembersRepository.findByHarmonyRoom(harmonyRoom);
		List<String> memberIds = members.stream()
			.map(member -> member.getUser().getId().toString())
			.collect(Collectors.toList());

		// 4 = 내가 소유자인지 확인
		boolean isRunning = harmonyRoom.isOwner(user);

		log.info("ℹ️ 하모니룸 정보 조회 완료: {} (멤버 {}명)", harmonyRoom.getName(), members.size());

		return HarmonyRoomResponse.Information.builder()
			.id(harmonyRoom.getId().toString())         // ✅ 하모니룸 ID 추가

			.profileImgLink(harmonyRoom.getProfileImageUrl())
			.name(harmonyRoom.getName())
			.category(harmonyRoom.getCategory())
			.intro(harmonyRoom.getIntro())
			.isRunning(isRunning)
			.isPrivate(harmonyRoom.getIsPrivate())
			.createdAt(harmonyRoom.getCreatedAt())
			.members(memberIds)
			.owner(harmonyRoom.getOwner().getId().toString())
			.isDirectAssign(harmonyRoom.getIsDirectAssign())
			.build();
	}

	/**
	 * 7. 하모니룸 상세 정보 조회
	 */

	@Transactional(readOnly = true)
	public HarmonyRoomResponse.Detail getHarmonyRoomDetail(String harmonyId, String authHeader) {
		// 0 = 사용자 인증 및 조회
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 1 = 하모니룸 조회
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 2 = 멤버 수 조회
		Long memberCount = harmonyRoomMembersRepository.countByHarmonyRoom(harmonyRoom);

		// 3 = 북마크 수 조회 (Entity의 필드 사용)
		Long actualBookmarkCount = (long) harmonyRoom.getBookMarkNum();

		//  랭킹 조회 (북마크 수 기준, 없으면 기본값)
		Long ranking = 1L; // 기본값
		try {
			ranking = harmonyRoomRepository.findRankingByBookMarkCount(actualBookmarkCount);
			if (ranking == null) ranking = 1L;
		} catch (Exception e) {
			log.warn("랭킹 조회 실패, 기본값 사용: {}", e.getMessage());
		}

		//  4 = 실제 게시글 수 조회 (HarmonyRoomPosts 테이블에서)
		Long postCount = harmonyRoomPostsRepository.countByHarmonyRoom(harmonyRoom);

		// 5 = 내가 멤버인지 확인
		boolean isAssign = harmonyRoomMembersRepository.existsByHarmonyRoomAndUser(harmonyRoom, user);

		//  6 = 내가 북마크했는지 확인
		boolean isBookmark = harmonyRoomBookmarkRepository.existsByUserAndHarmonyRoom(user, harmonyRoom);

		log.info("📋 하모니룸 상세정보 조회 완료: {} (멤버 {}명, 게시글 {}개, 북마크 {}개, 랭킹 {}위, 내 북마크: {})",
			harmonyRoom.getName(), memberCount, postCount, actualBookmarkCount, ranking, isBookmark);

		return HarmonyRoomResponse.Detail.builder()
			.id(harmonyRoom.getId().toString())
			.profileImgLink(harmonyRoom.getProfileImageUrl())
			.name(harmonyRoom.getName())
			.category(harmonyRoom.getCategory())
			.intro(harmonyRoom.getIntro())
			.memberNum(memberCount.intValue())
			.ranking(ranking.intValue())
			.countPosts(postCount.intValue())
			.isBookmark(isBookmark)
			.isAssign(isAssign)
			.build();
	}


	/**
	 * 8. 멤버 여부 확인
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.IsMember checkMembership(String harmonyId, String authHeader) {
		// 0 = ㅇㅇ 유저그거
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
		// 1 = ㅇㅇ 하모니룸 그거
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));
		// 2 = 내가 이 하모니룸의 멤버인지
		boolean isMember = harmonyRoomMembersRepository.existsByHarmonyRoomAndUser(harmonyRoom, user);

		log.info("👥 멤버 여부 확인: {} - {}", harmonyRoom.getName(), isMember ? "멤버임" : "비멤버");
		// 3 = 체크
		return HarmonyRoomResponse.IsMember.builder()
			.harmonyRoomId(harmonyRoom.getId().toString())  // ✅ 하모니룸 ID 추가
			.harmonyRoomName(harmonyRoom.getName())
			.isMember(isMember)
			.build();
	}

	/**
	 * 9. 하모니룸 정보 수정
	 */
	public void updateHarmonyRoom(String harmonyId, HarmonyRoomRequest.Update request, String authHeader) {
		// 0 = ㅇㅇ 유저 그거
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
		// 1 = 하모니룸 그거
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 2 = 소유자 권한 확인
		if (!harmonyRoom.isOwner(user)) {
			throw new SecurityException("하모니룸 소유자만 수정할 수 있습니다");
		}

		// 3 = 정보 업데이트 (null이 아닌 값만)
		harmonyRoom.update(
			request.getName(),
			request.getCategory(),
			request.getIntro(),
			request.getProfileImg(),
			request.getIsDirectAssign(),
			request.getIsPrivate()
		);

		harmonyRoomRepository.save(harmonyRoom);

		log.info("✏️ 하모니룸 수정 완료: {}", harmonyRoom.getName());
	}

	/**
	 * 10. 하모니룸 삭제 (연관된 북마크와 신고까지 모두 삭제)
	 */
	public void deleteHarmonyRoom(String harmonyId, HarmonyRoomRequest.Delete request, String authHeader) {
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 소유자 권한 확인
		if (!harmonyRoom.isOwner(user)) {
			throw new SecurityException("하모니룸 소유자만 삭제할 수 있습니다");
		}

		String roomName = harmonyRoom.getName();
		log.info("🗑️ 하모니룸 삭제 시작: {} (소유자: {})", roomName, user.getNickname());

		try {
			// ✅ 1. 북마크 먼저 삭제 (Foreign Key 제약 해결)
			List<HarmonyRoomBookmark> bookmarks = harmonyRoomBookmarkRepository.findByHarmonyRoomId(harmonyRoomId);
			if (!bookmarks.isEmpty()) {
				harmonyRoomBookmarkRepository.deleteAll(bookmarks);
				log.info("  📌 북마크 {}개 삭제 완료", bookmarks.size());
			}

			// ✅ 2. 신고 기록 삭제
			List<HarmonyRoomReport> reports = harmonyRoomReportRepository.findByHarmonyRoomIdOrderByReportedAtDesc(harmonyRoomId);
			if (!reports.isEmpty()) {
				harmonyRoomReportRepository.deleteAll(reports);
				log.info("  🚨 신고 기록 {}개 삭제 완료", reports.size());
			}

			// ✅ 3. 기존 연관 데이터 삭제
			harmonyRoomPostsRepository.findByHarmonyRoom(harmonyRoom).ifPresent(harmonyRoomPostsRepository::delete);
			log.info("  📝 게시글 목록 삭제 완료");

			harmonyRoomAssignWaitRepository.findByHarmonyRoom(harmonyRoom).ifPresent(harmonyRoomAssignWaitRepository::delete);
			log.info("  ⏳ 가입 대기 목록 삭제 완료");

			List<HarmonyRoomMembers> members = harmonyRoomMembersRepository.findByHarmonyRoom(harmonyRoom);
			if (!members.isEmpty()) {
				harmonyRoomMembersRepository.deleteAll(members);
				log.info("  👥 멤버 {}명 삭제 완료", members.size());
			}

			// ✅ 4. 마지막에 하모니룸 삭제
			harmonyRoomRepository.delete(harmonyRoom);
			log.info("  🏠 하모니룸 본체 삭제 완료");

			// ✅ 5. ElasticSearch에 삭제 로그 (선택사항)
			try {
				harmonyReportLogService.logHarmonyReport(
					"DELETE_" + System.currentTimeMillis(),
					harmonyRoomId.toString(),
					roomName + " (DELETED)",
					userId.toString(),
					"HARMONY_ROOM_DELETED",
					"사유: " + (request.getReason() != null ? request.getReason() : "미제공")
				);
			} catch (Exception e) {
				log.warn("⚠️ 삭제 로그 기록 실패: {}", e.getMessage());
			}

			log.info("✅ 하모니룸 완전 삭제 완료: {}", roomName);

		} catch (Exception e) {
			log.error("💥 하모니룸 삭제 중 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("하모니룸 삭제에 실패했습니다: " + e.getMessage());
		}
	}

	/**
	 * 11. 가입 승인 대기 유저 리스트
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.WaitingUsers getWaitingUsers(String harmonyId) {
		// 1 = 하모니룸 체크
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		Optional<HarmonyRoomAssignWait> assignWaitOpt = harmonyRoomAssignWaitRepository.findByHarmonyRoom(harmonyRoom);
		// 없으면 빈값리턴
		if (assignWaitOpt.isEmpty()) {
			return HarmonyRoomResponse.WaitingUsers.builder()
				.waitingUsers(List.of())
				.build();
		}
		// 2 = 유저 있으면 정보 리턴
		List<User> waitingUsers = assignWaitOpt.get().getWaitingUsers();
		List<HarmonyRoomResponse.WaitingUsers.WaitingUserInfo> waitingUserInfos = waitingUsers.stream()
			.map(waitingUser -> HarmonyRoomResponse.WaitingUsers.WaitingUserInfo.builder()
				.user(HarmonyRoomResponse.WaitingUsers.WaitingUserInfo.UserProfile.builder()
					.id(waitingUser.getId().toString())
					.nickname(waitingUser.getNickname())
					.profileImgLink(waitingUser.getProfileImageUrl())
					.intro(waitingUser.getIntro())
					.build())
				.build())
			.collect(Collectors.toList());

		log.info("⏳ 가입 대기 유저 조회 완료: {}개", waitingUserInfos.size());

		return HarmonyRoomResponse.WaitingUsers.builder()
			.waitingUsers(waitingUserInfos)
			.build();
	}

	/**
	 * 11-1 나 하모니룸 가입대기중임?
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.IsWaiting isWaitingUser(String harmonyId, String authHeader) {
		// 0 = 유저 체크
		UUID currentUserId = authHelper.authHelperAsUUID(authHeader);
		if (currentUserId == null) {
			throw new IllegalArgumentException("사용자 오류.");
		}

		// 1 = 하모니룸 체크
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 2 = 대기목록 조회
		Optional<HarmonyRoomAssignWait> assignWaitOpt = harmonyRoomAssignWaitRepository.findByHarmonyRoom(harmonyRoom);
		boolean isWaiting = false;

		if (assignWaitOpt.isPresent()) {
			List<User> waitingUsers = assignWaitOpt.get().getWaitingUsers();
			isWaiting = waitingUsers.stream().anyMatch(user -> user.getId().equals(currentUserId));
		}

		// 3 = 응답 DTO 구성
		return HarmonyRoomResponse.IsWaiting.builder()
			.harmonyRoomId(harmonyRoom.getId().toString())
			.harmonyRoomName(harmonyRoom.getName())
			.isWaiting(isWaiting)
			.build();
	}



	/**
	 * 12-1. 가입 승인
	 */
	public void approveUser(String harmonyId, HarmonyRoomRequest.ApproveOrDeny request) {
		// 0 = 룸아이디 체크
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		UUID targetUserId = UUID.fromString(request.getUserID());
		// 없을때 예외처리
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));
		// 타겟유저 없을때 예외처리
		User targetUser = userRepository.findById(targetUserId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 1 = 대기 목록에서 사용자 제거
		HarmonyRoomAssignWait assignWait = harmonyRoomAssignWaitRepository.findByHarmonyRoom(harmonyRoom)
			.orElseThrow(() -> new IllegalArgumentException("가입 대기 목록을 찾을 수 없습니다"));

		if (!assignWait.isWaiting(targetUser)) {
			throw new IllegalArgumentException("가입 신청하지 않은 사용자입니다");
		}

		assignWait.removeWaitingUser(targetUser);
		harmonyRoomAssignWaitRepository.save(assignWait);

		// 2 = 멤버로 추가
		HarmonyRoomMembers newMember = HarmonyRoomMembers.createMember(harmonyRoom, targetUser);
		harmonyRoomMembersRepository.save(newMember);

		log.info("✅ 가입 승인 완료: {} → {}", targetUser.getNickname(), harmonyRoom.getName());
	}

	/**
	 * 12-2. 가입 거절
	 */
	public void denyUser(String harmonyId, HarmonyRoomRequest.ApproveOrDeny request) {
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		UUID targetUserId = UUID.fromString(request.getUserID());

		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		User targetUser = userRepository.findById(targetUserId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 대기 목록에서 사용자 제거만
		HarmonyRoomAssignWait assignWait = harmonyRoomAssignWaitRepository.findByHarmonyRoom(harmonyRoom)
			.orElseThrow(() -> new IllegalArgumentException("가입 대기 목록을 찾을 수 없습니다"));

		if (!assignWait.isWaiting(targetUser)) {
			throw new IllegalArgumentException("가입 신청하지 않은 사용자입니다");
		}

		assignWait.removeWaitingUser(targetUser);
		harmonyRoomAssignWaitRepository.save(assignWait);

		log.info("❌ 가입 거절 완료: {} → {}", targetUser.getNickname(), harmonyRoom.getName());
	}


	/**
	 * 13. 하모니룸 즐겨찾기 추가/제거 (토글) - POST /api/harmony/{harmonyID}/bookmark
	 */
	@Transactional
	public HarmonyRoomResponse.BookmarkResult toggleBookmark(String harmonyId, String authHeader) {
		try {
			log.info("🔖 하모니룸 즐겨찾기 토글 시작: {}", harmonyId);

			// 1. 사용자 인증
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

			// 2. 하모니룸 존재 확인 (HarmonyRoom 사용)
			UUID harmonyRoomUuid = UUID.fromString(harmonyId);
			HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomUuid)
				.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

			// 3. 기존 즐겨찾기 확인
			Optional<HarmonyRoomBookmark> existingBookmark =
				harmonyRoomBookmarkRepository.findByUserIdAndHarmonyRoomId(userId, harmonyRoomUuid);

			boolean bookmarked;
			String message;

			if (existingBookmark.isPresent()) {
				// 즐겨찾기 제거
				harmonyRoomBookmarkRepository.delete(existingBookmark.get());
				bookmarked = false;
				message = "즐겨찾기에서 제거되었습니다";
				log.info("📌 즐겨찾기 제거됨 - 사용자: {}, 하모니룸: {}", user.getNickname(), harmonyRoom.getName());
			} else {
				// 즐겨찾기 추가
				HarmonyRoomBookmark bookmark = HarmonyRoomBookmark.create(user, harmonyRoom);
				harmonyRoomBookmarkRepository.save(bookmark);
				bookmarked = true;
				message = "즐겨찾기에 추가되었습니다";
				log.info("⭐ 즐겨찾기 추가됨 - 사용자: {}, 하모니룸: {}", user.getNickname(), harmonyRoom.getName());
			}

			return HarmonyRoomResponse.BookmarkResult.builder()
				.harmonyRoomId(harmonyRoom.getId().toString())  // ✅ 하모니룸 ID 추가
				.harmonyRoomName(harmonyRoom.getName())
				.bookmarked(bookmarked)
				.message(message)
				.build();

		} catch (IllegalArgumentException e) {
			log.error("❌ 즐겨찾기 토글 오류: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("💥 즐겨찾기 토글 처리 실패: {}", e.getMessage(), e);
			throw new RuntimeException("즐겨찾기 처리에 실패했습니다", e);
		}
	}


	/**
	 * 14. 하모니룸 공유
	 */
	// todo : 배포 후 링크 수정
	public HarmonyRoomResponse.Share shareHarmonyRoom(String harmonyId, String authHeader) {
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 딥링크 생성 (메시지 파라미터 제거)
		String deepLink = "melog://harmony/" + harmonyId;
		String webLink = "https://melog.app/harmony/" + harmonyId;
		String storeLink = "https://play.google.com/store/apps/details?id=com.osunji.melog";
		String qrCode = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + deepLink;

		log.info("🔗 하모니룸 공유 링크 생성 완료: {}", harmonyRoom.getName());

		return HarmonyRoomResponse.Share.builder()
			.deepLink(deepLink)
			.webLink(webLink)
			.storeLink(storeLink)
			.qrCode(qrCode)
			.build();
	}

	/**
	 * 15. 하모니룸 신고 (Field 오류 해결 버전)
	 */
	@Transactional
	public void reportHarmony(String harmonyId, HarmonyRoomRequest.Report reportRequest, String authHeader) {
		try {
			log.info("🚨 하모니룸 신고 시작: {} - 사유: {}", harmonyId, reportRequest.getReason());

			// 1. 사용자 인증
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			User reporter = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

			// 2. 하모니룸 존재 확인
			UUID harmonyRoomUuid = UUID.fromString(harmonyId);
			HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomUuid)
				.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

			// 3. 중복 신고 방지 (DB 기반)
			boolean alreadyReported = harmonyRoomReportRepository
				.existsByReporterIdAndHarmonyRoomId(userId, harmonyRoomUuid);

			if (alreadyReported) {
				log.warn("⚠️ 이미 신고한 하모니룸: 사용자={}, 하모니룸={}",
					reporter.getNickname(), harmonyRoom.getName());
				throw new IllegalArgumentException("이미 신고한 하모니룸입니다");
			}

			// 4. 신고 기록 저장 (DB)
			HarmonyRoomReport report = HarmonyRoomReport.create(
				reporter,
				harmonyRoom,
				reportRequest.getReason(),
				reportRequest.getCategory(),
				reportRequest.getDetails()
			);

			harmonyRoomReportRepository.save(report);
			log.info("📝 신고 기록 DB 저장 완료: {} - 신고자: {}", report.getId(), reporter.getNickname());

			// 5. ✅ ElasticSearch에 안전한 로그 기록 (Field 오류 해결)
			try {
				harmonyReportLogService.logHarmonyReportByCategory(
					report.getId().toString(),      // reportId
					harmonyRoom.getId().toString(), // harmonyId
					harmonyRoom.getName(),          // harmonyName (한글 지원)
					reporter.getId().toString(),    // reporterId
					reportRequest.getReason(),      // reason (한글 지원)
					reportRequest.getCategory(),    // category
					reportRequest.getDetails()      // details (한글 지원, null 허용)
				);
				log.info("📊 ElasticSearch 신고 로그 기록 완료");
			} catch (Exception e) {
				log.warn("⚠️ ElasticSearch 로그 기록 실패: {}", e.getMessage());
				// ElasticSearch 실패해도 신고는 정상 처리
			}

			// 6. 통계 업데이트
			try {
				harmonyReportLogService.logReportStatistics(harmonyId);
			} catch (Exception e) {
				log.warn("신고 통계 업데이트 실패: {}", e.getMessage());
			}

		} catch (IllegalArgumentException e) {
			log.error("❌ 신고 처리 오류: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("💥 신고 처리 실패: {}", e.getMessage(), e);
			throw new RuntimeException("신고 처리에 실패했습니다", e);
		}
	}


	/**
	 * ElasticSearch에 신고 로그 기록
	 */
	private void logReportToElasticsearch(HarmonyRoomReport report) {
		try {
			Map<String, Object> logData = Map.of(
				"type", "harmony_report",
				"harmonyId", report.getHarmonyRoom().getId().toString(),
				"harmonyName", report.getHarmonyRoom().getName(),
				"reporterId", report.getReporter().getId().toString(),
				"reporterNickname", report.getReporter().getNickname(),
				"reason", report.getReason(),
				"category", report.getCategory(),
				"details", report.getDetails() != null ? report.getDetails() : "",
				"timestamp", report.getReportedAt().toString(),
				"serverTime", LocalDateTime.now().toString()
			);

			// TODO: 실제 ElasticSearch 클라이언트 구현
			// elasticsearchOperations.index(IndexRequest.of(i -> i
			//     .index("harmony-reports")
			//     .document(logData)
			// ));

			log.info("📊 ElasticSearch 신고 로그 데이터: {}", logData);

		} catch (Exception e) {
			log.error("⚠️ ElasticSearch 로깅 실패: {}", e.getMessage());
			throw e;
		}
	}

	/**
	 * 16. 하모니룸 가입 신청
	 */
	public String joinHarmonyRoom(String harmonyId, String authHeader) {
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 이미 멤버인지 확인
		if (harmonyRoomMembersRepository.existsByHarmonyRoomAndUser(harmonyRoom, user)) {
			return "이미 멤버입니다";
		}

		// 이미 신청했는지 확인
		if (harmonyRoomAssignWaitRepository.existsByHarmonyRoomAndUser(harmonyRoom, user)) {
			return "이미 신청중입니다";
		}

		if (harmonyRoom.getIsDirectAssign()) {
			// 바로 승인인 경우 멤버로 추가
			HarmonyRoomMembers newMember = HarmonyRoomMembers.createMember(harmonyRoom, user);
			harmonyRoomMembersRepository.save(newMember);
			log.info("🚪 바로 가입 완료: {} → {}", user.getNickname(), harmonyRoom.getName());
			return "가입 완료";
		} else {
			// 승인 필요한 경우 대기 목록에 추가
			HarmonyRoomAssignWait assignWait = harmonyRoomAssignWaitRepository.findByHarmonyRoom(harmonyRoom)
				.orElseThrow(() -> new IllegalArgumentException("가입 대기 목록을 찾을 수 없습니다"));

			assignWait.addWaitingUser(user);
			harmonyRoomAssignWaitRepository.save(assignWait);
			log.info("🚪 가입 신청 완료: {} → {}", user.getNickname(), harmonyRoom.getName());
			return "가입 신청완료";
		}
	}

	/**
	 * 17. 하모니룸 탈퇴
	 */
	public void leaveHarmonyRoom(String harmonyId, String authHeader) {
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 소유자는 탈퇴할 수 없음
		if (harmonyRoom.isOwner(user)) {
			throw new SecurityException("하모니룸 소유자는 탈퇴할 수 없습니다");
		}

		// 멤버가 아니면 에러
		if (!harmonyRoomMembersRepository.existsByHarmonyRoomAndUser(harmonyRoom, user)) {
			throw new IllegalArgumentException("하모니룸의 멤버가 아닙니다");
		}

		// 멤버십 삭제
		harmonyRoomMembersRepository.deleteByHarmonyRoomAndUser(harmonyRoom, user);

		log.info("🚪 하모니룸 탈퇴 완료: {} ← {}", harmonyRoom.getName(), user.getNickname());
	}

	/**
	 * 18. 하모니룸 내부 피드 추천
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.HarmonyRoomPosts getRecommendPosts(String harmonyId, String authHeader) {
		// getHarmonyRoomPosts와 동일한 로직
		return getHarmonyRoomPosts(harmonyId,authHeader);
	}

	@Transactional(readOnly = true)
	public List<HarmonyRoomResponse.Simple> searchHarmonyRooms(String keyword) {

		if (keyword == null || keyword.isBlank()) throw new IllegalArgumentException("검색어를 입력하세요");

		List<HarmonyRoom> rooms = harmonyRoomRepository.searchByKeyword(keyword.trim());

		// 원하는 형태로 DTO 변환
		return rooms.stream()
			.map(room -> HarmonyRoomResponse.Simple.builder()
				.id(room.getId().toString())
				.name(room.getName())
				.intro(room.getIntro())
				.category(room.getCategory())
				.profileImgLink(room.getProfileImageUrl())
				.build())
			.collect(Collectors.toList());
	}


	// ========== 헬퍼 메서드 ==========

	/**
	 * 시간 계산 ("오늘" 또는 "n일전") - LocalDateTime 버전
	 */
	private String calculateCreatedAgo(LocalDateTime createdAt) {
		if (createdAt == null) return "알 수 없음";

		LocalDateTime now = LocalDateTime.now();
		long daysBetween = ChronoUnit.DAYS.between(createdAt.toLocalDate(), now.toLocalDate());

		if (daysBetween == 0) {
			return "오늘";
		} else {
			return daysBetween + "일전";
		}
	}



	// ========== 게시글 단건 상세 조회 ==========
	@Transactional(readOnly = true)
	public ApiMessage<HarmonyRoomResponse.PostDetail> getHarmonyPostDetail(String harmonyPostIdStr, String authHeader) {
		try {
			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);
			UUID userId = null;
			try {
				if (authHeader != null) {
					userId = authHelper.authHelperAsUUID(authHeader);
				}
			} catch (Exception ignored) {}

			final UUID finalUserId = userId;  // 람다에서 사용할 final 변수로 복사

			HarmonyRoomPosts post = harmonyRoomPostsRepository.findByIdWithAssociations(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			boolean isLiked = false;
			boolean isBookmarked = false;
			if (finalUserId != null) {
				User user = userRepository.findById(finalUserId).orElse(null);
				if (user != null) {
					isLiked = post.getLikes().stream().anyMatch(like -> like.getUser().getId().equals(finalUserId));
					isBookmarked = post.getBookmarks().stream().anyMatch(b -> b.getUser().getId().equals(finalUserId));
				}
			}


			int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
			int commentCount = post.getComments() != null ? post.getComments().size() : 0;

			Integer createdAgo = (post.getCreatedAt() == null) ? 0 :
				(int) ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());

			HarmonyRoomResponse.PostDetail postDetail = HarmonyRoomResponse.PostDetail.builder()
				.id(post.getId().toString())
				.content(post.getContent())
				.mediaType(post.getMediaType())
				.mediaUrl(post.getMediaUrl())
				.tags(post.getTags())
				.createdAgo(createdAgo)
				.likeCount(likeCount)
				.commentCount(commentCount)
				.isLiked(isLiked)
				.isBookmarked(isBookmarked)
				.build();

			return ApiMessage.success(200, "게시글 상세 조회 성공", postDetail);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "게시글 상세 조회 실패: " + e.getMessage());
		}
	}
	// ========== 게시글 전체 댓글 조회 ==========
	@Transactional(readOnly = true)
	public ApiMessage<HarmonyRoomResponse.HarmonyRoomPostComments> getHarmonyPostComments(String harmonyPostIdStr, String authHeader) {
		try {
			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);
			// 최상위 댓글 조회 (자식 댓글은 재귀적으로 CommentData 내 replies 필드로 포함)
			List<HarmonyPostComment> rootComments = harmonyCommentRepository.findRootCommentsByPostId(harmonyPostId);

			List<HarmonyRoomResponse.HarmonyRoomPostComments.CommentData> commentDataList =
				rootComments.stream()
					.map(this::toCommentData)
					.collect(Collectors.toList());

			HarmonyRoomResponse.HarmonyRoomPostComments response =
				HarmonyRoomResponse.HarmonyRoomPostComments.builder()
					.comments(commentDataList)
					.build();

			return ApiMessage.success(200, "댓글 목록 조회 성공", response);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 목록 조회 실패: " + e.getMessage());
		}
	}

	// 댓글 DTO 변환 메서드 (재귀 처리)
	private HarmonyRoomResponse.HarmonyRoomPostComments.CommentData toCommentData(HarmonyPostComment comment) {
		List<HarmonyRoomResponse.HarmonyRoomPostComments.CommentData> childCommentDtos = comment.getChildComments().stream()
			.map(this::toCommentData) // 재귀 호출
			.collect(Collectors.toList());

		return HarmonyRoomResponse.HarmonyRoomPostComments.CommentData.builder()
			.id(comment.getId().toString())
			.content(comment.getContent())
			.userId(comment.getUser().getId().toString())
			.userNickname(comment.getUser().getNickname())
			.userProfileImgLink(comment.getUser().getProfileImageUrl())
			.likeCount(comment.getLikeCount())
			.createdAgo(calculateCreatedAgo(comment.getCreatedAt().atStartOfDay()))
			.replies(childCommentDtos)
			.build();
	}




	// ========== 게시글 베스트 댓글 조회 ==========
	@Transactional(readOnly = true)
	public ApiMessage<HarmonyRoomResponse.HarmonyRoomBestComment> getBestHarmonyPostComment(String harmonyPostIdStr) {
		try {
			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);
			Optional<HarmonyPostComment> bestCommentOpt = harmonyCommentRepository.findBestComment(harmonyPostId);

			if (bestCommentOpt.isEmpty()) {
				return ApiMessage.success(200, "베스트 댓글이 없습니다.", null);
			}

			HarmonyPostComment bestComment = bestCommentOpt.get();

			HarmonyRoomResponse.HarmonyRoomBestComment bestDto = HarmonyRoomResponse.HarmonyRoomBestComment.builder()
				.id(bestComment.getId().toString())
				.content(bestComment.getContent())
				.userId(bestComment.getUser().getId().toString())              // userID → userId 변경
				.userNickname(bestComment.getUser().getNickname())            // profileUrl 대신 userNickname 사용
				.likeCount(bestComment.getLikeCount())                        // likes → likeCount 변경
				.build();

			return ApiMessage.success(200, "베스트 댓글 조회 성공", bestDto);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "베스트 댓글 조회 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 좋아요/취소 ==========
	public ApiMessage<Void> likeOrUnlikeHarmonyPost(String harmonyPostIdStr, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);
			HarmonyRoomPosts post = harmonyRoomPostsRepository.findByIdWithLikes(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			boolean wasLiked = post.getLikes().stream().anyMatch(like -> like.getUser().getId().equals(userId));

			if (wasLiked) {
				// 좋아요 취소
				post.getLikes().removeIf(like -> like.getUser().getId().equals(userId));
			} else {
				// 좋아요 추가
				HarmonyPostLike like = HarmonyPostLike.builder()
					.harmonyPost(post)
					.user(user)
					.build();
				post.getLikes().add(like);
			}

			harmonyRoomPostsRepository.save(post);

			int newLikeCount = post.getLikes().size();
			String action = wasLiked ? "취소" : "추가";
			String message = String.format("좋아요가 %s되었습니다. (현재 %d개)", action, newLikeCount);

			return ApiMessage.success(200, message, null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "좋아요 처리 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 좋아요 여부 조회 ==========
	@Transactional(readOnly = true)
	public ApiMessage<Boolean> isHarmonyPostLiked(String harmonyPostIdStr, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);

			HarmonyRoomPosts post = harmonyRoomPostsRepository.findById(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			boolean liked = post.getLikes().stream()
				.anyMatch(like -> like.getUser().getId().equals(userId));

			return ApiMessage.success(200, "좋아요 여부 조회 성공", liked);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "좋아요 여부 조회 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 북마크 추가 ==========
	public ApiMessage<Void> addHarmonyPostBookmark(String harmonyPostIdStr, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);
			HarmonyRoomPosts post = harmonyRoomPostsRepository.findById(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			boolean alreadyBookmarked = post.getBookmarks().stream()
				.anyMatch(bookmark -> bookmark.getUser().getId().equals(userId));
			if (alreadyBookmarked) {
				return ApiMessage.fail(409, "이미 북마크한 게시글입니다.");
			}

			HarmonyPostBookmark bookmark = HarmonyPostBookmark.builder()
				.harmonyPost(post)
				.user(user)
				.build();
			post.getBookmarks().add(bookmark);

			harmonyRoomPostsRepository.save(post);

			return ApiMessage.success(201, "북마크가 성공적으로 추가되었습니다.", null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "북마크 추가 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 북마크 제거 ==========
	public ApiMessage<Void> removeHarmonyPostBookmark(String harmonyPostIdStr, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);

			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);
			HarmonyRoomPosts post = harmonyRoomPostsRepository.findById(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			Optional<HarmonyPostBookmark> bookmarkOpt = post.getBookmarks().stream()
				.filter(bookmark -> bookmark.getUser().getId().equals(userId))
				.findFirst();

			if (bookmarkOpt.isEmpty()) {
				return ApiMessage.fail(404, "북마크 정보가 존재하지 않습니다.");
			}

			post.getBookmarks().remove(bookmarkOpt.get());

			harmonyRoomPostsRepository.save(post);

			return ApiMessage.success(200, "북마크가 성공적으로 제거되었습니다.", null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "북마크 제거 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 삭제 ==========
	public ApiMessage<Void> deleteHarmonyPost(String harmonyPostIdStr, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);

			HarmonyRoomPosts post = harmonyRoomPostsRepository.findByIdWithAssociations(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			if (!post.getUser().getId().equals(userId)) {
				return ApiMessage.fail(403, "삭제 권한이 없습니다.");
			}

			harmonyRoomPostsRepository.delete(post);

			return ApiMessage.success(200, "게시글이 성공적으로 삭제되었습니다.", null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "게시글 삭제 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 수정 ==========
	public ApiMessage<Void> updateHarmonyPost(String harmonyPostIdStr, HarmonyRoomRequest.UpdateHarmonyPost request, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);

			HarmonyRoomPosts post = harmonyRoomPostsRepository.findByIdWithAssociations(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			if (!post.getUser().getId().equals(userId)) {
				return ApiMessage.fail(403, "수정 권한이 없습니다.");
			}

			if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
				post.setContent(request.getContent());
			}
			if (request.getMediaType() != null) {
				post.setMediaType(request.getMediaType());
			}
			if (request.getMediaUrl() != null) {
				post.setMediaUrl(request.getMediaUrl());
			}
			if (request.getTags() != null) {
				post.getTags().clear();
				post.getTags().addAll(request.getTags());
			}

			harmonyRoomPostsRepository.save(post);

			return ApiMessage.success(200, "게시글이 성공적으로 수정되었습니다.", null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "게시글 수정 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 댓글 작성 ==========
	public ApiMessage<Void> createHarmonyPostComment(String harmonyPostIdStr, HarmonyRoomRequest.CreateComment request, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			UUID harmonyPostId = UUID.fromString(harmonyPostIdStr);
			HarmonyRoomPosts post = harmonyRoomPostsRepository.findById(harmonyPostId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

			HarmonyPostComment parentComment = null;
			if (request.getResponseTo() != null && !request.getResponseTo().trim().isEmpty()) {
				UUID parentId = UUID.fromString(request.getResponseTo());
				parentComment = harmonyCommentRepository.findById(parentId)
					.orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
			}


			HarmonyPostComment comment = (parentComment == null)
				? HarmonyPostComment.createComment(user, post, request.getContent())
				: HarmonyPostComment.createReply(user, post, request.getContent(), parentComment);

			harmonyCommentRepository.save(comment);

			return ApiMessage.success(201, "댓글이 성공적으로 작성되었습니다.", null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 작성 실패: " + e.getMessage());
		}
	}

	// ========== 게시글 댓글 삭제 ==========
	public ApiMessage<Void> deleteHarmonyPostComment(String harmonyPostIdStr, String commentIdStr, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			UUID commentId = UUID.fromString(commentIdStr);

			HarmonyPostComment comment = harmonyCommentRepository.findById(commentId)
				.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

			if (!comment.getUser().getId().equals(userId)) {
				return ApiMessage.fail(403, "삭제 권한이 없습니다.");
			}

			harmonyCommentRepository.delete(comment);

			return ApiMessage.success(200, "댓글이 성공적으로 삭제되었습니다.", null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 삭제 실패: " + e.getMessage());
		}
	}

	// ========== 댓글 좋아요/취소 ==========
	public ApiMessage<Void> likeOrUnlikeHarmonyComment(String harmonyPostIdStr, String commentIdStr, String authHeader) {
		try {
			UUID userId = authHelper.authHelperAsUUID(authHeader);
			User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

			UUID commentId = UUID.fromString(commentIdStr);
			HarmonyPostComment comment = harmonyCommentRepository.findById(commentId)
				.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

			if (comment.isLikedBy(user)) {
				comment.removeLike(user);
			} else {
				comment.addLike(user);
			}

			harmonyCommentRepository.save(comment);

			return ApiMessage.success(200, "댓글 좋아요/취소 완료", null);
		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, e.getMessage());
		} catch (IllegalStateException e) {
			return ApiMessage.fail(401, e.getMessage());
		} catch (Exception e) {
			return ApiMessage.fail(500, "댓글 좋아요 처리 실패: " + e.getMessage());
		}
	}
	// 유저별 하모니룸 게시글 목록 조회
	@Transactional(readOnly = true)
	public ApiMessage<HarmonyRoomResponse.UserHarmonyPosts> getUserHarmonyPosts(String userIdStr, String authHeader) {
		try {
			UUID userId = UUID.fromString(userIdStr);
			UUID currentUserId = null;
			if (authHeader != null) {
				try {
					currentUserId = authHelper.authHelperAsUUID(authHeader);
				} catch (Exception ignored) {}
			}

			List<HarmonyRoomPosts> posts = harmonyRoomPostsRepository.findByUserIdOrderByCreatedAtDesc(userId, currentUserId);

			List<HarmonyRoomResponse.UserHarmonyPosts.UserPostData> userPostList = posts.stream()
				.map(post -> {
					int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
					int commentCount = post.getComments() != null ? post.getComments().size() : 0;
					String createdAgo = calculateCreatedAgo(post.getCreatedAt());

					return HarmonyRoomResponse.UserHarmonyPosts.UserPostData.builder()
						.id(post.getId().toString())
						.content(post.getContent())
						.mediaType(post.getMediaType())
						.mediaUrl(post.getMediaUrl())
						.likeCount(likeCount)
						.commentCount(commentCount)
						.createdAgo(createdAgo)
						.build();
				})
				.toList();

			HarmonyRoomResponse.UserHarmonyPosts response = HarmonyRoomResponse.UserHarmonyPosts.builder()
				.results(userPostList)
				.build();

			return ApiMessage.success(200, "사용자 하모니룸 게시글 조회 성공", response);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다.");
		} catch (Exception e) {
			return ApiMessage.fail(500, "사용자 하모니룸 게시글 조회 실패: " + e.getMessage());
		}
	}

	// 유저별 하모니룸 북마크 게시글 목록 조회
	@Transactional(readOnly = true)
	public ApiMessage<HarmonyRoomResponse.UserHarmonyBookmarks> getHarmonyUserBookmarks(String userIdStr, String authHeader) {
		try {
			UUID userId = UUID.fromString(userIdStr);

			List<HarmonyPostBookmark> bookmarks = harmonyRoomBookmarkRepository.findBookmarkAllByUserId(userId);

			List<HarmonyRoomResponse.UserHarmonyBookmarks.UserBookmarkData> bookmarkList = bookmarks.stream()
				.map(bookmark -> {
					String createdAgo = calculateCreatedAgo(bookmark.getCreatedAt());
					return HarmonyRoomResponse.UserHarmonyBookmarks.UserBookmarkData.builder()
						.postId(bookmark.getHarmonyPost().getId().toString())
						.title(bookmark.getHarmonyPost().getContent())  // 하모니룸 게시글은 title 대신 content 사용
						.mediaUrl(bookmark.getHarmonyPost().getMediaUrl())
						.mediaType(bookmark.getHarmonyPost().getMediaType())
						.createdAgo(createdAgo)
						.build();
				})
				.toList();

			HarmonyRoomResponse.UserHarmonyBookmarks response = HarmonyRoomResponse.UserHarmonyBookmarks.builder()
				.results(bookmarkList)
				.build();

			return ApiMessage.success(200, "사용자 하모니룸 북마크 게시글 조회 성공", response);

		} catch (IllegalArgumentException e) {
			return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다.");
		} catch (Exception e) {
			return ApiMessage.fail(500, "사용자 하모니룸 북마크 게시글 조회 실패: " + e.getMessage());
		}}
}
