package com.osunji.melog.feed.repository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserReader {
    record UserProfile(UUID id, String nickName, String profileImg) {}
    Map<UUID, UserProfile> batchFindProfiles(List<UUID> userIds);
}