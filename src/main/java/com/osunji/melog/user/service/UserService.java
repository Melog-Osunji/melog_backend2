package com.osunji.melog.user.service;

import com.osunji.melog.feed.util.BuildFeedUtil;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.util.DtoMapperUtil;
import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.entity.HarmonyRoomMembers;
import com.osunji.melog.harmony.repository.HarmonyRoomBookmarkRepository;
import com.osunji.melog.harmony.repository.HarmonyRoomRepository;
import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.service.BookmarkService;
import com.osunji.melog.review.service.PostService;
import com.osunji.melog.user.domain.Agreement;
import com.osunji.melog.user.domain.Follow;
import com.osunji.melog.user.domain.Onboarding;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.FollowStatus;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.ResignationResponseDTO;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AgreementRepository agreementRepository;
    private final OnboardingRepository onboardingRepository;
    private final FollowRepository followRepository;
    private final HarmonyRoomRepository harmonyRoomRepository;
    private final HarmonyRoomBookmarkRepository harmonyRoomBookmarkRepository;
    private final PostService postService;
    private final UserProfileMusicService userProfileMusicService;
    private final BookmarkService bookmarkService;
    private final DtoMapperUtil dtoMapperUtil;
    private final BlockRepository blockRepository;
    private final BuildFeedUtil buildFeedUtil;
    private final PostRepository postRepository;

    public UserService(UserRepository userRepository, AgreementRepository agreementRepository, OnboardingRepository onboardingRepository, FollowRepository followRepository, HarmonyRoomRepository harmonyRoomRepository, HarmonyRoomBookmarkRepository harmonyRoomBookmarkRepository, PostService postService, UserProfileMusicService userProfileMusicService, BookmarkService bookmarkService, DtoMapperUtil dtoMapperUtil, BlockRepository blockRepository, BuildFeedUtil buildFeedUtil, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.agreementRepository = agreementRepository;
        this.onboardingRepository = onboardingRepository;
        this.followRepository = followRepository;
        this.harmonyRoomRepository = harmonyRoomRepository;
        this.harmonyRoomBookmarkRepository = harmonyRoomBookmarkRepository;
        this.postService = postService;
        this.userProfileMusicService = userProfileMusicService;
        this.bookmarkService = bookmarkService;
        this.dtoMapperUtil = dtoMapperUtil;
        this.blockRepository = blockRepository;
        this.buildFeedUtil = buildFeedUtil;
        this.postRepository = postRepository;
    }

    private static final Set<String> PROFILE_UPDATABLE_FIELDS = Set.of(
            "intro", "nickName", "profileImg"
    );

    @Transactional
    public ApiMessage<UserResponse.AgreementResponse> createAgreement(UserRequest.agreement req, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (agreementRepository.existsByUserId(user.getId())) {
            return ApiMessage.fail(HttpStatus.CONFLICT.value(), "이미 약관 동의가 존재합니다.");
        }

        Agreement agreement = Agreement.createAgreement(user, req.getMarketing());
        agreementRepository.save(agreement);

        return ApiMessage.success(HttpStatus.CREATED.value(), "created", toAgreementResponse(agreement));
    }

    @Transactional
    public ApiMessage<UserResponse.AgreementResponse> updateMarketing(UserRequest.agreement request, UUID userId) {
        // 1) 유저 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 기존 동의 조회 (없으면 404)
        Agreement agreement = agreementRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("약관 동의가 존재하지 않습니다."));

        // 3) 변경 감지 (무변경이면 바로 200)
        boolean changed = agreement.updateMarketing(request.getMarketing());

        // 4) 응답
        return ApiMessage.success(
                HttpStatus.OK.value(),
                changed ? "updated" : "no-change",
                toAgreementResponse(agreement)
        );
    }


    private UserResponse.AgreementResponse toAgreementResponse(Agreement agreement) {
        String createdAtIso = agreement.getCreatedAt()
                .atStartOfDay()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return UserResponse.AgreementResponse.builder()
                .id(agreement.getUserId().toString())
                .marketing(agreement.getMarketing())
                .createdAt(createdAtIso)
                .build();
    }


    // GET: /api/user/marketing
    @Transactional(readOnly = true)
    public ApiMessage<UserResponse.AgreementResponse> getMarketing(UUID userId) {
        // 1) 유저 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 기존 동의 조회 (없으면 404)
        Agreement agreement = agreementRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("약관 동의가 존재하지 않습니다."));

//        String createdAtIso = agreement.getCreatedAt()
//                .atStartOfDay()
//                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        UserResponse.AgreementResponse body = UserResponse.AgreementResponse.builder()
                .id(agreement.getUserId().toString())
                .marketing(agreement.getMarketing())
                .createdAt(agreement.getCreatedAt().toString())
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "success", body);
    }


    @Transactional
    public ApiMessage<UserResponse.OnboardingResponse> onboarding(UserRequest.onboarding request, UUID userId) {

        // 1) 이미 온보딩 완료한 유저면 409
        if (onboardingRepository.existsByUser_Id(userId)) {
            return ApiMessage.fail(HttpStatus.CONFLICT.value(), "이미 온보딩을 완료한 사용자입니다.");
        }

        // 2) 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        // 3) 입력 정제 (중복/공백 제거)
        List<String> composers = sanitize(request.getComposer());   // Lombok @Getter 기준
        List<String> periods = sanitize(request.getPeriod());
        List<String> instruments = sanitize(request.getInstrument());

        // 4) 생성 저장 (unique 충돌은 DB에 맡기고 캐치)
        Onboarding ob = Onboarding.createOnboarding(user, composers, periods, instruments);

        try {
            ob = onboardingRepository.saveAndFlush(ob);
        } catch (DataIntegrityViolationException e) {
            // 동시요청 등으로 unique(userId) 충돌
            return ApiMessage.fail(HttpStatus.CONFLICT.value(), "이미 온보딩을 완료한 사용자입니다.");
        }

        // 5) 응답 바디
        UserResponse.OnboardingResponse body =
                UserResponse.OnboardingResponse.builder()
                        .id(ob.getOnboardingId().toString())
                        .userId(user.getId().toString())
                        .composer(ob.getComposers())
                        .period(ob.getPeriods())
                        .instrument(ob.getInstruments())
                        .build();


        return ApiMessage.success(HttpStatus.CREATED.value(), "온보딩 생성 완료", body);
    }

    public ApiMessage<UserResponse.OnboardingResponse> getOnboarding(UUID userId) {

        // 1) 온보딩 정보 조회
        Onboarding ob = onboardingRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NoSuchElementException("온보딩 정보를 찾을 수 없습니다."));

        // 2) User도 필요하다면 fetch
        User user = ob.getUser();

        // 3) 응답 DTO 매핑
        UserResponse.OnboardingResponse body = UserResponse.OnboardingResponse.builder()
                .id(ob.getOnboardingId().toString())
                .userId(user.getId().toString())
                .composer(ob.getComposers())
                .period(ob.getPeriods())
                .instrument(ob.getInstruments())
                .build();

        // 4) ApiMessage success 래핑
        return ApiMessage.success(HttpStatus.OK.value(), "온보딩 조회 성공", body);
    }

    // ----- helpers -----
    private static List<String> sanitize(List<String> src) {
        if (src == null) return List.of();
        return src.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiMessage<UserResponse.OnboardingResponse> patchOnboarding(UserRequest.onboardingPatch request, UUID userId) {

        Onboarding ob = onboardingRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NoSuchElementException("온보딩이 존재하지 않습니다. 먼저 POST로 생성하세요."));

        // null = no change, [] = clear, [values] = replace
        if (request.getComposer() != null) {
            ob.getComposers().clear();
            ob.getComposers().addAll(sanitize(request.getComposer()));
        }
        if (request.getPeriod() != null) {
            ob.getPeriods().clear();
            ob.getPeriods().addAll(sanitize(request.getPeriod()));
        }
        if (request.getInstrument() != null) {
            ob.getInstruments().clear();
            ob.getInstruments().addAll(sanitize(request.getInstrument()));
        }

        // JPA Dirty Checking 로 업데이트 반영
        onboardingRepository.flush();

        UserResponse.OnboardingResponse body =
                UserResponse.OnboardingResponse.builder()
                        .id(ob.getOnboardingId().toString())
                        .userId(userId.toString()) // 이미 UUID 있음
                        .composer(ob.getComposers())
                        .period(ob.getPeriods())
                        .instrument(ob.getInstruments())
                        .build();

        return ApiMessage.success(HttpStatus.OK.value(), "온보딩 수정 완료", body);
    }

    @Transactional
    public ApiMessage<UserResponse.ProfileResponse> profile(UserRequest.profile request, UUID userId) {

        if (request == null) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "업데이트할 필드가 없습니다.");
        }

        // 1) 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        // 2) 업데이트 적용 (화이트리스트 & sanitize)

        Map<String, String> updates = dtoMapperUtil.toMapWithoutNulls(request);
        boolean changed = false;

        for (Map.Entry<String, String> e : updates.entrySet()) { // TODO: 조금 더 가독성 좋게 변경
            String key = e.getKey();
            if (!PROFILE_UPDATABLE_FIELDS.contains(key)) {
                // 허용되지 않은 키는 무시
                continue;
            }
            String value = sanitize(e.getValue());
            if (value == null || value.isBlank()) {
                // 빈 문자열은 무시 (필요시 삭제 로직 별도 정의 가능)
                continue;
            }

            switch (key) {
                case "intro" -> {
                    if (!value.equals(user.getIntro())) {
                        user.setIntro(value);
                        changed = true;
                    }
                }
                case "nickName" -> {
                    if (!value.equals(user.getNickname())) {   // ← 수정
                        user.setNickname(value);               // ← 수정
                        changed = true;
                    }
                }
                case "profileImg" -> {
                    if (!value.equals(user.getProfileImageUrl())) { // ← 수정
                        user.setProfileImageUrl(value);             // ← 수정
                        changed = true;
                    }
                }
            }

        }

        if (changed) {
            userRepository.saveAndFlush(user);
        }

        // 3) 응답 DTO
        UserResponse.ProfileResponse body = UserResponse.ProfileResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .platform(user.getPlatform() != null ? user.getPlatform().name().toLowerCase(Locale.ROOT) : null)
                .nickName(user.getNickname())
                .profileImg(user.getProfileImageUrl())
                .intro(user.getIntro())
                .build();

        return ApiMessage.success(
                HttpStatus.OK.value(),
                "프로필이 수정되었습니다.",
                body
        );
    }

    @Transactional(readOnly = true)
    public ApiMessage<UserResponse.ProfileResponse> getProfile(UUID userId) {
        // 1) 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 사용자입니다."));

        // 2) 응답 바디 생성
        UserResponse.ProfileResponse body = UserResponse.ProfileResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .nickName(user.getNickname())
                .profileImg(user.getProfileImageUrl())
                .platform(user.getPlatform().name())
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "프로필 조회 성공", body);
    }

    private String sanitize(String s) {
        return s == null ? null : s.trim();
    }

    @Transactional
    public ApiMessage<UserResponse.followingResponse> following(UserRequest.following request, UUID userId) {

        // 0) 파라미터 검증
        if (request == null || request.getFollower() == null) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "대상 사용자 ID가 없습니다.");
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(request.getFollower());
        } catch (IllegalArgumentException e) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "잘못된 UUID 형식입니다: " + request.getFollower());
        }

        if (userId.equals(targetId)) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "자기 자신을 팔로우할 수 없습니다.");
        }

        // 1) 유저 로드
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetId));

        // 2) 기존 팔로우 관계 조회
        Follow rel = followRepository.findByFollower_IdAndFollowing_Id(userId, targetId).orElse(null);

        // 3) 블록 상태 조회
        boolean iBlockedHim = blockRepository.existsByBlocker_IdAndBlocked_Id(userId, targetId);   // 내가 상대를 차단
        boolean heBlockedMe = blockRepository.existsByBlocker_IdAndBlocked_Id(targetId, userId);   // 상대가 나를 차단
        boolean blocked = iBlockedHim || heBlockedMe;

        String msg;

        boolean isPublicAccount = target.isPublicAccount();

        // === 토글 로직 ===
        if (rel == null || rel.isUnfollowed()) {
            // 지금은 "팔로우/요청이 안 되어 있는 상태"

            // 👉 새로 시도할 때만 블록 체크
            if (blocked) {
                return ApiMessage.fail(
                        HttpStatus.FORBIDDEN.value(),
                        "차단된 사용자와는 팔로우할 수 없습니다."
                );
            }

            FollowStatus nextStatus = isPublicAccount ? FollowStatus.ACCEPTED : FollowStatus.REQUESTED;

            if (rel == null) {
                // 새 관계 생성
                rel = Follow.createFollow(me, target, nextStatus);
                followRepository.save(rel);
            } else {
                // 기존 row 재활성화
                rel.activate(LocalDateTime.now(), nextStatus);
            }

            msg = isPublicAccount ? "followed" : "requested";

        } else if (rel.getStatus() == FollowStatus.REQUESTED) {
            // 🔹 비공개 계정에 이미 팔로우 요청 보낸 상태 → 다시 누르면 "요청 취소"
            rel.deactivate();
            msg = "request_canceled";

        } else if (rel.getStatus() == FollowStatus.ACCEPTED) {
            // 🔹 현재 팔로잉 상태 → 언팔로우
            rel.deactivate();
            msg = "unfollowed";

        } else {
            // BLOCKED 등 예외케이스가 걸리면 필요 시 별도 처리
            return ApiMessage.fail(
                    HttpStatus.BAD_REQUEST.value(),
                    "지원하지 않는 팔로우 상태입니다."
            );
        }

        UserResponse.followingResponse body = UserResponse.followingResponse.builder()
                .userId(me.getId().toString())
                .followingId(target.getId().toString())
                .msg(msg)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), msg, body);
    }



    @Transactional(readOnly = true)
    public ApiMessage<UserResponse.followingCheckResponse> followingListByNickname(UUID userId, String nickname) {
        UUID targetId = userRepository.findIdByNickname(nickname)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."))
                .getId();

        boolean iFollow = followRepository
                .existsByFollower_IdAndFollowing_IdAndStatus(
                        userId,
                        targetId,
                        FollowStatus.ACCEPTED
                );

        UserResponse.followingCheckResponse body = UserResponse.followingCheckResponse.builder()
                .result(iFollow)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "팔로우 정보 조회 성공", body);
    }

    @Transactional(readOnly = true)
    public ApiMessage<UserResponse.followingCheckResponse> followingListByUserId(UUID userId, UUID targetId) {

        Optional<FollowStatus> statusOpt = followRepository.findStatus(userId, targetId);

        FollowStatus status = statusOpt.orElse(FollowStatus.UNFOLLOW);

        boolean iFollow = (status == FollowStatus.ACCEPTED);

        UserResponse.followingCheckResponse body = UserResponse.followingCheckResponse.builder()
                .result(iFollow)
                .status(status)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "팔로우 정보 조회 성공", body);
    }



    @Transactional(readOnly = true)
    public ApiMessage<UserResponse.MyPageResponse> getMyPage(UUID userId, UUID profileUserId) {

        log.debug("➡️ getMyPage 요청: loginUser={}, profileUser={}", userId, profileUserId);

        // 1) 타겟 유저 결정
        UUID targetUserId = (profileUserId == null || userId.equals(profileUserId))
                ? userId
                : profileUserId;

        log.debug("🎯 targetUserId 결정 완료: {}", targetUserId);

        // 1) 유저 조회
        User user = userRepository.findById(targetUserId).orElse(null);
        if (user == null) {
            log.warn("❌ 유저 조회 실패: targetUserId={}", targetUserId);
            return ApiMessage.fail(HttpStatus.NOT_FOUND.value(), "user_not_found");
        }

        log.debug("👤 유저 조회 성공: nickname={}, id={}", user.getNickname(), targetUserId);


        // 2) 팔로워/팔로잉
        long followers = followRepository.countByFollowing_IdAndStatus(targetUserId, FollowStatus.ACCEPTED);
        long followings = followRepository.countByFollower_IdAndStatus(targetUserId, FollowStatus.ACCEPTED);

        log.debug("📊 팔로워/팔로잉 조회: followers={}, followings={}", followers, followings);


        // 3) 하모니룸
        List<HarmonyRoom> rooms = harmonyRoomRepository.findAllJoinedOrOwned(targetUserId);
        log.debug("🏠 하모니룸 조회: roomCount={}", rooms.size());

        List<HarmonyRoomMembers> memberships = harmonyRoomRepository.findByUserIdWithRoom(targetUserId);
        log.debug("👥 하모니룸 멤버십 조회: membershipCount={}", memberships.size());


        Map<UUID, String> roleByRoomId = memberships.stream()
                .collect(Collectors.toMap(
                        m -> m.getHarmonyRoom().getId(),
                        HarmonyRoomMembers::getRole,
                        (r1, r2) -> rankRole(r1) >= rankRole(r2) ? r1 : r2
                ));

        log.debug("🔑 역할 매핑 완료: mappedRoles={}", roleByRoomId.size());


        List<UserResponse.HarmonyRoomItem> roomItems = rooms.stream()
                .map(r -> {
                    UUID roomId = r.getId();
                    boolean isOwner = r.getOwner() != null && targetUserId.equals(r.getOwner().getId());
                    String role = roleByRoomId.get(roomId);
                    boolean isManager = isOwner
                            || "OWNER".equalsIgnoreCase(role)
                            || "ADMIN".equalsIgnoreCase(role);

                    boolean bookmarked = harmonyRoomBookmarkRepository
                            .existsByHarmonyRoom_IdAndUser_Id(roomId, targetUserId);

                    return UserResponse.HarmonyRoomItem.builder()
                            .roomId(roomId)
                            .roomName(r.getName())
                            .roomImg(r.getProfileImageUrl())
                            .bookmark(bookmarked)
                            .isManager(isManager)
                            .build();
                })
                .toList();

        log.debug("🏷️ 하모니룸 DTO 생성 완료: size={}", roomItems.size());


        // 4) 프로필 음악
        UserResponse.ProfileMusic profileMusic = userProfileMusicService.getActive(targetUserId)
                .map(m -> UserResponse.ProfileMusic.builder()
                        .youtube(m.getUrl())
                        .title(m.getTitle())
                        .build())
                .orElse(null);

        log.debug("🎵 프로필 음악 조회: exists={}", profileMusic != null);


        // 5) 사용자 게시글
        FilterPostResponse.FeedList posts;
        try {
            log.debug("📝 사용자 게시글 조회 시작...");
            List<Post> items = postRepository.findByUserIdOrderByCreatedAtDesc2(targetUserId, userId);
            log.debug("📝 사용자 게시글 조회: count={}", items.size());

            posts = buildFeedUtil.buildFeedList(items, userId);
            log.debug("📝 사용자 게시글 DTO 변환 완료");
        } catch (Exception e) {
            log.error("❌ 사용자 게시글 조회 실패", e);
            if (e instanceof IllegalArgumentException) {
                return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다.");
            }
            return ApiMessage.fail(500, "사용자 게시글 런타임 초과: " + e.getMessage());
        }


        // 5-1) 사용자 '미디어 포함' 게시글
        FilterPostResponse.FeedList mediaFeed;
        try {
            log.debug("🎬 미디어 게시글 조회 시작...");
            List<Post> mediaItems =
                    postRepository.findMediaPostsByUserIdOrderByCreatedAtDesc(targetUserId, userId);

            log.debug("🎬 미디어 게시글 조회: count={}", mediaItems.size());

            mediaFeed = mediaItems.isEmpty()
                    ? FilterPostResponse.FeedList.builder().results(List.of()).build()
                    : buildFeedUtil.buildFeedList(mediaItems, userId);

            log.debug("🎬 미디어 게시글 DTO 변환 완료");

        } catch (Exception e) {
            log.error("❌ 미디어 게시글 조회 실패", e);
            if (e instanceof IllegalArgumentException) {
                return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다: " + e.getMessage());
            }
            return ApiMessage.fail(500, "미디어 게시글 조회 중 서버 오류가 발생했습니다.");
        }


        // 5-2) 북마크 게시글
        FilterPostResponse.FeedList bookmarks;
        try {
            log.debug("🔖 북마크 게시글 조회 시작...");
            List<Post> bookmarkItems =
                    postRepository.findBookmarkedPostsByUserIdExcludingProfileHidden(targetUserId, userId);

            log.debug("🔖 북마크 게시글 조회: count={}", bookmarkItems.size());

            bookmarks = bookmarkItems.isEmpty()
                    ? FilterPostResponse.FeedList.builder().results(List.of()).build()
                    : buildFeedUtil.buildFeedList(bookmarkItems, userId);

            log.debug("🔖 북마크 게시글 DTO 변환 완료");

        } catch (Exception e) {
            log.error("❌ 북마크 게시글 조회 실패", e);
            if (e instanceof IllegalArgumentException) {
                return ApiMessage.fail(400, "잘못된 사용자 ID 형식입니다: " + e.getMessage());
            }
            return ApiMessage.fail(500, "북마크 게시글 조회 중 서버 오류가 발생했습니다.");
        }


        // 6) 응답 DTO
        log.debug("📦 최종 응답 DTO 생성 완료");

        UserResponse.MyPageResponse body = UserResponse.MyPageResponse.builder()
                .profileImg(user.getProfileImageUrl())
                .nickname(user.getNickname())
                .introduction(user.getIntro())
                .profileMusic(profileMusic)
                .followers(followers)
                .followings(followings)
                .harmonyRooms(roomItems)
                .posts(posts)
                .mediaPosts(mediaFeed)
                .bookmarks(bookmarks)
                .build();

        log.debug("✅ getMyPage 처리 성공: targetUserId={}", targetUserId);

        return ApiMessage.success(200, "response successful", body);
    }

    @Transactional
    public ResignationResponseDTO resignation(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("user_not_found"));

        // 이미 탈퇴한 유저라면 기존 값 그대로 돌려줌
        if (user.getDeleteAt() != null) {
            log.info("이미 탈퇴 처리된 유저 요청 - userId={}, deleteAt={}",
                    userId, user.getDeleteAt());

            return ResignationResponseDTO.builder()
                    .deleteAt(user.getDeleteAt())
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        user.resign(now);

        log.info("유저 탈퇴 처리 완료 - userId={}, deleteAt={}", userId, now);

        return ResignationResponseDTO.builder()
                .deleteAt(now)
                .build();
    }



    private int rankRole(String role) {
        if (role == null) return 0;
        return switch (role.toUpperCase()) {
            case "OWNER" -> 3;
            case "ADMIN" -> 2;
            default -> 1; // MEMBER 등
        };
    }

    @Transactional(readOnly = true)
    public ApiMessage<UserResponse.NicknameExistResponse> isNicknameExist(String nickname) {

        // 필요하면 간단 검증
        if (nickname == null || nickname.isBlank()) {
            return ApiMessage.<UserResponse.NicknameExistResponse>builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("닉네임은 비어 있을 수 없습니다.")
                    .data(null)
                    .build();
        }

        boolean exists = userRepository.existsByNickname(nickname.trim());

        UserResponse.NicknameExistResponse body =
                new UserResponse.NicknameExistResponse(exists);

        return ApiMessage.<UserResponse.NicknameExistResponse>builder()
                .code(HttpStatus.OK.value())
                .message("닉네임 중복 여부 조회 성공")
                .data(body)
                .build();
    }

}