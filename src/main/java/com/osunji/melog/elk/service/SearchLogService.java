package com.osunji.melog.elk.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.osunji.melog.elk.entity.SearchLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {

	private final ElasticsearchClient elasticsearchClient;

	/**
	 * 검색 로그 기록 - null 안전 처리
	 */
	public void logSearch(String query, String category, String userId) {
		try {
			// query 처리: null/빈 문자열 → "EMPTY_QUERY"로 변경
			String safeQuery = processQuery(query);

			// category 처리: null 허용 (그대로 전달)
			String safeCategory = processCategory(category);

			// userId 처리: null → "anonymous"로 변경
			String safeUserId = processUserId(userId);

			SearchLog searchLog = SearchLog.builder()
				.query(safeQuery)
				.category(safeCategory)  // null 허용
				.searchTime(LocalDateTime.now())
				.userId(safeUserId)
				.build();

			IndexRequest<SearchLog> request = IndexRequest.of(i -> i
				.index("search_logs")
				.document(searchLog)
			);

			elasticsearchClient.index(request);
			log.info("검색 로그 저장 완료: query='{}', category='{}', userId='{}'",
				safeQuery, safeCategory, safeUserId);

		} catch (Exception e) {
			log.error("검색 로그 기록 실패: query='{}', category='{}', error: {}",
				query, category, e.getMessage());
		}
	}

	/**
	 * query 필드 처리 (한글 지원)
	 */
	private String processQuery(String query) {
		if (query == null || query.trim().isEmpty()) {
			return "EMPTY_QUERY";
		}

		// UTF-8 안전성 보장 (한글 처리)
		try {
			String trimmedQuery = query.trim();
			// UTF-8 바이트로 변환 후 다시 문자열로 변환하여 인코딩 보장
			return new String(trimmedQuery.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.warn("Query 인코딩 처리 실패, 원본 사용: {}", query);
			return query.trim();
		}
	}

	/**
	 * category 필드 처리 (null 허용)
	 */
	private String processCategory(String category) {
		if (category == null) {
			return null;  // null 허용
		}

		String trimmed = category.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * userId 필드 처리
	 */
	private String processUserId(String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			return "anonymous";
		}
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