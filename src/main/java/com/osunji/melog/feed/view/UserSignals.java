package com.osunji.melog.feed;

import java.util.*;
import lombok.*;

@Getter
@Builder
public class UserSignals {
    private final List<String> topTags;    // 온보딩 + 검색로그에서 도출된 상위 키워드
    private final Set<String> followeeIds; // 내가 팔로우한 작성자 IDs
    private final double alpha;            // 온보딩 비중(동적 조절용, 필요시 가중치에 반영)
}
