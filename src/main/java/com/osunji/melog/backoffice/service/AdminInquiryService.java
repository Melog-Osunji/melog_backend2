package com.osunji.melog.backoffice.service;

import com.osunji.melog.inquirySettings.domain.Inquiry;
import com.osunji.melog.inquirySettings.domain.InquiryParentType;
import com.osunji.melog.inquirySettings.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getInquiries(String type, String query,
                                             String fromDate, String toDate,
                                             Integer year, Integer month,
                                             int page, int size) {
        // Validate: fromDate/toDate and year/month cannot be used simultaneously
        if (fromDate != null && year != null) {
            throw new IllegalArgumentException("fromDate/toDate와 year/month는 동시에 사용할 수 없습니다.");
        }

        InquiryParentType parentType = null;
        if (type != null && !type.isBlank()) {
            switch (type) {
                case "SERVICE" -> parentType = InquiryParentType.ACCOUNT;
                case "FEATURE" -> parentType = InquiryParentType.SUGGESTION;
                case "ETC" -> parentType = InquiryParentType.OTHER;
            }
        }

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        if (fromDate != null && toDate != null && !fromDate.isBlank() && !toDate.isBlank()) {
            fromDateTime = LocalDate.parse(fromDate).atStartOfDay();
            toDateTime = LocalDate.parse(toDate).atTime(23, 59, 59);
        } else if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            fromDateTime = ym.atDay(1).atStartOfDay();
            toDateTime = ym.atEndOfMonth().atTime(23, 59, 59);
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> inquiryPage = inquiryRepository.findByAdminFilter(
                parentType, query, fromDateTime, toDateTime, pageable);

        List<Map<String, Object>> content = inquiryPage.getContent().stream()
                .map(inquiry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("inquiryId", inquiry.getId().toString());
                    item.put("name", inquiry.getUser().getNickname());
                    item.put("email", inquiry.getUser().getEmail());
                    item.put("type", mapInquiryType(inquiry.getParentType()));
                    item.put("title", inquiry.getTitle());
                    item.put("status", inquiry.getAnswerStatus() == Inquiry.AnswerStatus.ANSWERED
                            ? "ANSWERED" : "PENDING");
                    item.put("createdAt", inquiry.getCreatedAt().toString());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", inquiryPage.getNumber());
        result.put("size", inquiryPage.getSize());
        result.put("totalElements", inquiryPage.getTotalElements());
        result.put("totalPages", inquiryPage.getTotalPages());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInquiryDetail(UUID inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의를 찾을 수 없습니다."));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inquiryId", inquiry.getId().toString());
        result.put("name", inquiry.getUser().getNickname());
        result.put("email", inquiry.getUser().getEmail());
        result.put("type", mapInquiryType(inquiry.getParentType()));
        result.put("title", inquiry.getTitle());
        result.put("content", inquiry.getContent());
        result.put("status", inquiry.getAnswerStatus() == Inquiry.AnswerStatus.ANSWERED
                ? "ANSWERED" : inquiry.getAnswerStatus().name());
        result.put("createdAt", inquiry.getCreatedAt().toString());
        result.put("answer", inquiry.getAnswer());
        result.put("answerStatus", inquiry.getAnswerStatus().name());
        result.put("answeredAt", inquiry.getAnsweredAt() != null ? inquiry.getAnsweredAt().toString() : null);
        return result;
    }

    @Transactional
    public void saveDraft(UUID inquiryId, String answer) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의를 찾을 수 없습니다."));
        inquiry.saveDraft(answer);
    }

    @Transactional
    public void publishAnswer(UUID inquiryId, String answer) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의를 찾을 수 없습니다."));
        inquiry.publishAnswer(answer);
    }

    private String mapInquiryType(InquiryParentType parentType) {
        return switch (parentType) {
            case ACCOUNT -> "SERVICE";
            case SUGGESTION -> "FEATURE";
            case OTHER -> "ETC";
        };
    }
}
