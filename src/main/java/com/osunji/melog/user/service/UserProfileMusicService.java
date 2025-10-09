package com.osunji.melog.user.service;


import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.domain.UserProfileMusic;
import com.osunji.melog.user.repository.UserProfileMusicRepository;
import com.osunji.melog.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserProfileMusicService {

    private final UserRepository userRepository;
    private final UserProfileMusicRepository userProfileMusicRepository;

    private static final Pattern YT_ID_PATTERN = Pattern.compile(
            "(?:v=|/videos/|embed/|youtu\\.be/|/v/)([A-Za-z0-9_-]{6,})"
    );

    /** 대표곡 조회 */
    @Transactional(readOnly = true)
    public Optional<UserProfileMusic> getActive(UUID userId) {
        return userProfileMusicRepository.findTopByUser_IdAndIsActiveTrueOrderBySelectedAtDesc(userId);
    }

    /** 대표곡 변경(기존 활성 비활성화 → 신규 활성 저장) */
    @Transactional
    public void setActive(UUID userId, String youtubeUrl, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user_not_found"));

        String safeUrl = (youtubeUrl != null && !youtubeUrl.isBlank()) ? youtubeUrl.trim() : null;
        String safeTitle = (title != null && !title.isBlank()) ? title.trim() : null;

        // 둘 다 비어있으면 대표곡 해제(모두 비활성화만)
        if (safeUrl == null && safeTitle == null) {
            userProfileMusicRepository.deactivateAllActive(userId);
            return;
        }

        String videoId = extractVideoId(safeUrl);
        userProfileMusicRepository.deactivateAllActive(userId);
        userProfileMusicRepository.save(UserProfileMusic.select(user, videoId, safeUrl, safeTitle, null));
    }

    private String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m = YT_ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
