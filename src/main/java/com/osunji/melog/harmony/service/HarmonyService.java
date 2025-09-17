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
import com.osunji.melog.user.UserRepository;
import com.osunji.melog.review.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
	private final AuthHelper authHelper;
	private final PostService postService;
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

		// 3 = 게시글 목록 생성
		HarmonyRoomPosts harmonyRoomPosts = HarmonyRoomPosts.create(harmonyRoom);
		harmonyRoomPostsRepository.save(harmonyRoomPosts);

		// 4 = 가입 대기 목록 생성
		HarmonyRoomAssignWait assignWait = HarmonyRoomAssignWait.create(harmonyRoom);
		harmonyRoomAssignWaitRepository.save(assignWait);

		log.info("✅ SERVICE LINE 41 : 하모니룸 생성 완료: {} (소유자: {})", harmonyRoom.getName(), user.getNickname());
	}

	/**
	 * 2. 나의 하모니룸 조회
	 */
	//todo : 추후 즐겨찾기 한 하모니룸 추가
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

		log.info("📋SERVICE LINE 75 : 나의 하모니룸 조회 완료: 생성 하모니룸  {}개, 멤버인 하모니룸  {}개", myHarmony.size(), harmony.size());

		return HarmonyRoomResponse.MyHarmony.builder()
			.myHarmony(myHarmony)
			.harmony(harmony)
			.bookmarkHarmony(List.of()) // 북마크 기능 빈리스트로 리턴
			.build();
	}

	/**
	 * 3. 최근 업로드 미디어 조회
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.RecentMedia getRecentMedia(String authHeader) {
		// 0 = 토큰으로 유저 인식 및 유저 체크
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		// 1 = 내가 속한 하모니룸들 조회
		List<HarmonyRoomMembers> membershipList = harmonyRoomMembersRepository.findByUser(user);
		List<HarmonyRoom> myHarmonyRooms = membershipList.stream()
			.map(HarmonyRoomMembers::getHarmonyRoom)
			.collect(Collectors.toList());
		// 1.1 = 속한 하모니룸이 없을 때 예외
		if (myHarmonyRooms.isEmpty()) {
			log.info("📺 속한 하모니룸이 없어서 최근 미디어 없음");
			return HarmonyRoomResponse.RecentMedia.builder()
				.recentMedia(List.of())
				.build();
		}

		// 2 = 하모니룸들의 게시글 조회
		List<HarmonyRoomPosts> harmonyRoomPostsList = harmonyRoomPostsRepository.findByHarmonyRoomIn(myHarmonyRooms);

		List<HarmonyRoomResponse.RecentMedia.RecentMediaInfo> recentMediaList = new ArrayList<>();

		for (HarmonyRoomPosts harmonyRoomPosts : harmonyRoomPostsList) {
			List<String> postIds = harmonyRoomPosts.getPostIds();
			if (postIds.isEmpty()) continue;

			// 3 =  게시글 ID들을 UUID로 변환
			List<UUID> postUuids = postIds.stream()
				.map(UUID::fromString)
				.collect(Collectors.toList());

			// 4 = 게시글들 조회하고 YouTube 미디어만 필터링
			List<Post> posts = postRepository.findAllById(postUuids);
			posts.stream()
				.filter(post -> "youtube".equals(post.getMediaType()) && post.getMediaUrl() != null)
				.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // ✅ 시간까지 정확한 정렬
				.forEach(post -> {
					String createdAgo = calculateCreatedAgo(post.getCreatedAt());
					recentMediaList.add(HarmonyRoomResponse.RecentMedia.RecentMediaInfo.builder()
						.userNickname(post.getUser().getNickname())
						.userProfileImgLink(post.getUser().getProfileImageUrl())
						.harmonyRoomName(harmonyRoomPosts.getHarmonyRoom().getName())
						.postID(post.getId().toString())
						.mediaUrl(post.getMediaUrl())
						.mediaType(post.getMediaType())
						.createdAgo(createdAgo)
						.build());
				});
		}

		log.info("📺 SERVICELine 119 최근 미디어 조회 완료: {}개", recentMediaList.size());

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
	 * 5. 하모니룸 게시글 조회
	 */
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.HarmonyRoomPosts getHarmonyRoomPosts(String harmonyId) {
		// 0 = 하모니룸 아이디로 하모니룸 찾기
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 1 = 하모니룸의 게시글 목록 조회
		Optional<HarmonyRoomPosts> harmonyRoomPostsOpt = harmonyRoomPostsRepository.findByHarmonyRoom(harmonyRoom);
		if (harmonyRoomPostsOpt.isEmpty() || harmonyRoomPostsOpt.get().getPostIds().isEmpty()) {
			log.info("📝 하모니룸 {}에 게시글이 없음", harmonyRoom.getName());
			return HarmonyRoomResponse.HarmonyRoomPosts.builder()
				.recommend(List.of())
				.popular(List.of())
				.build();
		}

		List<String> postIds = harmonyRoomPostsOpt.get().getPostIds();
		List<UUID> postUuids = postIds.stream()
			.map(UUID::fromString)
			.collect(Collectors.toList());

		List<Post> posts = postRepository.findAllById(postUuids);
		//todo: 추후 추천 post 리턴하기
		// 2 = 추천 (최신순)
		List<HarmonyRoomResponse.HarmonyRoomPosts.PostResult> recommend = posts.stream()
			.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
			.map(this::createPostResult)
			.collect(Collectors.toList());

		// 3 =  인기 (좋아요순)
		List<HarmonyRoomResponse.HarmonyRoomPosts.PostResult> popular = posts.stream()
			.sorted((a, b) -> {
				int likesA = a.getLikes() != null ? a.getLikes().size() : 0;
				int likesB = b.getLikes() != null ? b.getLikes().size() : 0;
				return Integer.compare(likesB, likesA);
			})
			.map(this::createPostResult)
			.collect(Collectors.toList());

		log.info("📝 하모니룸 게시글 조회 완료: {}개", posts.size());

		return HarmonyRoomResponse.HarmonyRoomPosts.builder()
			.recommend(recommend)
			.popular(popular)
			.build();
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
	// todo : 즐겨찾기(북마크) 기능 추가
	@Transactional(readOnly = true)
	public HarmonyRoomResponse.Detail getHarmonyRoomDetail(String harmonyId, String authHeader) {
		// 0 = ㅇㅇ유저그거
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
		// 1 = 하모니룸 체크
		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 2 = 멤버 수 조회
		Long memberCount = harmonyRoomMembersRepository.countByHarmonyRoom(harmonyRoom);

		// 3 = 랭킹 조회 (북마크 수 기준)
		Long ranking = harmonyRoomRepository.findRankingByBookMarkNum(harmonyRoom.getBookMarkNum());

		// 4 = 게시글 수 조회
		Optional<HarmonyRoomPosts> harmonyRoomPostsOpt = harmonyRoomPostsRepository.findByHarmonyRoom(harmonyRoom);
		int postCount = harmonyRoomPostsOpt.map(posts -> posts.getPostIds().size()).orElse(0);

		// 5 = 내가 멤버인지 확인
		boolean isAssign = harmonyRoomMembersRepository.existsByHarmonyRoomAndUser(harmonyRoom, user);

		log.info("📋 하모니룸 상세정보 조회 완료: {} (멤버 {}명, 랭킹 {}위)",
			harmonyRoom.getName(), memberCount, ranking);

		return HarmonyRoomResponse.Detail.builder()
			.profileImgLink(harmonyRoom.getProfileImageUrl())
			.name(harmonyRoom.getName())
			.category(harmonyRoom.getCategory())
			.intro(harmonyRoom.getIntro())
			.memberNum(memberCount.intValue())
			.ranking(ranking.intValue())
			.countPosts(postCount)
			.isBookmark(false) // 북마크 기능 제거
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
			request.getIsDirectAssign()
		);

		harmonyRoomRepository.save(harmonyRoom);

		log.info("✏️ 하모니룸 수정 완료: {}", harmonyRoom.getName());
	}

	/**
	 * 10. 하모니룸 삭제
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

		// 연관 데이터 삭제
		harmonyRoomPostsRepository.findByHarmonyRoom(harmonyRoom).ifPresent(harmonyRoomPostsRepository::delete);
		harmonyRoomAssignWaitRepository.findByHarmonyRoom(harmonyRoom).ifPresent(harmonyRoomAssignWaitRepository::delete);

		List<HarmonyRoomMembers> members = harmonyRoomMembersRepository.findByHarmonyRoom(harmonyRoom);
		harmonyRoomMembersRepository.deleteAll(members);

		// 하모니룸 삭제
		harmonyRoomRepository.delete(harmonyRoom);

		// TODO: ElasticSearch에 폐쇄 사유 로그 저장
		log.info("🗑️ 하모니룸 삭제 완료: {} (사유: {})", roomName, request.getReason());
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
		return getHarmonyRoomPosts(harmonyId);
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


	/**
	 * Post Entity를 PostResult DTO로 변환
	 */
	private HarmonyRoomResponse.HarmonyRoomPosts.PostResult createPostResult(Post post) {
		return HarmonyRoomResponse.HarmonyRoomPosts.PostResult.builder()
			.post(HarmonyRoomResponse.HarmonyRoomPosts.PostResult.PostDetail.builder()
				.id(post.getId().toString())
				.title(post.getTitle())
				.content(post.getContent())
				.mediaType(post.getMediaType())
				.mediaUrl(post.getMediaUrl())
				.tags(post.getTags())
				.createdAgo(calculateHoursFromDateTime(post.getCreatedAt()))
				.likeCount(post.getLikes() != null ? post.getLikes().size() : 0)
				.hiddenUser(post.getHiddenUsers() != null ?
					post.getHiddenUsers().stream()
						.map(user -> user.getId().toString())
						.collect(Collectors.toList()) : List.of())
				.commentCount(getCommentCount(post))
				.bestComment(getBestComment(post))
				.build())
			.user(HarmonyRoomResponse.HarmonyRoomPosts.PostResult.UserInfo.builder()
				.id(post.getUser().getId().toString())
				.nickName(post.getUser().getNickname())
				.profileImg(post.getUser().getProfileImageUrl())
				.build())
			.build();
	}
	/**
	 * LocalDateTime을 일수로 변환
	 */
	private Integer calculateDaysFromDateTime(LocalDateTime createdAt) {
		if (createdAt == null) return 0;

		LocalDateTime now = LocalDateTime.now();
		return (int) ChronoUnit.DAYS.between(createdAt.toLocalDate(), now.toLocalDate());
	}
	/**
	 * LocalDate를 시간으로 변환
	 */
	private Integer calculateHoursFromDateTime(LocalDateTime createdAt) {
		if (createdAt == null) return 0;

		LocalDateTime now = LocalDateTime.now();
		return (int) ChronoUnit.HOURS.between(createdAt, now);
	}


	/**
	 * 댓글 수 조회 - CommentRepository 활용 ✅
	 */
	private Integer getCommentCount(Post post) {
		try {
			return commentRepository.countCommentByPostId(post.getId());
		} catch (Exception e) {
			log.error("댓글 수 조회 실패: {}", e.getMessage());
			return 0;
		}
	}

	/**
	 * 베스트 댓글 조회 - CommentRepository 활용 ✅
	 */
	private HarmonyRoomResponse.HarmonyRoomPosts.PostResult.PostDetail.BestComment getBestComment(Post post) {
		try {
			Optional<PostComment> bestCommentOpt = commentRepository.findBestComment(post.getId());

			if (bestCommentOpt.isEmpty()) {
				return null;
			}

			PostComment bestComment = bestCommentOpt.get();
			return HarmonyRoomResponse.HarmonyRoomPosts.PostResult.PostDetail.BestComment.builder()
				.userId(bestComment.getUser().getId().toString())
				.content(bestComment.getContent())
				.build();

		} catch (Exception e) {
			log.error("베스트 댓글 조회 실패: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 하모니룸 게시글 생성 (PostService 활용)
	 */
	public void createHarmonyRoomPost(String harmonyId, PostRequest.Create request, String authHeader) {
		log.info("📝 하모니룸 게시글 생성 시작: {}", harmonyId);

		// 1. 사용자 및 하모니룸 조회
		UUID userId = authHelper.authHelperAsUUID(authHeader);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

		UUID harmonyRoomId = UUID.fromString(harmonyId);
		HarmonyRoom harmonyRoom = harmonyRoomRepository.findById(harmonyRoomId)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸을 찾을 수 없습니다"));

		// 2. 멤버 권한 확인
		if (!harmonyRoomMembersRepository.existsByHarmonyRoomAndUser(harmonyRoom, user)) {
			throw new SecurityException("하모니룸 멤버만 게시글을 작성할 수 있습니다");
		}

		// 3. PostService로 게시글 생성 (기존 로직 활용)
		log.info("📝 PostService를 통한 게시글 생성 시작");
		ApiMessage<String> createResult = postService.createPost(request, authHeader);

		if (!createResult.isSuccess()) {
			throw new RuntimeException("게시글 생성 실패: " + createResult.getMessage());
		}

		// 4. 생성된 게시글 ID 가져오기 ✅ 이제 간단함
		String postId = createResult.getData();
		log.info("📝 생성된 게시글 ID: {}", postId);

		// 5. HarmonyRoomPosts에 게시글 ID 추가
		HarmonyRoomPosts harmonyRoomPosts = harmonyRoomPostsRepository.findByHarmonyRoom(harmonyRoom)
			.orElseThrow(() -> new IllegalArgumentException("하모니룸 게시글 목록을 찾을 수 없습니다"));

		harmonyRoomPosts.getPostIds().add(postId);
		harmonyRoomPostsRepository.save(harmonyRoomPosts);

		log.info("✅ 하모니룸 게시글 생성 완료: {} (하모니룸: {})", postId, harmonyRoom.getName());
	}

	/**
	 * PostService 응답에서 게시글 ID 추출
	 */
	private String extractPostIdFromResponse(String responseData) {
		try {
			// "게시글이 생성되었습니다. ID: uuid-string" 형태에서 ID 추출
			if (responseData.contains("ID: ")) {
				return responseData.split("ID: ")[1].trim();
			}
			throw new RuntimeException("응답에서 게시글 ID를 찾을 수 없습니다");
		} catch (Exception e) {
			throw new RuntimeException("게시글 ID 추출 실패: " + e.getMessage());
		}
	}
}
