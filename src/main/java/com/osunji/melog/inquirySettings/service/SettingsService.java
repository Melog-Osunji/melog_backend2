package com.osunji.melog.inquirySettings.service;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.inquirySettings.dto.request.SettingsRequest;
import com.osunji.melog.inquirySettings.dto.response.SettingsResponse;
import com.osunji.melog.user.domain.Block;
import com.osunji.melog.user.domain.Follow;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.FollowStatus;
import com.osunji.melog.user.repository.BlockRepository;
import com.osunji.melog.user.repository.FollowRepository;
import com.osunji.melog.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.osunji.melog.user.domain.enums.FollowStatus.*;

@Service
public class SettingsService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;

    public SettingsService(UserRepository userRepository, FollowRepository followRepository, BlockRepository blockRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public ApiMessage<SettingsResponse.infoSettingsResponse> getInfoSettings(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // protect가 null이면 false로 기본 처리
        boolean active = Boolean.TRUE.equals(user.getActive());

        // 언어 저장소가 아직 없으므로 기본값 kor 사용 (후에 교체)
        String language = resolveLanguageFor(user);

        SettingsResponse.infoSettingsResponse body = SettingsResponse.infoSettingsResponse.builder()
                .userId(user.getId())
                .platform(user.getPlatform())
                .email(user.getEmail())
                .isActive(active)
                .language(language)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "success", body);
    }

    private String resolveLanguageFor(User user) {
        // TODO: 향후 UserSettings(언어) 테이블 연동
        return "kor";
    }


    @Transactional(readOnly = true)
    public ApiMessage<List<SettingsResponse.FollowResponse>> getFollow(UUID userId) {
        var applicants = followRepository.findApplicantsByFollowingIdAndStatus(
                userId, FollowStatus.REQUESTED
        );

        var body = applicants.stream()
                .map(SettingsResponse.FollowResponse::from)
                .toList();

        return ApiMessage.success(HttpStatus.OK.value(), "success", body);
    }

    @Transactional(readOnly = true)
    public ApiMessage<List<SettingsResponse.FollowResponse>> getBlock(UUID userId) {
        List<Block> blocks = blockRepository.findAllByBlockerIdFetchBlocked(userId);

        List<SettingsResponse.FollowResponse> body = blocks.stream()
                .map(b -> SettingsResponse.FollowResponse.from(b.getBlocked())) // User -> DTO
                .toList();

        return ApiMessage.success(HttpStatus.OK.value(), "success", body);
    }


    @Transactional
    public ApiMessage<SettingsResponse.CheckResponse> postBlockUser(UUID userId, SettingsRequest request) {

        if (request == null || request.getAcceptUserId() == null) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "대상 사용자 ID가 없습니다.");
        }

        UUID targetId = request.getAcceptUserId();

        if (userId.equals(targetId)) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "자기 자신을 차단할 수 없습니다.");
        }

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetId));

        // 1) Block row 생성 (이미 있으면 건너뜀: idempotent)
        boolean alreadyBlocked = blockRepository.existsByBlocker_IdAndBlocked_Id(userId, targetId);
        if (!alreadyBlocked) {
            Block block = Block.create(me, target, LocalDateTime.now());
            blockRepository.save(block);
        }

        // 2) 내가 → 그 사람 : BLOCKED
        followRepository.findByFollower_IdAndFollowing_Id(userId, targetId)
                .ifPresent(Follow::blockByMe);

        // 3) 그 사람이 → 나 : UNFOLLOW
        followRepository.findByFollower_IdAndFollowing_Id(targetId, userId)
                .ifPresent(Follow::deactivate);

        SettingsResponse.CheckResponse body = SettingsResponse.CheckResponse.builder()
                .userId(targetId)
                .status(BLOCKED)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "blocked", body);
    }


    @Transactional
    public ApiMessage<SettingsResponse.CheckResponse> postAcceptUser(UUID userId, SettingsRequest request) {
        // 요청 보낸 사람(= follower) 식별
        UUID followerId = request.getAcceptUserId(); // 필드명이 다르면 여기를 맞춰주세요.

        // 유저 존재 여부(선택: 없으면 404)
        userRepository.findById(followerId)
                .orElseThrow(() -> new EntityNotFoundException("Follower not found: " + followerId));
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // (follower -> following=나) 로우가 REQUESTED 상태인지 조회
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Follow request not found."));

        // 상태별 처리 (멱등성 보장)
        switch (follow.getStatus()) {
            case REQUESTED -> {
                follow.activate(LocalDateTime.now(), ACCEPTED); // ACCEPTED + followedAt 갱신
                // JPA 영속 상태라 save() 불필요하지만 명시적으로 호출해도 무방
                // followRepository.save(follow);
            }
            case ACCEPTED -> {
                // 이미 수락된 상태면 그대로 통과(멱등)
            }
            case BLOCKED, UNFOLLOW -> {
                // 비정상 상태에서 수락 불가
                throw new IllegalStateException("Cannot accept follow in status: " + follow.getStatus());
            }
        }

        // 응답: 수락된 상대(= follower) ID를 돌려줌
        SettingsResponse.CheckResponse body = SettingsResponse.CheckResponse.builder()
                .userId(followerId)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "success", body);
    }

    @Transactional
    public ApiMessage<SettingsResponse.CheckResponse> postUnblockUser(UUID userId, SettingsRequest request) {

        if (request == null || request.getAcceptUserId() == null) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "대상 사용자 ID가 없습니다.");
        }

        UUID targetId = request.getAcceptUserId();

        if (userId.equals(targetId)) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "자기 자신을 차단 해제할 수 없습니다.");
        }

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetId));

        // 1) Block row 조회
        var blockOpt = blockRepository.findByBlocker_IdAndBlocked_Id(userId, targetId);

        if (blockOpt.isPresent()) {
            // 실제로 차단된 상태라면 Block 삭제
            blockRepository.delete(blockOpt.get());

            // 2) 나 → 그 사람 방향 Follow가 BLOCKED였다면 → UNFOLLOW로 되돌림
            followRepository.findByFollower_IdAndFollowing_Id(userId, targetId)
                    .ifPresent(Follow::unblockByMe);
        }

        // 현재 상태 재확인(idempotent)
        boolean blockedNow = blockRepository.existsByBlocker_IdAndBlocked_Id(userId, targetId);

        SettingsResponse.CheckResponse body = SettingsResponse.CheckResponse.builder()
                .userId(targetId)
                .status(blockedNow ? BLOCKED : UNFOLLOW)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), "unblocked", body);
    }

    @Transactional
    public ApiMessage<SettingsResponse.ActivateResponse> postActivateUser(UUID userId) {

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + userId));

        boolean wasPublic = target.isPublicAccount();

        if (wasPublic) {
            target.makePrivate();   // 공개 -> 비공개
        } else {
            target.makePublic();    // 비공개 -> 공개
        }

        boolean isPublicNow = target.isPublicAccount();

        String msg = isPublicNow ? "Public" : "Private";

        SettingsResponse.ActivateResponse body = SettingsResponse.ActivateResponse.builder()
                .userId(target.getId())
                .status(isPublicNow)
                .build();

        return ApiMessage.success(HttpStatus.OK.value(), msg, body);
    }


}
