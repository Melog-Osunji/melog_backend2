package com.osunji.melog.elk.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.osunji.melog.elk.entity.UserLog;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogService {

    private final ElasticsearchClient elasticsearchClient;
    private final BlockingQueue<UserLog> logQueue = new LinkedBlockingQueue<>(500);
    private final BlockingQueue<UserLog> retryQueue = new LinkedBlockingQueue<>(2000);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final int BATCH_SIZE = 100;
    private final int RETRY_MAX_ATTEMPTS = 5;


    @PostConstruct
    public void startScheduler() {
        // 주기적 기본 로그 플러시
        scheduler.scheduleAtFixedRate(this::flushIfNeeded, 5, 5, TimeUnit.SECONDS);
        // 주기적 재전송 큐 플러시
        scheduler.scheduleAtFixedRate(this::flushRetryQueue, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 유저 이벤트 로그 기록 - null/빈값 안전 처리ddd
     */
    public void logUserEvent(String userId, String eventType, String ip, String userAgent, String metaJson) {
        try {
            UserLog logDoc = UserLog.builder()
                .id(UUID.randomUUID().toString())
                .userId(processUserId(userId))
                .eventType(processEventType(eventType))
                .eventTime(LocalDateTime.now())
                .ip(processIp(ip))
                .userAgent(processUserAgent(userAgent))
                .metaJson(processMetaJson(metaJson))
                .build();

            if (!logQueue.offer(logDoc)) {
                log.error("로그 큐 가득 참, 로그 폐기: userId={}, eventType={}", logDoc.getUserId(), logDoc.getEventType());
                return;
            }

            if (logQueue.size() >= BATCH_SIZE) {
                scheduler.execute(this::flushLogQueue);
            }
        } catch (Exception e) {
            log.error("유저 로그 기록 실패: userId='{}', eventType='{}', error={}",
                userId, eventType, e.getMessage(), e);
        }
    }
    private void flushIfNeeded() {
        if (!logQueue.isEmpty()) {
            flushLogQueue();
        }
    }

    private void flushLogQueue() {
        List<UserLog> batch = new ArrayList<>();
        logQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) return;

        try {
            elasticsearchClient.bulk(b -> {
                for (UserLog log : batch) {
                    b.operations(op -> op.index(idx -> idx.index("user_logs").document(log)));
                }
                return b;
            });
            log.info("벌크 로그 저장 성공 ({}건)", batch.size());
        } catch (Exception e) {
            log.error("벌크 로그 저장 실패: {}, 재전송 큐에 적재", e.getMessage());
            // 실패한 로그는 retry 큐에 적재
            for (UserLog log : batch) {
                if (!retryQueue.offer(log)) {
                }
            }
        }
    }
    private void flushRetryQueue() {
        List<UserLog> retryBatch = new ArrayList<>();
        retryQueue.drainTo(retryBatch, BATCH_SIZE);

        if (retryBatch.isEmpty()) return;

        try {
            elasticsearchClient.bulk(b -> {
                for (UserLog log : retryBatch) {
                    b.operations(op -> op.index(idx -> idx.index("user_logs").document(log)));
                }
                return b;
            });
            log.info("재전송 큐 벌크 저장 성공 ({}건)", retryBatch.size());
        } catch (Exception e) {
            log.error("재전송 큐 벌크 저장 실패: {}", e.getMessage(), e);
            // 재전송 실패 시 다시 큐에 적재 (백오프 재시도 정책 필요)
            for (UserLog log : retryBatch) {
                if (!retryQueue.offer(log)) {
                }
            }
        }
    }
    /* ===================== 편의 메서드들 (필요 시 선택 사용) ===================== */

    public void logLogin(String userId, String ip, String userAgent, String provider) {
        logUserEvent(userId, "LOGIN", ip, userAgent, jsonOf(Map.of("provider", provider)));
    }

    public void logLoginFail(String ip, String userAgent, String reason) {
        logUserEvent("anonymous", "USER_LOGIN_FAIL", ip, userAgent, jsonOf(Map.of("reason", reason)));
    }

    public void logLogout(String userId, String ip, String userAgent) {
        logUserEvent(userId, "LOGOUT", ip, userAgent, "{}");
    }

    public void logSignup(String userId, String ip, String userAgent, String provider) {
        logUserEvent(userId, "SIGNUP", ip, userAgent, jsonOf(Map.of("provider", provider)));
    }

    public void logTokenRefresh(String userId, String ip, String userAgent, String jti, Integer ttlSec) {
        logUserEvent(userId, "TOKEN_REFRESH", ip, userAgent, jsonOf(Map.of("jti", jti, "ttlSec", ttlSec)));
    }

    public void logFollow(String userId, String targetUserId) {
        logUserEvent(userId, "FOLLOW", null, null, jsonOf(Map.of("targetUserId", targetUserId)));
    }

    public void logUnfollow(String userId, String targetUserId) {
        logUserEvent(userId, "UNFOLLOW", null, null, jsonOf(Map.of("targetUserId", targetUserId)));
    }

    public void logPreferenceUpdate(String userId, String key, String value) {
        logUserEvent(userId, "PREFERENCE_UPDATE", null, null, jsonOf(Map.of("key", key, "value", value)));
    }

    /**
     * 벌크 유저 로그 저장 (간단 루프; 대량이면 Bulk API 별도 구성 권장)
     */
    public void logMultipleUserEvents(List<UserLog> logs) {
        try {
            for (UserLog doc : logs) {
                UserLog safe = UserLog.builder()
                        .id(doc.getId() != null ? doc.getId() : UUID.randomUUID().toString())
                        .userId(processUserId(doc.getUserId()))
                        .eventType(processEventType(doc.getEventType()))
                        .eventTime(doc.getEventTime() != null ? doc.getEventTime() : LocalDateTime.now())
                        .ip(processIp(doc.getIp()))
                        .userAgent(processUserAgent(doc.getUserAgent()))
                        .metaJson(processMetaJson(doc.getMetaJson()))
                        .build();

                IndexRequest<UserLog> req = IndexRequest.of(i -> i.index("user_logs").document(safe));
                elasticsearchClient.index(req);
            }
        } catch (Exception e) {
        }
    }

    /* ============================= 필드 처리 ============================= */

    private String processUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) return "anonymous";
        return userId.trim();
    }

    private String processEventType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) return "UNKNOWN";
        // UTF-8 안전성 보장 + 대문자 통일
        try {
            String trimmed = eventType.trim().toUpperCase();
            return new String(trimmed.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("eventType 인코딩 처리 실패, 원본 사용: {}", eventType);
            return eventType.trim().toUpperCase();
        }
    }

    private String processIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) return null; // null 허용
        return ip.trim();
    }

    private String processUserAgent(String ua) {
        if (ua == null || ua.trim().isEmpty()) return null; // null 허용
        // 너무 길면 잘림 방지: 256자 제한 가정(매핑 ignore_above=256 권장)
        String t = ua.trim();
        return t.length() > 256 ? t.substring(0, 256) : t;
    }

    private String processMetaJson(String metaJson) {
        if (metaJson == null || metaJson.trim().isEmpty()) return "{}";
        return metaJson.trim();
    }

    private String jsonOf(Map<String, ?> map) {
        // 간단/의존성 최소: 매우 단순한 JSON 직렬화 (특수문자 없다는 가정)
        // Jackson 사용 가능하면 ObjectMapper로 교체해도 됨.
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(e.getKey()).append('"').append(':');
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
            else sb.append('"').append(String.valueOf(v).replace("\"","\\\"")).append('"');
        }
        return sb.append('}').toString();
    }
}
