package com.osunji.melog.elk.repository;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import com.osunji.melog.elk.entity.SearchLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ELKSearchRepository {

	private final ElasticsearchClient elasticsearchClient;

	/**
	 * 검색 로그 기록 (실제 기능)
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
			log.info("검색 로그 저장 완료: query={}, category={}, userId={}", query, category, userId);
		} catch (Exception e) {
			log.error("검색 로그 기록 실패: {}", e.getMessage());
		}
	}
	/**
	 * 인기 검색어 조회 (최근 7일, 최대 20개)
	 */
	public boolean testConnection() {
		try {
			System.out.println("🔍 ELK 연결 테스트 시작");

			var response = elasticsearchClient.info();
			System.out.println("✅ ELK 연결 성공: " + response.clusterName());
			System.out.println("  - 클러스터: " + response.clusterName());
			System.out.println("  - 버전: " + response.version().number());

			return true;
		} catch (Exception e) {
			System.out.println("❌ ELK 연결 실패: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 인기 검색어 조회 - 20개 빈도 내림차순
	 */
	public List<String> getPopularSearchTerms() {
		try {
			System.out.println("📊 ELK 인기 검색어 집계 시작 (20개 빈도순)");
			ensureIndexExists("search_logs");

			SearchRequest searchRequest = SearchRequest.of(s -> s
					.index("search_logs")
					.size(0)
					.query(q -> q.range(r -> r
							.date(d -> d
									.field("searchTime")
									.gte("now-3d")
							)
					))

				.aggregations("popular_terms", a -> a
					.terms(t -> t
						.field("query")
						.size(20)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			List<String> results = response.aggregations()
				.get("popular_terms")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> {
					String term = bucket.key().stringValue();
					long count = bucket.docCount();
					System.out.println("    🔥 " + count + "회: '" + term + "'");
					return term;
				})
				.collect(Collectors.toList());  // ✅ 필터링 제거, 모든 검색어 포함

			System.out.println("✅ ELK 인기 검색어 " + results.size() + "개 (빈도순)");

			// ✅ 20개 미만이면 그냥 반환, 20개 이상이면 20개만
			return results;

		} catch (Exception e) {
			log.error("인기 검색어 조회 실패: {}", e.getMessage(), e);

			// ✅ 실패 시에도 20개 기본값
			return Arrays.asList(
				"피아노", "교향곡", "협주곡", "소나타", "바이올린",
				"첼로", "오페라", "클래식", "바로크", "낭만주의",
				"베토벤", "모차르트", "쇼팽", "바흐", "브람스",
				"리스트", "드뷔시", "라벨", "바그너", "브루크너"
			);
		}
	}

	/**
	 * 카테고리별 인기 검색어 조회
	 */
	public List<String> getPopularSearchByCategory(String category, int days, int size) {
		try {
			ensureIndexExists("search_logs");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("search_logs")
				.size(0)
				.query(q -> q
					.bool(b -> b
						.must(m -> m.term(t -> t.field("category").value(category)))
						.must(m -> m.range(r -> r
								.date(d->d
										.field("searchTime")
										.gte("now-3d"))
						))
					)
				)
				.aggregations("popular_" + category, a -> a
					.terms(t -> t
						.field("query")
						.size(size)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.aggregations()
				.get("popular_" + category)
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("{} 카테고리 인기 검색어 조회 실패: {}", category, e.getMessage());
			return getDefaultByCategory(category);
		}
	}

	/**
	 * 인기 작곡가 조회 (최근 7일)
	 */
	public List<String> getPopularComposers() {
		return getPopularSearchByCategory("composer", 7, 5);
	}

	/**
	 * 인기 연주가 조회 (최근 7일)
	 */
	public List<String> getPopularPlayers() {
		return getPopularSearchByCategory("player", 7, 10);
	}

	/**
	 * 인기 시대 조회 (최근 7일)
	 */
	public List<String> getPopularPeriods() {
		return getPopularSearchByCategory("period", 7, 10);
	}

	/**
	 * 인기 악기 조회 (최근 7일)
	 */
	public List<String> getPopularInstruments() {
		return getPopularSearchByCategory("instrument", 7, 10);
	}

	/**
	 * 연주가별 관련 키워드 조회
	 */
	public List<String> getPlayerRelatedKeywords(String playerName) {
		try {
			ensureIndexExists("posts");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("posts")
				.size(0)
				.query(q -> q
					.multiMatch(m -> m
						.query(playerName)
						.fields("title", "content", "tags")
					)
				)
				.aggregations("related_keywords", a -> a
					.terms(t -> t
						.field("tags")
						.size(5)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			List<String> keywords = response.aggregations()
				.get("related_keywords")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.filter(keyword -> !keyword.toLowerCase().contains(playerName.toLowerCase()))
				.collect(Collectors.toList());

			return keywords.isEmpty() ? getDefaultPlayerKeywords(playerName) : keywords;

		} catch (Exception e) {
			log.error("연주가 관련 키워드 조회 실패: {}", e.getMessage());
			return getDefaultPlayerKeywords(playerName);
		}
	}

	/**
	 * 게시글 검색 (제목, 내용, 태그)
	 */
	public List<String> searchPosts(String q) {
		if (q == null || q.isBlank()) return List.of();

		List<String> out = new ArrayList<>();
		try {
			var shoulds = new ArrayList<Query>();

			// 기본 multi_match
			shoulds.add(Query.of(qb -> qb
				.multiMatch(m -> m
					.query(q)
					.fields("title^3", "content", "tags")
				)
			));

			// 짧은 쿼리일수록 wildcard 보강
			if (q.length() <= 2) {
				shoulds.add(Query.of(qb -> qb.wildcard(w -> w.field("title").value("*" + q + "*"))));
				shoulds.add(Query.of(qb -> qb.wildcard(w -> w.field("content").value("*" + q + "*"))));
				shoulds.add(Query.of(qb -> qb.wildcard(w -> w.field("tags").value("*" + q + "*"))));
			} else {
				shoulds.add(Query.of(qb -> qb.wildcard(w -> w.field("title").value("*" + q + "*"))));
			}

			var req = SearchRequest.of(s -> s
				.index("posts")
				.size(200)
				.query(qb -> qb.bool(b -> b
					.should(shoulds)
					.minimumShouldMatch("1")
				))
				.source(src -> src.filter(f -> f.includes("id")))
			);

			var resp = elasticsearchClient.search(req, Map.class);

			resp.hits().hits().forEach(h -> {
				Map<String, Object> src = h.source();
				if (src != null && src.get("id") != null) {
					out.add(String.valueOf(src.get("id")));
				}
			});

			return out;
		} catch (Exception e) {
			return List.of();
		}
	}



	/**
	 * 사용자 검색 (닉네임, 자기소개)
	 */
	public List<String> searchUsers(String query) {
		try {
			ensureIndexExists("user");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("user")
				.query(q -> q
					.multiMatch(m -> m
						.query(query)
						.fields("nickname^2", "intro")
					)
				)
				.size(10)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.hits().hits().stream()
				.map(hit -> hit.id())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("사용자 검색 실패: {}", e.getMessage());
			return Arrays.asList();
		}
	}

	// =========================== 헬퍼 메서드들 ===========================

	/**
	 * 인덱스 존재 확인 및 생성
	 */
	private void ensureIndexExists(String indexName) {
		try {
			GetIndexRequest getIndexRequest = GetIndexRequest.of(g -> g.index(indexName));
			elasticsearchClient.indices().get(getIndexRequest);
		} catch (Exception e) {
			createIndex(indexName);
		}
	}

	/**
	 * 인덱스 생성
	 */
	private void createIndex(String indexName) {
		try {
			if ("search_logs".equals(indexName)) {
				CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
					.index(indexName)
					.mappings(m -> m
						.properties("query", p -> p.keyword(k -> k))
						.properties("category", p -> p.keyword(k -> k))
						.properties("searchTime", p -> p.date(d -> d))
						.properties("userId", p -> p.keyword(k -> k))
					)
				);
				elasticsearchClient.indices().create(createRequest);
				log.info("search_logs 인덱스 생성 완료");
			}
		} catch (Exception e) {
			log.error("인덱스 생성 실패: {}", e.getMessage());
		}
	}

	/**
	 * 카테고리별 기본값 반환
	 */
	private List<String> getDefaultByCategory(String category) {
		switch (category) {
			case "composer":
				return Arrays.asList("베토벤", "모차르트", "쇼팽");
			case "player":
				return Arrays.asList("랑랑", "조성진", "정명훈");
			case "period":
				return Arrays.asList("바로크", "고전주의", "낭만주의", "근현대", "현대");
			case "instrument":
				return Arrays.asList("피아노", "바이올린", "첼로", "플루트", "클라리넷");
			default:
				return Arrays.asList();
		}
	}

	/**
	 * 연주가별 기본 키워드
	 */
	private List<String> getDefaultPlayerKeywords(String playerName) {
		switch (playerName.toLowerCase()) {
			case "랑랑":
				return Arrays.asList("피아노", "협주곡", "중국", "베토벤");
			case "조성진":
				return Arrays.asList("쇼팽", "피아노", "콩쿠르", "한국");
			case "정명훈":
				return Arrays.asList("지휘", "오케스트라", "서울시향", "프랑스");
			default:
				return Arrays.asList("클래식", "연주", "음악");
		}
	}
}
