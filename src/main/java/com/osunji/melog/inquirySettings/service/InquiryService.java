package com.osunji.melog.inquirySettings.service;


import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.inquirySettings.domain.Inquiry;
import com.osunji.melog.inquirySettings.domain.InquiryChildType;
import com.osunji.melog.inquirySettings.domain.InquiryParentType;
import com.osunji.melog.inquirySettings.dto.InquiryRequest;
import com.osunji.melog.inquirySettings.dto.InquiryResponse;
import com.osunji.melog.inquirySettings.repository.InquiryRepository;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

    @Service
    @RequiredArgsConstructor
    public class InquiryService {

        private final InquiryRepository inquiryRepository;
        private final UserRepository userRepository;

        @Transactional
        public ApiMessage<InquiryResponse> createAgreement(InquiryRequest request, UUID userId) {
            // 1) 필수 파라미터 기본 가드 (Bean Validation으로도 걸리지만 한 번 더 방어)
            if (request == null) {
                return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "요청 본문이 없습니다.");
            }

            // 2) 문자열 -> Enum 파싱(대소문자 허용)
            InquiryParentType parentType;
            InquiryChildType childType;
            try {
                parentType = InquiryParentType.valueOf(request.getParentType().trim().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "parentType 값이 올바르지 않습니다.");
            }
            try {
                childType = InquiryChildType.valueOf(request.getChildType().trim().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), "childType 값이 올바르지 않습니다.");
            }

            // 3) User 프록시 로딩 (존재하지 않으면 영속화 시점에 예외)
            User userRef = userRepository.getReferenceById(userId);

            // 4) 도메인 생성 (내부 ensureValidPair에서 부모-자식 유효성 재검증)
            Inquiry entity;
            try {
                entity = Inquiry.create(
                        parentType,
                        childType,
                        request.getTitle().trim(),
                        request.getContent().trim(),
                        userRef
                );
            } catch (IllegalArgumentException ex) {
                return ApiMessage.fail(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
            }

            // 5) 저장
            Inquiry saved = inquiryRepository.save(entity);

            // 6) 응답 DTO 구성
            InquiryResponse body = InquiryResponse.builder()
                    .id(saved.getId())
                    .userId(userId)
                    .createdAt(saved.getCreatedAt())
                    .build();

            return ApiMessage.success(HttpStatus.CREATED.value(), "정상적으로 저장됨", body);
        }
    }
