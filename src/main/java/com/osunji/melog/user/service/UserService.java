package com.osunji.melog.user.service;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.global.util.DtoMapperUtil;
import com.osunji.melog.harmony.entity.HarmonyRoom;
import com.osunji.melog.harmony.repository.HarmonyRoomBookmarkRepository;
import com.osunji.melog.harmony.repository.HarmonyRoomRepository;
import com.osunji.melog.review.dto.response.BookmarkResponse;
import com.osunji.melog.review.dto.response.FilterPostResponse;
import com.osunji.melog.review.service.BookmarkService;
import com.osunji.melog.review.service.PostService;
import com.osunji.melog.user.domain.Agreement;
import com.osunji.melog.user.domain.Follow;
import com.osunji.melog.user.domain.Onboarding;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.FollowStatus;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.repository.AgreementRepository;
import com.osunji.melog.user.repository.FollowRepository;
import com.osunji.melog.user.repository.OnboardingRepository;
import com.osunji.melog.user.repository.UserRepository;
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

    public UserService(UserRepository userRepository, AgreementRepository agreementRepository, OnboardingRepository onboardingRepository, FollowRepository followRepository, HarmonyRoomRepository harmonyRoomRepository, HarmonyRoomBookmarkRepository harmonyRoomBookmarkRepository, PostService postService, UserProfileMusicService userProfileMusicService, BookmarkService bookmarkService, DtoMapperUtil dtoMapperUtil) {
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
    }

    private static final Set<String> PROFILE_UPDATABLE_FIELDS = Set.of(
            "intro", "nickName", "profileImg"
    );

    @Transactional
    public ApiMessage<UserResponse.AgreementResponse> createAgreement(UserRequest.agreement request, UUID userId) {
        // 1) 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 이미 동의가 있으면 409
        if (agreementRepository.existsById(userId)) {
            return ApiMessage.fail(HttpStatus.CONFLICT.value(), "이미 약관 동의가 존재합니다.");
        }

        // 3) 생성
        Agreement agreement = Agreement.createAgreement(user, request.isMarketing());
        agreementRepository.save(agreement);

        // 4) 응답
        return ApiMessage.success(
                HttpStatus.CREATED.value(),
                "created",
                toAgreementResponse(agreement)
        );
    }

    @Transactional
    public ApiMessage<UserResponse.AgreementResponse> updateMarketing(UserRequest.agreement request, UUID userId) {
        // 1) 기존 동의 없으면 404
        Agreement agreement = agreementRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("약관 동의가 존재하지 않습니다."));

        // 2) 마케팅 동의만 갱신
        agreement.updateMarketing(request.isMarketing());
        // JPA dirty checking으로 flush

        // 3) 응답
        return ApiMessage.success(
                HttpStatus.OK.value(),
                "updated",
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

    @Transactional(readOnly = true)
    public ApiMessage<UserResponse.AgreementResponse> getMarketing(UUID userId) {
        Agreement agreement = agreementRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("약관 동의가 존재하지 않습니다."));

        String createdAtIso = agreement.getCreatedAt()
                .atStartOfDay()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        UserResponse.AgreementResponse body = UserResponse.AgreementResponse.builder()
                .id(agreement.getUserId().toString())
                .marketing(agreement.getMarketing())
                .createdAt(createdAtIso)
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

        for (Map.Entry<String, String> e : updates.entrySet()) {
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
                .platform(user.getPlatform().name())
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "프로필 조회 성공", body);
    }

    private String sanitize(String s) {
        return s == null ? null : s.trim();
    }


    @Transactional
    public ApiMessage<UserResponse.followingResponse> following(UserRequest.following request, UUID userId) {

        // 0) 파라미터 해석 및 기본 검증
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

        // 1) 주체/대상 유저 로드
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetId));

        // 2) 기존 팔로우 관계 조회
        Follow rel = followRepository.findByFollower_IdAndFollowing_Id(userId, targetId).orElse(null);

        String msg;
        if (rel == null) {
            // 없으면 새로 팔로우
            rel = Follow.createFollow(me, target);
            followRepository.save(rel);
        } else {
            // 기존 기록은 있는데 비활성 상태면 다시 팔로우
            rel.activate(LocalDateTime.now());
        }
        msg = "followed";

        UserResponse.followingResponse body = UserResponse.followingResponse.builder()
                .userId(me.getId().toString())
                .followingId(target.getId().toString())
                .msg(msg)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), msg, body);
    }

    @Transactional
    public ApiMessage<UserResponse.followingCheckResponse> followingListByNickname(UUID userId, String nickname) {
        UUID targetId = userRepository.findIdByNickname(nickname)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다.")).getId();

        boolean iFollow   = followRepository.existsByFollower_IdAndFollowing_Id(userId, targetId);

        UserResponse.followingCheckResponse body = UserResponse.followingCheckResponse.builder()
                .result(iFollow)
                .build();
        return ApiMessage.success(HttpStatus.OK.value(), "팔로우 정보 조회 성공", body);
    }

    public ApiMessage<UserResponse.MyPageResponse> getMyPage(UUID userId) {
        // 1) 유저 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiMessage.fail(HttpStatus.NOT_FOUND.value(), "user_not_found");
        }

        // 2) 팔로워/팔로잉 카운트 (ACCEPTED만 집계)
        long followers = followRepository.countByFollowing_IdAndStatus(userId, FollowStatus.ACCEPTED);
        long followings = followRepository.countByFollower_IdAndStatus(userId, FollowStatus.ACCEPTED);

        // 3) 하모니룸: 내가 소유한 방(=매니저) 기준 + 북마크 여부
        List<HarmonyRoom> ownedRooms = harmonyRoomRepository.findByOwner_Id(userId);
        List<UserResponse.HarmonyRoomItem> roomItems = ownedRooms.stream()
                .map(r -> UserResponse.HarmonyRoomItem.builder()
                        .roomId(r.getId())
                        .roomName(r.getName())
                        .isManager(true)
                        .roomImg(r.getProfileImageUrl())
                        .bookmark(harmonyRoomBookmarkRepository.existsByHarmonyRoom_IdAndUser_Id(r.getId(), userId))
                        .build())
                .toList();

        // 프로필 음악
        UserResponse.ProfileMusic profileMusic = userProfileMusicService.getActive(userId)
                .map(m -> UserResponse.ProfileMusic.builder()
                        .youtube(m.getUrl())
                        .title(m.getTitle())
                        .build())
                .orElse(null);

        // 5) 사용자 게시글 (전체)
        List<FilterPostResponse.UserPostData> posts = Collections.emptyList();
        try {
            var postsMsg = postService.getUserPosts(userId.toString());
            if (postsMsg == null) {
                log.warn("getUserPosts: ApiMessage가 null");
            } else if (postsMsg.isSuccess() && postsMsg.getData() != null) {
                posts = Optional.ofNullable(postsMsg.getData().getResults()).orElse(List.of());
            } else {
                log.warn("getUserPosts 실패: code={}, message={}", postsMsg.getCode(), postsMsg.getMessage());
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) log.warn("getUserPosts 예외: {}", e, e);
        }

        // 5-1) 사용자 '미디어 포함' 게시글 (분리)
        List<FilterPostResponse.UserPostData> mediaPosts = Collections.emptyList();
        try {
            var mediaMsg = postService.getUserMediaPosts(userId.toString(), userId.toString());
            if (mediaMsg == null) {
                log.warn("getUserMediaPosts: ApiMessage가 null");
            } else if (mediaMsg.isSuccess() && mediaMsg.getData() != null) {
                mediaPosts = Optional.ofNullable(mediaMsg.getData().getResults()).orElse(List.of());
            } else {
                log.warn("getUserMediaPosts 실패: code={}, message={}", mediaMsg.getCode(), mediaMsg.getMessage());
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) log.warn("getUserMediaPosts 예외: {}", e, e);
        }

        // 5-2) 사용자 북마크 목록
        List<BookmarkResponse.BookmarkData> bookmarks = Collections.emptyList();
        try {
            var bmMsg = bookmarkService.getBookmarksByUser(userId.toString()); // ← API 23번 서비스 호출
            if (bmMsg == null) {
                log.warn("getBookmarksByUser: ApiMessage가 null");
            } else if (bmMsg.isSuccess() && bmMsg.getData() != null) {
                bookmarks = Optional.ofNullable(bmMsg.getData().getResults()).orElse(List.of());
            } else {
                log.warn("getBookmarksByUser 실패: code={}, message={}", bmMsg.getCode(), bmMsg.getMessage());
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) log.warn("getBookmarksByUser 예외: {}", e, e);
        }

        // 6) 응답 DTO
        UserResponse.MyPageResponse body = UserResponse.MyPageResponse.builder()
                .profileImg(user.getProfileImageUrl())
                .nickname(user.getNickname())
                .introduction(user.getIntro())
                .profileMusic(profileMusic)
                .followers(followers)
                .followings(followings)
                .harmonyRooms(roomItems)
                .posts(posts)
                .mediaPosts(mediaPosts)
                .bookmarks(bookmarks)
                .build();

        return ApiMessage.success(200, "response successful", body);
    }
}