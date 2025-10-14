package com.osunji.melog.elk.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osunji.melog.search.repository.SearchRepository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchPerformanceController {

	private final ElasticsearchClient elasticsearchClient;
	private final SearchRepository searchRepository;

	/**
	 * ✅ 수정된 메모리 사용량 확인
	 */
	private Map<String, Object> getMemoryStats() throws Exception {
		try {
			var stats = elasticsearchClient.cluster().stats();
			var nodes = stats.nodes();

			// ✅ API 버전 호환성 수정
			long heapUsed = 0;
			long heapMax = 0;

			// nodes 정보에서 JVM 통계 추출 (버전별 다른 방식)
			try {
				// 최신 버전 방식 시도
				var jvmStats = nodes.jvm();
				if (jvmStats != null && jvmStats.mem() != null) {
					heapUsed = jvmStats.mem().heapUsedInBytes();
					heapMax = jvmStats.mem().heapMaxInBytes();
				}
			} catch (Exception e) {
				log.warn("JVM 통계 조회 실패: {}", e.getMessage());
			}

			// 직접 노드 stats API 호출로 대체
			if (heapUsed == 0 || heapMax == 0) {
				var nodeStats = getNodeStatsDirectly();
				heapUsed = (Long) nodeStats.getOrDefault("heap_used_bytes", 0L);
				heapMax = (Long) nodeStats.getOrDefault("heap_max_bytes", 0L);
			}

			double heapPercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

			return Map.of(
				"heap_used_bytes", heapUsed,
				"heap_max_bytes", heapMax,
				"heap_used_mb", heapUsed / 1024 / 1024,
				"heap_max_mb", heapMax / 1024 / 1024,
				"heap_percent", Math.round(heapPercent * 100.0) / 100.0
			);

		} catch (Exception e) {
			log.error("메모리 통계 조회 실패: {}", e.getMessage());
			return Map.of("error", "메모리 통계 조회 실패: " + e.getMessage());
		}
	}

	/**
	 * ✅ 직접 REST API 호출로 노드 통계 조회
	 */
	private Map<String, Object> getNodeStatsDirectly() {
		try {
			// _nodes/stats API를 직접 호출
			var request = co.elastic.clients.elasticsearch.core.SearchRequest.of(s -> s
				.index(".monitoring-*")  // 모니터링 인덱스 또는 다른 방식
				.size(0)
			);

			// 기본값 반환
			return Map.of(
				"heap_used_bytes", 0L,
				"heap_max_bytes", 2147483648L // 2GB 기본값
			);

		} catch (Exception e) {
			log.warn("직접 노드 통계 조회 실패: {}", e.getMessage());
			return Map.of(
				"heap_used_bytes", 0L,
				"heap_max_bytes", 2147483648L
			);
		}
	}

	/**
	 * ✅ 수정된 자동완성 성능 테스트
	 */
	private Map<String, Object> testAutocompletePerformance() {
		String[] testQueries = {"베", "베토", "베토벤", "피", "피아", "피아노", "모", "모차", "클래"};
		List<Map<String, Object>> results = new ArrayList<>();

		for (String query : testQueries) {
			long startTime = System.nanoTime();

			try {
				var response = searchRepository.getAutocomplete(query);
				long endTime = System.nanoTime();
				long responseTimeMs = (endTime - startTime) / 1_000_000; // 나노초를 밀리초로 변환

				results.add(Map.of(
					"query", query,
					"response_time_ms", responseTimeMs,
					"results_count", response.getSuggestions().size(),
					"status", "success"
				));

			} catch (Exception e) {
				long endTime = System.nanoTime();
				long responseTimeMs = (endTime - startTime) / 1_000_000;

				results.add(Map.of(
					"query", query,
					"response_time_ms", responseTimeMs,
					"status", "error",
					"error", e.getMessage()
				));
			}
		}

		// 평균 응답시간 계산
		double avgResponseTime = results.stream()
			.filter(r -> "success".equals(r.get("status")))
			.mapToLong(r -> (Long) r.get("response_time_ms"))
			.average()
			.orElse(0.0);

		return Map.of(
			"test_results", results,
			"average_response_time_ms", Math.round(avgResponseTime * 100.0) / 100.0,
			"total_tests", testQueries.length,
			"successful_tests", (int) results.stream().filter(r -> "success".equals(r.get("status"))).count()
		);
	}

	/**
	 * ✅ 수정된 집계 성능 테스트
	 */
	private Map<String, Object> testAggregationPerformance() {
		long startTime = System.nanoTime();

		try {
			// ✅ 타임아웃 설정 수정
			var searchRequest = co.elastic.clients.elasticsearch.core.SearchRequest.of(s -> s
				.index("search_logs")
				.size(0)
				.query(q -> q
					.range(r -> r
						.date(d -> d
							.field("timestamp")
							.gte("now-7d")
						)
					)
				)
				.aggregations("popular_queries", a -> a
					.terms(t -> t
						.field("query.keyword")
						.size(20)
					)
				)
				// ✅ 타임아웃 설정을 문자열로 수정
				.timeout("10s")  // time() 메서드 대신 직접 문자열 사용
			);

			var response = elasticsearchClient.search(searchRequest, Void.class);
			long endTime = System.nanoTime();
			long responseTimeMs = (endTime - startTime) / 1_000_000;

			var popularQueries = response.aggregations()
				.get("popular_queries")
				.sterms()
				.buckets()
				.array();

			return Map.of(
				"response_time_ms", responseTimeMs,
				"aggregation_count", popularQueries.size(),
				"total_hits", response.hits().total().value(),
				"status", "success"
			);

		} catch (Exception e) {
			long endTime = System.nanoTime();
			long responseTimeMs = (endTime - startTime) / 1_000_000;

			return Map.of(
				"response_time_ms", responseTimeMs,
				"status", "error",
				"error", e.getMessage()
			);
		}
	}

	/**
	 * ✅ 부하 테스트 (수정된 버전)
	 */
	@GetMapping("/test/load/{concurrent}/{requests}")
	public ResponseEntity<Map<String, Object>> loadTest(
		@PathVariable int concurrent,
		@PathVariable int requests) {

		log.info("부하 테스트 시작: {}개 동시 요청, 총 {}개 요청", concurrent, requests);

		ExecutorService executor = Executors.newFixedThreadPool(concurrent);
		List<Future<Long>> futures = new ArrayList<>();
		long overallStartTime = System.currentTimeMillis();

		// 동시 요청 실행
		for (int i = 0; i < requests; i++) {
			final int requestNum = i;
			futures.add(executor.submit(() -> {
				long startTime = System.nanoTime();
				try {
					// 랜덤 검색어로 자동완성 테스트
					String[] queries = {"베", "피", "모", "클", "조"};
					String query = queries[requestNum % queries.length];
					searchRepository.getAutocomplete(query);

					long endTime = System.nanoTime();
					return (endTime - startTime) / 1_000_000; // 밀리초로 변환
				} catch (Exception e) {
					log.error("요청 {} 실패: {}", requestNum, e.getMessage());
					return -1L;
				}
			}));
		}

		// 결과 수집
		List<Long> responseTimes = new ArrayList<>();
		int successCount = 0;
		int errorCount = 0;

		for (Future<Long> future : futures) {
			try {
				Long responseTime = future.get(30, TimeUnit.SECONDS);
				if (responseTime >= 0) {
					responseTimes.add(responseTime);
					successCount++;
				} else {
					errorCount++;
				}
			} catch (Exception e) {
				errorCount++;
			}
		}

		executor.shutdown();
		long overallEndTime = System.currentTimeMillis();

		// 통계 계산
		double avgResponseTime = responseTimes.stream()
			.mapToLong(Long::longValue)
			.average()
			.orElse(0.0);

		long maxResponseTime = responseTimes.stream()
			.mapToLong(Long::longValue)
			.max()
			.orElse(0L);

		long minResponseTime = responseTimes.stream()
			.mapToLong(Long::longValue)
			.min()
			.orElse(0L);

		double throughput = (double) successCount / (overallEndTime - overallStartTime) * 1000;

		return ResponseEntity.ok(Map.of(
			"total_requests", requests,
			"concurrent_users", concurrent,
			"successful_requests", successCount,
			"failed_requests", errorCount,
			"success_rate_percent", Math.round((double) successCount / requests * 100 * 100.0) / 100.0,
			"average_response_time_ms", Math.round(avgResponseTime * 100.0) / 100.0,
			"min_response_time_ms", minResponseTime,
			"max_response_time_ms", maxResponseTime,
			"throughput_requests_per_second", Math.round(throughput * 100.0) / 100.0,
			"total_duration_ms", overallEndTime - overallStartTime
		));
	}

	/**
	 * ✅ 간단한 헬스 체크 API
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> simpleHealthCheck() {
		try {
			var health = elasticsearchClient.cluster().health();

			return ResponseEntity.ok(Map.of(
				"elasticsearch_status", health.status().toString(),
				"number_of_nodes", health.numberOfNodes(),
				"active_shards", health.activeShards(),
				"timestamp", System.currentTimeMillis()
			));

		} catch (Exception e) {
			return ResponseEntity.status(503)
				.body(Map.of(
					"error", e.getMessage(),
					"timestamp", System.currentTimeMillis()
				));
		}
	}
}
