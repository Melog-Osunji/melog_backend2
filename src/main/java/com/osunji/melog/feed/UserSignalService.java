package com.osunji.melog.feed;

import com.osunji.melog.user.repository.FollowRepository;
import com.osunji.melog.user.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;


@Service
@RequiredArgsConstructor
public class UserSignalService {

    private final OnboardingRepository onboardingRepository;
    private final FollowRepository followRepository;
    private final SearchLogReader searchLogReader;

    public UserSignals build(String userId) {
        // 1) 온보딩 → 태그 후보
        var obTags = new ArrayList<String>();
        onboardingRepository.findByUser_Id(userId).ifPresent(ob -> {
            if (ob.getComposers()   != null) obTags.addAll(ob.getComposers());
            if (ob.getPeriods()     != null) obTags.addAll(ob.getPeriods());
            if (ob.getInstruments() != null) obTags.addAll(ob.getInstruments());
        });

        // 2) 검색 로그(최근 30일): query + category
        var topQ = searchLogReader.topQueries(userId, 30, 30).keySet();     // ["베토벤 피아노", ...]
        var topC = searchLogReader.topCategories(userId, 30, 30).keySet();  // ["낭만파","피아노",...]

        var qTokens = new ArrayList<String>();
        for (var q : topQ) {
            for (var t : q.split("\\s+")) {
                if (t.length() >= 2) qTokens.add(t);
            }
        }

        // 3) 통합 + 중복 제거 + 상위 20개
        var merged = new LinkedHashSet<String>();
        merged.addAll(topC);
        merged.addAll(qTokens);
        merged.addAll(obTags);
        var topTags = merged.stream().limit(20).toList();

        // 4) 동적 alpha (온보딩 vs 활동 비중)
        int events = topQ.size() + topC.size();
        double alpha = Math.max(0.2, Math.min(0.8, 1.0 / (1.0 + (events / 30.0))));

        // 5) 팔로우
        var followees = new HashSet<>(followRepository.findFolloweeIds(userId));

        return UserSignals.builder()
                .topTags(topTags)
                .followeeIds(followees)
                .alpha(alpha)
                .build();
    }
}
