package com.osunji.melog.elk.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.osunji.melog.elk.entity.SearchLog;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {

	private final ElasticsearchClient elasticsearchClient;
	private final BlockingQueue<SearchLog> logQueue = new LinkedBlockingQueue<>(500);
	private final int BATCH_SIZE = 100;

	// 재시도 큐 대신 실패 로그 ID(또는 해시) 담는 단순 Set으로 중복재시도 방지
	private final java.util.Set<String> retryingLogIds = ConcurrentHashMap.newKeySet();

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@PostConstruct
	public void startScheduler() {
		scheduler.scheduleAtFixedRate(this::flushIfNeeded, 5, 5, TimeUnit.SECONDS);
	}
	public void logSearch(String query, String category, String userId) {
		try {
			SearchLog logDoc = SearchLog.builder()
				.id(UUID.randomUUID().toString())
				.query(processQuery(query))
				.category(processCategory(category))
				.searchTime(LocalDateTime.now())
				.userId(processUserId(userId))
				.build();

			if (!logQueue.offer(logDoc)) {
				log.warn("검색 로그 큐 가득 참, 폐기: query='{}', category='{}', userId='{}'",
					logDoc.getQuery(), logDoc.getCategory(), logDoc.getUserId());
				return;
			}

			if (logQueue.size() >= BATCH_SIZE) {
				scheduler.execute(this::flushLogQueue);
			}
		} catch (Exception e) {
			log.error("검색 로그 기록 실패: query='{}', category='{}', error={}",
				query, category, e.getMessage());
		}
	}
	private void flushIfNeeded() {
		if (!logQueue.isEmpty()) {
			flushLogQueue();
		}
	}

	private void flushLogQueue() {
		List<SearchLog> batch = new ArrayList<>();
		logQueue.drainTo(batch, BATCH_SIZE);

		if (batch.isEmpty()) return;

		try {
			elasticsearchClient.bulk(b -> {
				for (SearchLog log : batch) {
					b.operations(op -> op.index(idx -> idx.index("search_logs").document(log)));
				}
				return b;
			});
			log.info("벌크 검색 로그 저장 성공 ({}건)", batch.size());

			// 성공했으면 재시도 세트에서 ID 제거
			batch.forEach(l -> retryingLogIds.remove(l.getId()));

		} catch (Exception e) {
			log.error("벌크 검색 로그 저장 실패: {}", e.getMessage());

			// 재시도 실패한 로그만 한 번씩 재시도 대상에 넣기
			for (SearchLog log : batch) {
				if (!retryingLogIds.contains(log.getId())) {
					retryingLogIds.add(log.getId());
					// 재시도 위해 큐에 넣기(실패 로그만 한 번만 다시 시도)
					boolean reoffered = logQueue.offer(log);
					if (!reoffered) {
					}
				} else {
				}
			}
		}
	}
	/**
	 * query 필드 처리 (한글 지원)
	 */
	// 기존의 processXXX 메서드 재사용
	private String processQuery(String query) {
		if (query == null || query.trim().isEmpty()) return "EMPTY_QUERY";
		try {
			String trimmed = query.trim();
			return new String(trimmed.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.warn("Query 인코딩 실패, 원본 사용: {}", query);
			return query.trim();
		}
	}

	/**
	 * category 필드 처리 (null 허용)
	 */
	private String processCategory(String category) {
		if (category == null) return null;
		String trimmed = category.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String processUserId(String userId) {
		if (userId == null || userId.trim().isEmpty()) return "anonymous";
		return userId.trim();
	}
	/**
	 * 카테고리별 검색 로그 (명시적)
	 */
	public void logCategorySearch(String query, String category, String userId) {
		logSearch(query, category, userId);
	}

	/**
	 * 일반 검색 로그 (카테고리 없음)
	 */
	public void logGeneralSearch(String query, String userId) {
		logSearch(query, null, userId);  // category = null
	}

	/**
	 * 통합 검색 페이지 접근 로그
	 */
	public void logAllSearchAccess(String userId) {
		logSearch("ALL_SEARCH_PAGE", "page_access", userId);
	}

	/**
	 * 카테고리별 페이지 접근 로그
	 */
	public void logCategoryPageAccess(String category, String userId) {
		logSearch(category.toUpperCase() + "_PAGE", "category_page_access", userId);
	}

	/**
	 * 검색 결과 로그
	 */
	public void logSearchResult(String query, String searchType, int resultCount, String userId) {
		logSearch(query, "search_result_" + searchType, userId);
	}

	/**
	 * 벌크 검색 로그 저장
	 */
	public void logMultipleSearches(List<SearchLog> searchLogs) {
		try {
			for (SearchLog searchLog : searchLogs) {
				IndexRequest<SearchLog> request = IndexRequest.of(i -> i
					.index("search_logs")
					.document(searchLog)
				);
				elasticsearchClient.index(request);
			}
			log.info("벌크 검색 로그 저장 완료: {}개", searchLogs.size());
		} catch (Exception e) {
			log.error("벌크 검색 로그 저장 실패: {}", e.getMessage());
		}
	}
}