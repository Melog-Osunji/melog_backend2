package com.osunji.melog.user.service;

import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.user.domain.Agreement;
import com.osunji.melog.user.domain.Onboarding;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.dto.request.UserRequest;
import com.osunji.melog.user.dto.response.UserResponse;
import com.osunji.melog.user.repository.AgreementRepository;
import com.osunji.melog.user.repository.OnboardingRepository;
import com.osunji.melog.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AgreementRepository agreementRepository;
    private final OnboardingRepository onboardingRepository;

    public UserService(UserRepository userRepository, AgreementRepository agreementRepository, OnboardingRepository onboardingRepository) {
        this.userRepository = userRepository;
        this.agreementRepository = agreementRepository;
        this.onboardingRepository = onboardingRepository;
    }

    private static final Set<String> PROFILE_UPDATABLE_FIELDS = Set.of(
            "intro", "nickName", "profileImg"
    );

    @Transactional
    public ApiMessage<UserResponse.AgreementResponse> agreement(UserRequest.agreement request, UUID userId) {
        // 1) 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2) 약관 레코드 upsert
        Agreement agreement = agreementRepository.findById(userId)
                .map(a -> {                      // 기존 있으면 marketing만 갱신
                    a.updateMarketing(request.isMarketing());
                    return a;
                })
                .orElseGet(() ->
                        Agreement.createAgreement(user, request.isMarketing())
                );

        agreementRepository.save(agreement);

        // 3) createdAt을 ISO-8601 문자열로 변환
        // 현재 엔티티가 LocalDate이므로 00:00:00으로 맞춰 ISO-8601 출력
        String createdAtIso = agreement.getCreatedAt()
                .atStartOfDay()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // 4) 응답 DTO 구성
        UserResponse.AgreementResponse body = UserResponse.AgreementResponse.builder()
                .id(agreement.getUserId().toString())
                .marketing(agreement.getMarketing())
                .createdAt(createdAtIso)
                .build();

        return ApiMessage.success(200, "success", body);
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
        List<String> composers   = sanitize(request.getComposer());   // Lombok @Getter 기준
        List<String> periods     = sanitize(request.getPeriod());
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
    public ApiMessage<UserResponse.ProfileResponse> profile(UserRequest.profilePatch request, UUID userId) {

        if (request == null || request.getUpdates() == null || request.getUpdates().isEmpty()) {
            return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "업데이트할 필드가 없습니다.");
        }

        // 1) 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        // 2) 업데이트 적용 (화이트리스트 & sanitize)
        Map<String, String> updates = request.getUpdates();
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

    private String sanitize(String s) {
        return s == null ? null : s.trim();
    }

}