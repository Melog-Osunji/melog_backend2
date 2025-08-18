package com.osunji.melog.elk.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.osunji.melog.elk.entity.SearchLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {

	private final ElasticsearchClient elasticsearchClient;

	/**
	 * 검색 로그 기록
	 */
	public void logSearch(String query, String category, String userId) {
		try {
			SearchLog searchLog = SearchLog.builder()
				.query(query)
				.category(category)
				.searchTime(LocalDateTime.now())
				.userId(userId)
				.build();

			IndexRequest<SearchLog> request = IndexRequest.of(i -> i
				.index("search_logs")
				.document(searchLog)
			);

			elasticsearchClient.index(request);
		} catch (Exception e) {
			log.error("검색 로그 기록 실패: {}", e.getMessage());
		}
	}
}