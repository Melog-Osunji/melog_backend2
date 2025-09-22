package com.osunji.melog.feed.reader;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class UserReader implements com.osunji.melog.feed.repository.UserReader {
    @Override
    public Map<UUID, UserProfile> batchFindProfiles(List<UUID> userIds) {
        // TODO: 나중에 UserRepository로 교체
        return Collections.emptyMap();
    }
}
