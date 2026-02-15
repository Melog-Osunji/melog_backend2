package com.osunji.melog.backoffice.service;

import com.osunji.melog.backoffice.dto.AdminNotificationCreateRequest;
import com.osunji.melog.backoffice.dto.AdminPageResponse;
import com.osunji.melog.backoffice.entity.AdminNotification;
import com.osunji.melog.backoffice.repository.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<AdminNotification> notificationPage = adminNotificationRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<Map<String, Object>> content = notificationPage.getContent().stream()
                .map(n -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", n.getId().toString());
                    item.put("title", n.getTitle());
                    item.put("content", n.getContent());
                    item.put("targetType", n.getTargetType());
                    item.put("createdAt", n.getCreatedAt().toString());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", AdminPageResponse.fromZeroBased(notificationPage));
        return result;
    }

    @Transactional
    public Map<String, Object> createNotification(AdminNotificationCreateRequest request) {
        AdminNotification notification = AdminNotification.create(
                request.getTitle(),
                request.getContent(),
                request.getTargetType()
        );
        AdminNotification saved = adminNotificationRepository.save(notification);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId().toString());
        result.put("title", saved.getTitle());
        result.put("content", saved.getContent());
        result.put("targetType", saved.getTargetType());
        result.put("createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null);
        return result;
    }
}
