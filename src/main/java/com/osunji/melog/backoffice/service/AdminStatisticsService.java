package com.osunji.melog.backoffice.service;

import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.enums.Platform;
import com.osunji.melog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSocialLoginStatistics() {
        List<User> allUsers = userRepository.findAll();

        Map<Platform, Long> platformCounts = allUsers.stream()
                .filter(u -> u.getDeleteAt() == null)
                .collect(Collectors.groupingBy(User::getPlatform, Collectors.counting()));

        long total = platformCounts.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);

        List<Map<String, Object>> platforms = List.of(
                createPlatformStat("KAKAO", platformCounts.getOrDefault(Platform.KAKAO, 0L), total),
                createPlatformStat("GOOGLE", platformCounts.getOrDefault(Platform.GOOGLE, 0L), total),
                createPlatformStat("NAVER", platformCounts.getOrDefault(Platform.NAVER, 0L), total)
        );
        result.put("platforms", platforms);
        return result;
    }

    private Map<String, Object> createPlatformStat(String name, long count, long total) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("platform", name);
        stat.put("count", count);
        stat.put("percentage", total > 0 ? Math.round((double) count / total * 1000.0) / 10.0 : 0.0);
        return stat;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getActiveTimeStatistics() {
        // Active time statistics - DB-based design (placeholder)
        // In a real implementation, this would query user activity logs by hour
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> timeSlots = new java.util.ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("hour", hour);
            slot.put("count", 0);
            timeSlots.add(slot);
        }
        result.put("timeSlots", timeSlots);
        result.put("totalSessions", 0);
        return result;
    }
}
