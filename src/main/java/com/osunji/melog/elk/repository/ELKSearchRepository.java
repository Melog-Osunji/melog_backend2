package com.osunji.melog.elk.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.TermsExclude;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ELKSearchRepository {

	private final ElasticsearchClient elasticsearchClient;

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
			}
		} catch (Exception e) {
			log.error("인덱스 생성 실패: {}", e.getMessage());
		}
	}

	/**
	 *  인기 검색어 조회
	 */
	public List<String> getPopularSearchTerms() {
		try {
			// 인덱스 존재 확인
			ensureIndexExists("search_logs");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("search_logs")
				.size(0)
				.query(q -> q
					.range(r -> r
						.field("searchTime")
						.gte(JsonData.of(LocalDateTime.now().minusDays(3)))
					)
				)
				.aggregations("popular_terms", a -> a
					.terms(t -> t
						.field("query")
						.size(20)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.aggregations()
				.get("popular_terms")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("인기 검색어 조회 실패: {}", e.getMessage());
			// 실패 시 기본값 반환
			return Arrays.asList(
				"피아노", "교향곡", "협주곡", "소나타", "바이올린",
				"첼로", "오페라", "클래식", "바로크", "낭만주의"
			);
		}
	}


	/**
	 * 32번 API - 인기 작곡가 조회
	 */
	public List<String> getPopularComposers() {
		try {
			ensureIndexExists("search_logs");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("search_logs")
				.size(0)
				.query(q -> q
					.bool(b -> b
						.must(m -> m.term(t -> t.field("category").value("composer")))
						.must(m -> m.range(r -> r
							.field("searchTime")
							.gte(JsonData.of(LocalDateTime.now().minusDays(7)))
						))
					)
				)
				.aggregations("popular_composers", a -> a
					.terms(t -> t
						.field("query")
						.size(5)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.aggregations()
				.get("popular_composers")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("인기 작곡가 조회 실패: {}", e.getMessage());
			return Arrays.asList("베토벤", "모차르트", "쇼팽");
		}
	}
	/**
	 * 33번 API - 인기 연주가 조회 (검색량 기반)
	 */
	public List<String> getPopularPlayers() {
		try {
			ensureIndexExists("search_logs");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("search_logs")
				.size(0)
				.query(q -> q
					.bool(b -> b
						.must(m -> m.term(t -> t.field("category").value("player")))
						.must(m -> m.range(r -> r
							.field("searchTime")
							.gte(JsonData.of(LocalDateTime.now().minusDays(7)))
						))
					)
				)
				.aggregations("popular_players", a -> a
					.terms(t -> t
						.field("query")
						.size(10)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.aggregations()
				.get("popular_players")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("인기 연주가 조회 실패: {}", e.getMessage());
			return Arrays.asList("랑랑", "조성진", "정명훈");
		}
	}


	/**
	 * 35번 API - 인기 시대 조회 (검색량 + 태그량 기반)
	 */
	public List<String> getPopularPeriods() {
		try {
			ensureIndexExists("search_logs");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("search_logs")
				.size(0)
				.query(q -> q
					.bool(b -> b
						.must(m -> m.term(t -> t.field("category").value("period")))
						.must(m -> m.range(r -> r
							.field("searchTime")
							.gte(JsonData.of(LocalDateTime.now().minusDays(7)))
						))
					)
				)
				.aggregations("popular_periods", a -> a
					.terms(t -> t
						.field("query")
						.size(10)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.aggregations()
				.get("popular_periods")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("인기 시대 조회 실패: {}", e.getMessage());
			return Arrays.asList("바로크", "고전주의", "낭만주의", "근현대", "현대");
		}
	}

	/**
	 * 36번 API - 인기 악기 조회 (검색량 + 태그량 기반)
	 */
	public List<String> getPopularInstruments() {
		try {
			ensureIndexExists("search_logs");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("search_logs")
				.size(0)
				.query(q -> q
					.bool(b -> b
						.must(m -> m.term(t -> t.field("category").value("instrument")))
						.must(m -> m.range(r -> r
							.field("searchTime")
							.gte(JsonData.of(LocalDateTime.now().minusDays(7)))
						))
					)
				)
				.aggregations("popular_instruments", a -> a
					.terms(t -> t
						.field("query")
						.size(10)
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.aggregations()
				.get("popular_instruments")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("인기 악기 조회 실패: {}", e.getMessage());
			return Arrays.asList("피아노", "바이올린", "첼로", "플루트", "클라리넷");
		}
	}

	/**
	 * 37번~39번 API - 게시글 검색 (제목, 내용, 태그 기반)
	 */
	public List<String> searchPosts(String query) {
		try {
			ensureIndexExists("posts");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("posts")
				.query(q -> q
					.multiMatch(m -> m
						.query(query)
						.fields("title^2", "content", "tags^1.5")
					)
				)
				.sort(sort -> sort
					.field(f -> f.field("likeCount").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
				)
				.size(20)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.hits().hits().stream()
				.map(hit -> hit.id())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("게시글 검색 실패: {}", e.getMessage());
			return Arrays.asList();
		}
	}

	/**
	 * 38번 API - 사용자 프로필 검색
	 */
	public List<String> searchUsers(String query) {
		try {
			ensureIndexExists("users");

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("users")
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

	/**
	 * 연주가별 관련 키워드 조회 (33번 API용)
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
						.exclude((TermsExclude)Arrays.asList(playerName.toLowerCase()))  // 연주가 이름 제외
					)
				)
			);

			SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

			return response.aggregations()
				.get("related_keywords")
				.sterms()
				.buckets()
				.array()
				.stream()
				.map(bucket -> bucket.key().stringValue())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("연주가 관련 키워드 조회 실패: {}", e.getMessage());
			return getDefaultPlayerKeywords(playerName);
		}
	}

	/**
	 * 기본 연주가 키워드 (실패 시 사용)
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
