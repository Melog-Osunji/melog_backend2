package com.osunji.melog.search.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.repository.UserRepository;
import com.osunji.melog.user.repository.FollowRepository;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.repository.CommentRepository;
import com.osunji.melog.elk.repository.ELKSearchRepository;
import com.osunji.melog.elk.service.SearchLogService;
import com.osunji.melog.search.preset.SearchPresetLoader;
import com.osunji.melog.search.dto.response.SearchResponse;
import com.osunji.melog.global.common.AuthHelper;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.ScoreSort;
import com.osunji.melog.search.dto.AutocompleteKeyword;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.CompletableFuture;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SearchRepository {

	private final ELKSearchRepository elkSearchRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final FollowRepository followRepository;
	private final CommentRepository commentRepository;
	private final SearchLogService searchLogService;
	private final SearchPresetLoader searchPresetLoader;
	private final ElasticsearchClient elasticsearchClient;
	private final AuthHelper authHelper;

	/** 31번 통합 검색 데이터 조회 - /api/search/all */
	public SearchResponse.AllSearch getAllSearchData() {
		try {
			log.info("🔍 통합 검색 데이터 조회 시작");

			// ✅ 실제 ELK에서 인기 검색어 20개 조회 (최근 7일)
			List<String> livePopularSearch = getActualPopularSearchTerms();
			log.info("  - ELK에서 조회된 인기 검색어 수: {} ", livePopularSearch.size());

			// 추천 키워드 6개
			List<String> recommendKeywords = Arrays.asList(
				"베토벤", "모차르트", "쇼팽", "바흐", "브람스", "리스트"
			);

			// 현재 시간
			String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));
			log.info("✅ 통합 검색 데이터 조회 완료 - 현재 시간: {}" ,nowTime);

			return SearchResponse.AllSearch.builder()
				.recommendKeyword(recommendKeywords)
				.livePopularSearch(livePopularSearch)
				.nowTime(nowTime)
				.build();

		} catch (Exception e) {
			log.error("통합 검색 데이터 조회 실패: {}", e.getMessage(), e);
			return SearchResponse.AllSearch.builder()
				.recommendKeyword(Arrays.asList("베토벤", "모차르트", "쇼팽", "바흐", "브람스", "리스트"))
				.livePopularSearch(Arrays.asList("클래식", "피아노", "오케스트라", "교향곡", "협주곡"))
				.nowTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")))
				.build();
		}
	}

	private List<String> getActualPopularSearchTerms() {
		try {
			log.info("📊 실제 인기 검색어 집계 시작");
			List<String> popularTerms = elkSearchRepository.getPopularSearchTerms();
			log.info("🔍 ELK에서 받은 데이터: {}개" , popularTerms.size() );
			return popularTerms.isEmpty() ? getDefaultPopularTerms() : popularTerms;
		} catch (Exception e) {
			log.error("실제 인기 검색어 조회 실패: {}", e.getMessage(), e);
			return getDefaultPopularTerms();
		}
	}

	private List<String> getDefaultPopularTerms() {
		return Arrays.asList(
			"피아노", "교향곡", "협주곡", "소나타", "바이올린",
			"첼로", "오페라", "클래식", "바로크", "낭만주의"
		);
	}

	/** 32번 인기 작곡가 조회 - /api/search/composer */
	public List<SearchResponse.Composer> getPopularComposers() {
		try {
			log.info("🎼 인기 작곡가 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode composerPreset = searchPresetLoader.composer();
			if (composerPreset == null || !composerPreset.has("name") || !composerPreset.has("imgLink")) {
				log.info("  - 사전 설정 파일 없음, 기본값 사용");
				return getDefaultComposers();
			}

			List<String> allComposers = new ObjectMapper().convertValue(
				composerPreset.get("name"), List.class);
			List<String> allImgLinks = new ObjectMapper().convertValue(
				composerPreset.get("imgLink"), List.class);

			log.info("  - 사전 설정 작곡가 수:{} ", allComposers.size());

			// ✅ 2단계: 각 작곡가별 검색 빈도 조회
			Map<String, Long> searchCounts = getComposerSearchCounts(allComposers);

			// ✅ 3단계: 검색량 순으로 정렬 (많은 순 → 적은 순)
			List<Integer> sortedIndexes = IntStream.range(0, allComposers.size())
				.boxed()
				.sorted((i1, i2) -> {
					String composer1 = allComposers.get(i1);
					String composer2 = allComposers.get(i2);
					Long count1 = searchCounts.getOrDefault(composer1, 0L);
					Long count2 = searchCounts.getOrDefault(composer2, 0L);

					// 검색량 기준 내림차순 정렬
					return Long.compare(count2, count1);
				})
				.collect(Collectors.toList());

			// ✅ 4단계: 정렬된 순서로 작곡가와 이미지 재배열
			List<String> sortedComposers = sortedIndexes.stream()
				.map(allComposers::get)
				.collect(Collectors.toList());

			List<String> sortedImgLinks = sortedIndexes.stream()
				.map(allImgLinks::get)
				.collect(Collectors.toList());

			// ✅ 5단계: 정렬 결과 로그 출력
			log.info("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, sortedComposers.size()); i++) {
				String composer = sortedComposers.get(i);
				Long count = searchCounts.getOrDefault(composer, 0L);
				log.info("    {}. {} (검색 {}회)", i + 1, composer, count);
			}

			return Arrays.asList(
				SearchResponse.Composer.builder()
					.name(sortedComposers)
					.imgLink(sortedImgLinks)
					.build()
			);

		} catch (Exception e) {
			log.error("작곡가 조회 실패: {}", e.getMessage(), e);
			return getDefaultComposers();
		}
	}

	/**
	 * 작곡가별 검색 빈도 조회
	 */
	private Map<String, Long> getComposerSearchCounts(List<String> composers) {
		Map<String, Long> searchCounts = new HashMap<>();

		try {
			log.info("📊 작곡가별 검색 빈도 조회 시작");

			for (String composer : composers) {
				try {
					Long count = getSearchCountForKeyword(composer);
					searchCounts.put(composer, count);
					if (count > 0) {
						log.info("    {}: {}회", composer, count);
					}
				} catch (Exception e) {
					log.warn("    {}: 조회 실패", composer, e);
					searchCounts.put(composer, 0L);
				}
			}


		} catch (Exception e) {
			log.error("작곡가 검색 빈도 조회 실패: {}", e.getMessage());
		}

		return searchCounts;
	}

	/**
	 * 기본 작곡가 목록 반환
	 */
	private List<SearchResponse.Composer> getDefaultComposers() {
		return Arrays.asList(
			SearchResponse.Composer.builder()
				.name(Arrays.asList("베토벤", "모차르트", "쇼팽", "바흐", "브람스", "리스트"))
				.imgLink(Arrays.asList(
					"https://example.com/images/composer/beethoven.jpg",
					"https://example.com/images/composer/mozart.jpg",
					"https://example.com/images/composer/chopin.jpg",
					"https://example.com/images/composer/bach.jpg",
					"https://example.com/images/composer/brahms.jpg",
					"https://example.com/images/composer/liszt.jpg"
				))
				.build()
		);
	}

	/** 33번 인기 연주가 + 관련 키워드 조회 - /api/search/player */
	public List<SearchResponse.Player> getPopularPlayers() {
		try {
			log.info("🎹 인기 연주가 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode playerPreset = searchPresetLoader.player();
			if (playerPreset == null || !playerPreset.isArray()) {
				log.info("  - 사전 설정 파일 없음, ELK 조회");
				return getPlayersFromElk();
			}

			List<SearchResponse.Player> allPlayers = new ArrayList<>();
			ObjectMapper mapper = new ObjectMapper();

			// JSON 배열을 Player 객체로 변환
			for (JsonNode playerNode : playerPreset) {
				String playerName = playerNode.get("name").asText();
				List<String> keywords = mapper.convertValue(playerNode.get("keyword"), List.class);

				allPlayers.add(SearchResponse.Player.builder()
					.name(playerName)
					.keyword(keywords)
					.build());
			}

			log.info("  - 사전 설정 연주가 수: {}",allPlayers.size());

			// ✅ 2단계: 각 연주가별 검색 빈도 조회 후 정렬
			allPlayers.sort((p1, p2) -> {
				Long count1 = getSearchCountForKeyword(p1.getName());
				Long count2 = getSearchCountForKeyword(p2.getName());
				return Long.compare(count2, count1); // 내림차순
			});

			// ✅ 3단계: 정렬 결과 로그 출력
			log.info("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, allPlayers.size()); i++) {
				SearchResponse.Player player = allPlayers.get(i);
				Long count = getSearchCountForKeyword(player.getName());
				log.info("    {}. {} (검색 {}회)", i + 1, player.getName(), count);
			}

			return allPlayers;

		} catch (Exception e) {
			log.error("연주가 조회 실패: {}", e.getMessage(), e);
			return getPlayersFromElk();
		}
	}

	/**
	 * ELK에서 연주가 조회 (fallback)
	 */
	private List<SearchResponse.Player> getPlayersFromElk() {
		try {
			List<String> popularPlayers = elkSearchRepository.getPopularPlayers();
			return popularPlayers.stream()
				.map(playerName -> {
					List<String> keywords = elkSearchRepository.getPlayerRelatedKeywords(playerName);
					return SearchResponse.Player.builder()
						.name(playerName)
						.keyword(keywords)
						.build();
				})
				.collect(Collectors.toList());
		} catch (Exception e) {
			return getDefaultPlayers();
		}
	}

	/**
	 * 기본 연주가 목록 반환
	 */
	private List<SearchResponse.Player> getDefaultPlayers() {
		return Arrays.asList(
			SearchResponse.Player.builder()
				.name("조성진")
				.keyword(Arrays.asList("쇼팽", "피아노", "콩쿠르", "한국"))
				.build(),
			SearchResponse.Player.builder()
				.name("랑랑")
				.keyword(Arrays.asList("피아노", "협주곡", "중국", "베토벤"))
				.build(),
			SearchResponse.Player.builder()
				.name("정명훈")
				.keyword(Arrays.asList("지휘", "오케스트라", "서울시향", "프랑스"))
				.build(),
			SearchResponse.Player.builder()
				.name("임윤찬")
				.keyword(Arrays.asList("피아노", "한국", "클리번", "젊은"))
				.build()
		);
	}

	/** 34번 장르 + 관련 키워드 조회 - /api/search/genre */
	public List<SearchResponse.Genre> getGenres() {
		try {
			log.info("🎵 장르 데이터 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode genrePreset = searchPresetLoader.genre();
			if (genrePreset == null || !genrePreset.isArray()) {
				log.info("  - 사전 설정 파일 없음, 기본값 사용");
				return getDefaultGenres();
			}

			List<SearchResponse.Genre> allGenres = new ArrayList<>();
			ObjectMapper mapper = new ObjectMapper();

			// JSON 배열을 Genre 객체로 변환
			for (JsonNode genreNode : genrePreset) {
				String genreName = genreNode.get("genre").asText();
				List<String> keywords = mapper.convertValue(genreNode.get("keyword"), List.class);

				allGenres.add(SearchResponse.Genre.builder()
					.genre(genreName)
					.keyword(keywords)
					.build());
			}

			log.info("  - 사전 설정 장르 수: " , allGenres.size());

			// ✅ 2단계: 각 장르별 검색 빈도 조회 후 정렬
			allGenres.sort((g1, g2) -> {
				Long count1 = getSearchCountForKeyword(g1.getGenre());
				Long count2 = getSearchCountForKeyword(g2.getGenre());
				return Long.compare(count2, count1); // 내림차순
			});

			// ✅ 3단계: 정렬 결과 로그 출력
			log.info("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, allGenres.size()); i++) {
				SearchResponse.Genre genre = allGenres.get(i);
				Long count = getSearchCountForKeyword(genre.getGenre());
				log.info("    {}. {} (검색 {}회)", i + 1, genre.getGenre(), count);
			}

			return allGenres;

		} catch (Exception e) {
			log.error("장르 조회 실패: {}", e.getMessage(), e);
			return getDefaultGenres();
		}
	}

	/**
	 * 기본 장르 목록 반환
	 */
	private List<SearchResponse.Genre> getDefaultGenres() {
		return Arrays.asList(
			SearchResponse.Genre.builder()
				.genre("클래식")
				.keyword(Arrays.asList("교향곡", "협주곡", "소나타", "오케스트라"))
				.build(),
			SearchResponse.Genre.builder()
				.genre("바로크")
				.keyword(Arrays.asList("바흐", "헨델", "푸가", "하프시코드"))
				.build(),
			SearchResponse.Genre.builder()
				.genre("낭만주의")
				.keyword(Arrays.asList("쇼팽", "리스트", "브람스", "감정표현"))
				.build(),
			SearchResponse.Genre.builder()
				.genre("재즈")
				.keyword(Arrays.asList("즉흥연주", "블루스", "스윙", "비밥"))
				.build()
		);
	}

	/** 35번 인기 시대 조회 - /api/search/period */
	public SearchResponse.Period getPeriods() {
		try {
			log.info("⏰ 시대 데이터 조회 시작");

			// ✅ 1순위: 사전 설정값 사용
			JsonNode periodPreset = searchPresetLoader.period();
			if (periodPreset != null && periodPreset.has("era")) {
				log.info("  - 사전 설정 시대 데이터 사용");

				List<String> eras = new ObjectMapper().convertValue(
					periodPreset.get("era"), List.class);

				log.info("  - 사전 설정 시대 수:{} " ,eras.size());

				return SearchResponse.Period.builder()
					.era(eras)
					.build();
			}

			// ✅ 2순위: Elasticsearch에서 인기 시대 조회
			List<String> popularPeriods = elkSearchRepository.getPopularPeriods();
			if (!popularPeriods.isEmpty()) {
				log.info("  - ELK에서 조회된 시대 수: {}", popularPeriods.size());
				return SearchResponse.Period.builder()
					.era(popularPeriods)
					.build();
			}

		} catch (Exception e) {
			log.error("시대 조회 실패: {}", e.getMessage());
		}

		// ✅ 3순위: 기본값 반환
		log.info("  - 기본값 시대 반환");
		return SearchResponse.Period.builder()
			.era(Arrays.asList("바로크", "고전주의", "낭만주의", "근현대", "현대"))
			.build();
	}

	/** 36번 인기 악기 조회 - /api/search/instrument */
	public SearchResponse.Instrument getInstruments() {
		try {
			log.info("🎺 인기 악기 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode instrumentPreset = searchPresetLoader.instrument();
			if (instrumentPreset == null || !instrumentPreset.has("instrument") || !instrumentPreset.has("imgLink")) {
				log.info("  - 사전 설정 파일 없음, 기본값 사용");
				return getDefaultInstruments();
			}

			List<String> allInstruments = new ObjectMapper().convertValue(
				instrumentPreset.get("instrument"), List.class);
			List<String> allImgLinks = new ObjectMapper().convertValue(
				instrumentPreset.get("imgLink"), List.class);

			log.info("  - 사전 설정 악기 수: {}", allInstruments.size());

			// ✅ 2단계: 각 악기별 검색 빈도 조회
			Map<String, Long> searchCounts = getInstrumentSearchCounts(allInstruments);

			// ✅ 3단계: 검색량 순으로 정렬 (많은 순 → 적은 순)
			List<Integer> sortedIndexes = IntStream.range(0, allInstruments.size())
				.boxed()
				.sorted((i1, i2) -> {
					String instrument1 = allInstruments.get(i1);
					String instrument2 = allInstruments.get(i2);
					Long count1 = searchCounts.getOrDefault(instrument1, 0L);
					Long count2 = searchCounts.getOrDefault(instrument2, 0L);

					// 검색량 기준 내림차순 정렬
					return Long.compare(count2, count1);
				})
				.collect(Collectors.toList());

			// ✅ 4단계: 정렬된 순서로 악기와 이미지 재배열
			List<String> sortedInstruments = sortedIndexes.stream()
				.map(allInstruments::get)
				.collect(Collectors.toList());

			List<String> sortedImgLinks = sortedIndexes.stream()
				.map(allImgLinks::get)
				.collect(Collectors.toList());

			// ✅ 5단계: 정렬 결과 로그 출력
			log.info("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, sortedInstruments.size()); i++) {
				String instrument = sortedInstruments.get(i);
				Long count = searchCounts.getOrDefault(instrument, 0L);
				log.info("    {}. {} (검색 {}회)", i + 1, instrument, count);
			}

			return SearchResponse.Instrument.builder()
				.instrument(sortedInstruments)
				.imgLink(sortedImgLinks)
				.build();

		} catch (Exception e) {
			log.error("악기 조회 실패: {}", e.getMessage(), e);
			return getDefaultInstruments();
		}
	}

	/**
	 * 악기별 검색 빈도 조회 wpqkf
	 */
	private Map<String, Long> getInstrumentSearchCounts(List<String> instruments) {
		Map<String, Long> searchCounts = new HashMap<>();

		try {
			log.info("📊 악기별 검색 빈도 조회 시작");

			// ✅ 각 악기별로 검색 로그에서 빈도 조회
			for (String instrument : instruments) {
				try {
					Long count = getSearchCountForKeyword(instrument);
					searchCounts.put(instrument, count);
					if (count > 0) {
						log.info("    {}: {}회", instrument, count);
					}
				} catch (Exception e) {
					log.warn("    {}: 조회 실패", instrument, e);
					searchCounts.put(instrument, 0L);
				}
			}


		} catch (Exception e) {
			log.error("악기 검색 빈도 조회 실패: {}", e.getMessage());
		}

		return searchCounts;
	}

	/**
	 * 특정 키워드의 검색 빈도 조회 - 한글/영어 통합
	 */
	private Long getSearchCountForKeyword(String keyword) {
		try {
			// ✅ 한글-영어 매핑
			List<String> searchKeywords = getEquivalentKeywords(keyword);
			log.info("    검색 키워드들: {}" ,searchKeywords);

			long totalCount = 0L;

			// ✅ 모든 등가 키워드로 검색
			for (String searchKeyword : searchKeywords) {
				var searchRequest = co.elastic.clients.elasticsearch.core.SearchRequest.of(s -> s
					.index("search_logs")
					.size(0)
					.query(q -> q
						.bool(b -> b
								.must(m -> m
									.match(ma -> ma
										.field("query")
										.query(searchKeyword)
									)
								)
							// 시간 필터는 일단 제거 (field 오류 때문에)
						)
					)
				);

				var response = elasticsearchClient.search(searchRequest, Void.class);
				long count = response.hits().total().value();
				if (count > 0) {
					log.info("      '{}': {}회", searchKeyword, count);
					totalCount += count;
				}
			}

			return totalCount;

		} catch (Exception e) {
			log.error("        키워드 '{}' 검색 실패: {}", keyword, e.getMessage(), e);
			return 0L;
		}
	}

	/**
	 * 키워드의 등가 검색어들 반환 (한글, 영어, 별칭 등)
	 */
	private List<String> getEquivalentKeywords(String keyword) {
		Map<String, List<String>> keywordMap = createKeywordMap();

		// 직접 매핑된 경우
		if (keywordMap.containsKey(keyword)) {
			return keywordMap.get(keyword);
		}

		// 매핑에서 찾기 (역방향 검색)
		for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
			if (entry.getValue().contains(keyword)) {
				return entry.getValue();
			}
		}

		// 매핑이 없으면 원본만 반환
		return Arrays.asList(keyword);
	}

	/**
	 * 키워드 매핑 맵 생성
	 */
	private Map<String, List<String>> createKeywordMap() {
		Map<String, List<String>> map = new HashMap<>();

		// ✅ 악기 매핑 (한글, 영어, 별칭)
		map.put("피아노", Arrays.asList("피아노", "piano", "Piano"));
		map.put("바이올린", Arrays.asList("바이올린", "violin", "Violin", "바이얼린"));
		map.put("첼로", Arrays.asList("첼로", "cello", "Cello"));
		map.put("플루트", Arrays.asList("플루트", "flute", "Flute", "플룻"));
		map.put("클라리넷", Arrays.asList("클라리넷", "clarinet", "Clarinet", "클라리넷"));
		map.put("트럼펫", Arrays.asList("트럼펫", "trumpet", "Trumpet", "트램펫"));
		map.put("호른", Arrays.asList("호른", "horn", "Horn", "혼"));
		map.put("트롬본", Arrays.asList("트롬본", "trombone", "Trombone"));
		map.put("튜바", Arrays.asList("튜바", "tuba", "Tuba"));
		map.put("오보에", Arrays.asList("오보에", "oboe", "Oboe"));
		map.put("바순", Arrays.asList("바순", "bassoon", "Bassoon", "파곳"));
		map.put("색소폰", Arrays.asList("색소폰", "saxophone", "Saxophone", "색스폰", "sax"));
		map.put("하프", Arrays.asList("하프", "harp", "Harp"));
		map.put("기타", Arrays.asList("기타", "guitar", "Guitar"));
		map.put("드럼", Arrays.asList("드럼", "drum", "Drum", "북"));
		map.put("심벌즈", Arrays.asList("심벌즈", "cymbals", "Cymbals", "심발"));

		// ✅ 작곡가 매핑 추가 (한글, 영어, 별칭)
		map.put("바흐", Arrays.asList("바흐", "bach", "Bach", "J.S. Bach"));
		map.put("베토벤", Arrays.asList("베토벤", "beethoven", "Beethoven"));
		map.put("모차르트", Arrays.asList("모차르트", "mozart", "Mozart", "W.A. Mozart"));
		map.put("쇼팽", Arrays.asList("쇼팽", "chopin", "Chopin"));
		map.put("브람스", Arrays.asList("브람스", "brahms", "Brahms"));
		map.put("리스트", Arrays.asList("리스트", "liszt", "Liszt"));
		map.put("슈베르트", Arrays.asList("슈베르트", "schubert", "Schubert"));
		map.put("하이든", Arrays.asList("하이든", "haydn", "Haydn"));
		map.put("슈만", Arrays.asList("슈만", "schumann", "Schumann"));
		map.put("드뷔시", Arrays.asList("드뷔시", "debussy", "Debussy"));
		map.put("라벨", Arrays.asList("라벨", "ravel", "Ravel"));
		map.put("차이콥스키", Arrays.asList("차이콥스키", "tchaikovsky", "Tchaikovsky", "차이코프스키"));
		map.put("라흐마니노프", Arrays.asList("라흐마니노프", "rachmaninoff", "Rachmaninoff"));
		map.put("베르디", Arrays.asList("베르디", "verdi", "Verdi"));
		map.put("푸치니", Arrays.asList("푸치니", "puccini", "Puccini"));
		map.put("바그너", Arrays.asList("바그너", "wagner", "Wagner"));
		map.put("윤이상", Arrays.asList("윤이상", "yun isang", "Yun Isang"));

		// 장르 매핑 추가
		map.put("바로크", Arrays.asList("바로크", "baroque", "Baroque"));
		map.put("클래식", Arrays.asList("클래식", "classical", "Classical", "클라식"));
		map.put("낭만주의", Arrays.asList("낭만주의", "romantic", "Romantic", "낭만파"));
		map.put("재즈", Arrays.asList("재즈", "jazz", "Jazz"));
		map.put("오페라", Arrays.asList("오페라", "opera", "Opera"));
		map.put("교향곡", Arrays.asList("교향곡", "symphony", "Symphony"));
		map.put("협주곡", Arrays.asList("협주곡", "concerto", "Concerto"));
		map.put("소나타", Arrays.asList("소나타", "sonata", "Sonata"));

		// ✅ 용어 매핑
		map.put("교향곡", Arrays.asList("교향곡", "symphony", "Symphony"));
		map.put("협주곡", Arrays.asList("협주곡", "concerto", "Concerto"));
		map.put("소나타", Arrays.asList("소나타", "sonata", "Sonata"));
		map.put("오페라", Arrays.asList("오페라", "opera", "Opera"));

		return map;
	}

	/**
	 * 기본 악기 목록 반환
	 */
	private SearchResponse.Instrument getDefaultInstruments() {
		return SearchResponse.Instrument.builder()
			.instrument(Arrays.asList("피아노", "바이올린", "첼로", "플루트", "클라리넷", "트럼펫"))
			.imgLink(Arrays.asList(
				"https://example.com/images/keyboard/piano.png",
				"https://example.com/images/strings/violin.png",
				"https://example.com/images/strings/cello.png",
				"https://example.com/images/woodwind/flute.png",
				"https://example.com/images/woodwind/clarinet.png",
				"https://example.com/images/brass/trumpet.png"
			))
			.build();
	}

	/** 37번 검색결과 - 게시글 + 인기미디어 (DB 직접 검색 추가) */
	public SearchResponse.SearchResultAll searchAll(String query) {
		log.info("🔍 통합 검색 실행: {} ",query );

		UUID currentUserId = getCurrentUserId();
		String userIdString = convertUserIdForLogging(currentUserId);

		try {
			searchLogService.logGeneralSearch(query, userIdString);
		} catch (Exception e) {
			log.warn("검색 로그 저장 실패: {}", e.getMessage());
		}

		try {
			// ✅ 1단계: Elasticsearch에서 게시글 ID 검색
			List<String> postIdStrings = elkSearchRepository.searchPosts(query);
			log.info("  - ELK 검색 결과: {}개", postIdStrings.size());

			List<SearchResponse.PostResult> results;

			if (postIdStrings.isEmpty()) {
				log.info("  - ELK 결과 없음, DB 직접 검색 실행");

				// ✅ 2단계: DB에서 직접 검색 (제목, 내용, 태그)
				List<Post> posts = postRepository.findByTitleContainingOrContentContaining(query);
				log.info("  - DB 직접 검색 결과: {}개",posts.size());

				results = posts.stream()
					.limit(20)
					.map(post -> SearchResponse.PostResult.builder()
						.post(post)
						.user(post.getUser())
						.build())
					.collect(Collectors.toList());
			} else {
				// ELK 결과가 있으면 기존 로직 실행
				List<UUID> postIds = postIdStrings.stream()
					.limit(20)
					.map(UUID::fromString)
					.collect(Collectors.toList());

				results = postRepository.findAllByIdInWithUser(postIds).stream()
					.map(post -> SearchResponse.PostResult.builder()
						.post(post)
						.user(post.getUser())
						.build())
					.collect(Collectors.toList());
			}

			// 인기 미디어 조회
			List<SearchResponse.SearchResultAll.PopularMedia> popularMedia =
				getPopularMediaForSearch(currentUserId);

			log.info("✅ 통합 검색 완료 - 게시글: {}개, 미디어: {}개",results.size(), popularMedia.size() );

			return SearchResponse.SearchResultAll.builder()
				.results(results)
				.popularMedia(popularMedia)
				.build();

		} catch (Exception e) {
			log.error("통합 검색 실패: {}", e.getMessage(), e);
			return getEmptySearchResultAll();
		}
	}

	/** 38번 검색결과 - 프로필 */
	public SearchResponse.SearchProfile searchProfile(String query, String authHeader) {
		log.info("👤 프로필 검색 실행: {}", query );

		// ✅ 현재 사용자 ID 가져오기 (토큰에서 추출)
		UUID currentUserId = authHelper.authHelperAsUUID(authHeader);
		String userIdString = convertUserIdForLogging(currentUserId);

		try {
			searchLogService.logSearch(query, "profile", userIdString);
		} catch (Exception e) {
			log.warn("검색 로그 저장 실패: {}", e.getMessage());
		}

		try {
			// ✅ 1단계: Elasticsearch에서 사용자 검색
			List<String> userIdStrings = elkSearchRepository.searchUsers(query);
			log.info("  - ELK 사용자 검색 결과: {}개", userIdStrings.size());

			List<SearchResponse.SearchProfile.UserProfile> profiles;

			if (userIdStrings.isEmpty()) {
				log.info("  - ELK 결과 없음, DB 직접 검색 실행");

				// ✅ 2단계: DB에서 직접 사용자 검색 (닉네임, 자기소개 포함)
				List<User> users = userRepository.findAll().stream()
					.filter(user -> {
						// 자기 자신 제외
						if (user.getId().equals(currentUserId)) {
							return false;
						}
						if (user.getNickname() != null &&
							user.getNickname().toLowerCase().contains(query.toLowerCase())) {
							return true;
						}
						if (user.getIntro() != null &&
							user.getIntro().toLowerCase().contains(query.toLowerCase())) {
							return true;
						}
						return false;
					})
					.limit(10)
					.collect(Collectors.toList());

				log.info("  - DB 직접 검색 결과: {}개",users.size());

				// ✅ 효율적인 팔로우 상태 확인 - 한 번에 모든 팔로우 관계 조회
				List<UUID> targetUserIds = users.stream()
					.map(User::getId)
					.collect(Collectors.toList());

				Set<UUID> followingUserIds = getFollowingUserIds(currentUserId, targetUserIds);

				profiles = users.stream()
					.map(user -> SearchResponse.SearchProfile.UserProfile.builder()
						.userNickname(user.getNickname())
						.profileUrl(user.getProfileImageUrl())
						.intro(user.getIntro())
						.follow(followingUserIds.contains(user.getId()) ? "T" : "F")  // ✅ 팔로우 상태 확인
						.build())
					.collect(Collectors.toList());

			} else {
				// ✅ 3단계: ELK 결과가 있으면 해당 사용자들 조회
				List<UUID> userIds = userIdStrings.stream()
					.limit(10)
					.map(UUID::fromString)
					.filter(userId -> !userId.equals(currentUserId))  // ✅ 자기 자신 제외
					.collect(Collectors.toList());

				List<User> users = userRepository.findAllByIdIn(userIds);

				// ✅ 효율적인 팔로우 상태 확인 - 한 번에 모든 팔로우 관계 조회
				Set<UUID> followingUserIds = getFollowingUserIds(currentUserId, userIds);

				profiles = users.stream()
					.map(user -> SearchResponse.SearchProfile.UserProfile.builder()
						.userNickname(user.getNickname())
						.profileUrl(user.getProfileImageUrl())
						.intro(user.getIntro())
						.follow(followingUserIds.contains(user.getId()) ? "T" : "F")  // ✅ 팔로우 상태 확인
						.build())
					.collect(Collectors.toList());
			}

			log.info("✅ 프로필 검색 완료: {}개 ", profiles.size());

			return SearchResponse.SearchProfile.builder()
				.user(profiles)
				.build();

		} catch (Exception e) {
			log.error("프로필 검색 실패: {}", e.getMessage());
			return SearchResponse.SearchProfile.builder().user(List.of()).build();
		}
	}

	/**
	 * ✅ 효율적인 팔로우 상태 확인 헬퍼 메서드
	 * 현재 사용자가 팔로우하는 사람들 중에서 대상 사용자 목록에 포함된 사람들만 필터링
	 */
	private Set<UUID> getFollowingUserIds(UUID currentUserId, List<UUID> targetUserIds) {
		try {
			// ✅ 현재 사용자가 팔로우하는 모든 사용자 ID 조회
			List<UUID> allFollowingIds = followRepository.findFolloweeIds(currentUserId);

			// ✅ 대상 사용자 목록과 교집합 구하기
			return allFollowingIds.stream()
				.filter(targetUserIds::contains)
				.collect(Collectors.toSet());

		} catch (Exception e) {
			log.warn("팔로우 상태 조회 실패: {}", e.getMessage());
			return new HashSet<>();
		}
	}
	// List<Post> posts = postRepository.findAllByIdInWithUser(postIds);

	/**
	 * ✅ 현재 사용자 ID 가져오기 (기존 메서드 활용 또는 수정)
	 */
	private UUID getCurrentUserId() {
		// 이 메서드는 이미 있다면 제거하고, authHelper 사용
		try {
			return SecurityContextHolder.getContext().getAuthentication() != null ?
				UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName()) : null;
		} catch (Exception e) {
			return null;
		}
	}

	/** 39번 검색결과 - 피드 */
	public SearchResponse.SearchFeed searchFeed(String query) {
		log.info("📰 피드 검색 실행: {}",query );

		UUID currentUserId = getCurrentUserId();
		String userIdString = convertUserIdForLogging(currentUserId);

		try {
			searchLogService.logSearch(query, "feed", userIdString);
		} catch (Exception e) {
			log.warn("검색 로그 저장 실패: {}", e.getMessage());
		}

		try {
			CompletableFuture<List<Post>> postsFuture = CompletableFuture.supplyAsync(() -> {
				List<String> postIdStrings = elkSearchRepository.searchPosts(query);
				log.info("  - ELK 피드 검색 결과: {}개", postIdStrings.size());

				if (postIdStrings.isEmpty()) {
					log.info("  - ELK 결과 없음, DB 직접 검색 실행");
					return postRepository.findByTitleContainingOrContentContaining(query)
						.stream()
						.limit(50)
						.collect(Collectors.toList());
				} else {
					List<UUID> postIds = postIdStrings.stream()
						.limit(50)
						.map(UUID::fromString)
						.collect(Collectors.toList());
					return postRepository.findAllByIdInWithUser(postIds);
				}
			});

			// 최신순 정렬 병렬 처리
			CompletableFuture<List<SearchResponse.PostResult>> recentFuture = postsFuture.thenApplyAsync(posts ->
				posts.stream()
					.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
					.map(post -> SearchResponse.PostResult.builder()
						.post(post)
						.user(post.getUser())
						.build())
					.collect(Collectors.toList())
			);

			// 인기순 정렬 병렬 처리
			CompletableFuture<List<SearchResponse.PostResult>> popularFuture = postsFuture.thenApplyAsync(posts ->
				posts.stream()
					.sorted((a, b) -> {
						int likesA = a.getLikes() != null ? a.getLikes().size() : 0;
						int likesB = b.getLikes() != null ? b.getLikes().size() : 0;
						return Integer.compare(likesB, likesA);
					})
					.map(post -> SearchResponse.PostResult.builder()
						.post(post)
						.user(post.getUser())
						.build())
					.collect(Collectors.toList())
			);

			// 결과 기다림
			CompletableFuture.allOf(recentFuture, popularFuture).join();

			return SearchResponse.SearchFeed.builder()
				.resultsRecent(recentFuture.get())
				.resultPopular(popularFuture.get())
				.build();

		} catch (Exception e) {
			log.error("검색 피드 조회 실패: {}", e.getMessage());
			return getEmptySearchFeed();
		}
	}


	// ========== 헬퍼 메서드 ==========

	/**
	 * 인기 미디어 조회 - 간단 버전
	 */
	private List<SearchResponse.SearchResultAll.PopularMedia> getPopularMediaForSearch(UUID currentUserId) {
		try {
			log.info("📺 인기 YouTube 미디어 조회 시작");

			List<Post> youtubePosts = postRepository.findAll().stream()
				.filter(post -> post.getMediaUrl() != null &&
					!post.getMediaUrl().trim().isEmpty() &&
					(post.getMediaUrl().toLowerCase().contains("youtube") ||
						post.getMediaUrl().toLowerCase().contains("youtu.be")))
				.sorted((p1, p2) -> {
					int likes1 = p1.getLikes() != null ? p1.getLikes().size() : 0;
					int likes2 = p2.getLikes() != null ? p2.getLikes().size() : 0;
					return Integer.compare(likes2, likes1);
				})
				.limit(10)
				.collect(Collectors.toList());

			log.info("  - YouTube 미디어 {}개", youtubePosts.size() );

			return youtubePosts.stream()
				.map(post -> SearchResponse.SearchResultAll.PopularMedia.builder()
					.userNickname(post.getUser().getNickname())
					.userProfileImgLink(post.getUser().getProfileImageUrl())
					.postID(post.getId().toString())
					.mediaURL(post.getMediaUrl())
					.mediaType("youtube")
					.createdAgo(calculateDaysAgoFromDateTime(post.getCreatedAt())) // ✅ 메서드명 변경
					.build())
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("인기 미디어 조회 실패: {}", e.getMessage());
			return List.of();
		}
	}

	/** UUID를 String으로 안전하게 변환 */
	private String convertUserIdForLogging(UUID userId) {
		return (userId != null) ? userId.toString() : "anonymous";
	}

	/** LocalDateTime 기준 며칠 전인지 계산  */
	private int calculateDaysAgo(LocalDateTime createdAt) {
		if (createdAt == null)
			return 0;
		return (int)ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
	}

	/** 빈 검색 결과 반환 */
	private SearchResponse.SearchResultAll getEmptySearchResultAll() {
		return SearchResponse.SearchResultAll.builder()
			.results(List.of())
			.popularMedia(List.of())
			.build();
	}

	/** 빈 피드 검색 결과 반환 */
	private SearchResponse.SearchFeed getEmptySearchFeed() {
		return SearchResponse.SearchFeed.builder()
			.resultsRecent(List.of())
			.resultPopular(List.of())
			.build();
	}

	/** LocalDateTime 기준 며칠 전인지 계산 (SearchResponse용) ✅ */
	private int calculateDaysAgoFromDateTime(LocalDateTime createdAt) {
		if (createdAt == null)
			return 0;
		return (int)ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
	}

	/** LocalDateTime 기준 시간 계산 (다른 용도) ✅ */
	private int calculateHoursAgoFromDateTime(LocalDateTime createdAt) {
		if (createdAt == null)
			return 0;
		return (int)ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
	}


	/** 40번 자동완성 검색어 조회 (더 유연한 검색) */
	public SearchResponse.Autocomplete getAutocomplete(String query) {
		log.info("🔍 Elasticsearch 자동완성 검색: {}", query);

		try {
			// ✅ 1단계: Elasticsearch에서 유연한 검색
			List<String> elasticSuggestions = getElasticSuggestions(query);

			// ✅ 2단계: 사전 정의된 키워드에서 유연한 검색
			List<String> presetSuggestions = getPresetSuggestions(query);

			// ✅ 3단계: 사용자 닉네임에서 유연한 검색
			List<String> userSuggestions = getUserSuggestionsFromDb(query);

			// ✅ 4단계: 모든 결과 합치고 정렬
			List<String> allSuggestions = new ArrayList<>();
			allSuggestions.addAll(elasticSuggestions);
			allSuggestions.addAll(presetSuggestions);
			allSuggestions.addAll(userSuggestions);

			// 중복 제거 및 스마트 정렬
			List<String> finalSuggestions = allSuggestions.stream()
				.distinct()
				.filter(s -> isMatch(s, query))  // ✅ 유연한 매칭 로직
				.sorted((a, b) -> compareRelevance(a, b, query))  // ✅ 관련성 기반 정렬
				.limit(10)
				.collect(Collectors.toList());

			log.info("✅ 자동완성 결과: {}개", finalSuggestions.size() );
			finalSuggestions.forEach(s -> log.info("  - {}", s));

			// 비동기 키워드 업데이트
			updateAutocompleteKeywords(query);

			return SearchResponse.Autocomplete.builder()
				.suggestions(finalSuggestions)
				.build();

		} catch (Exception e) {
			log.error("Elasticsearch 자동완성 실패: {}", e.getMessage());
			return getFallbackAutocomplete(query);
		}
	}

	/**
	 * ✅ Elasticsearch에서 유연한 검색
	 */
	private List<String> getElasticSuggestions(String query) {
		try {
			var searchRequest = co.elastic.clients.elasticsearch.core.SearchRequest.of(s -> s
				.index("autocomplete-keywords")
				.size(20)  // 더 많이 가져와서 필터링
				.query(q -> q
					.bool(b -> b
						.should(sh -> sh
							// 정확히 시작하는 것 (최고 점수)
							.prefix(p -> p
								.field("keyword")
								.value(query.toLowerCase())
								.boost(5.0f)
							)
						)
						.should(sh -> sh
							// 부분 일치 (높은 점수)
							.wildcard(w -> w
								.field("keyword")
								.value("*" + query.toLowerCase() + "*")
								.boost(3.0f)
							)
						)
						.should(sh -> sh
							// 퍼지 매칭 (오타 허용)
							.fuzzy(f -> f
								.field("keyword")
								.value(query.toLowerCase())
								.fuzziness("AUTO")
								.boost(1.0f)
							)
						)
						.minimumShouldMatch("1")
					)
				)
			);

			var response = elasticsearchClient.search(searchRequest, AutocompleteKeyword.class);

			return response.hits().hits().stream()
				.map(hit -> hit.source().getKeyword())
				.distinct()
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.warn("Elastic 검색 실패: {}", e.getMessage());
			return List.of();
		}
	}

	/**
	 * ✅ 사전 정의된 키워드에서 유연한 검색 (확장 버전)
	 */
	private List<String> getPresetSuggestions(String query) {
		List<String> presetKeywords = Arrays.asList(
			// 작곡가 (클래식)
			"바흐", "베토벤", "모차르트", "쇼팽", "브람스", "리스트", "슈베르트", "하이든",
			"슈만", "드뷔시", "라벨", "차이콥스키", "라흐마니노프", "베르디", "푸치니", "바그너",
			"멘델스존", "그리그", "드보르자크", "시벨리우스", "스트라빈스키", "프로코피예프",
			"비발디", "헨델", "텔레만", "파헬벨", "알비노니", "코렐리", "퍼셀",
			"로시니", "도니체티", "벨리니", "마스카니", "레온카발로", "폰키엘리",
			"말러", "브루크너", "생상스", "프랑크", "마스네", "비제", "구노",
			"베버", "글루크", "루소", "레스피기", "카탈라니", "보로딘", "무소르그스키",
			"림스키코르사코프", "글라주노프", "스크리아빈", "파가니니", "비외탕", "생상스",

			// 현대/재즈 작곡가
			"거슈윈", "번스타인", "코플런드", "아이브스", "케이지", "글래스", "라이히",
			"앤드류 로이드 웨버", "스티븐 손드하임", "미셸 르그랑", "엔니오 모리코네",

			// 한국 작곡가/음악가
			"윤이상", "나운영", "김동진", "김순남", "현제명", "홍난파", "채동선",
			"안익태", "김성태", "이건용", "박재훈", "진은숙", "황병기",

			// 피아니스트 (국외)
			"글렌 굴드", "아르투르 루빈스타인", "블라디미르 호로비츠", "스비아토슬라브 리히터",
			"마르타 아르헤리치", "다니엘 바렌보임", "알프레드 브렌델", "클라우디오 아라우",
			"머레이 페라이어", "라두 루푸", "안드라스 시프", "크리스티안 치메르만",
			"랑랑", "유자 왕", "키신", "플레트네프", "마우리치오 폴리니", "미첼란젤리",

			// 피아니스트 (한국)
			"조성진", "임윤찬", "손열음", "김선욱", "이루마", "신지호", "김정원",
			"문지영", "김태형", "백건우", "강충모", "피아니스트",

			// 바이올리니스트
			"이츠하크 펄만", "야샤 하이페츠", "다비드 오이스트라흐", "요헤디 메뉴힌",
			"안네 소피 무터", "힐러리 한", "사라 장", "정경화", "강동석", "김봄소리",
			"양인모", "클라라 주미 강", "바이올리니스트",

			// 첼리스트
			"요요마", "미샤 마이스키", "자클린 뒤 프레", "파블로 카잘스", "므스티슬라프 로스트로포비치",
			"송영훈", "장한나", "한재민", "첼리스트",

			// 성악가
			"루치아노 파바로티", "플라시도 도밍고", "호세 카레라스", "마리아 칼라스",
			"조수미", "홍혜경", "신영옥", "김우경", "성악가", "소프라노", "테너", "바리톤", "베이스",

			// 지휘자
			"헤르베르트 폰 카라얀", "레오나르드 번스타인", "정명훈", "구스타프 말러",
			"토스카니니", "푸르트벵글러", "클라우디오 아바도", "주빈 메타", "정명훈",
			"금난새", "조성진", "지휘자", "마에스트로",

			// 악기
			"피아노", "바이올린", "비올라", "첼로", "콘트라베이스", "더블베이스",
			"플루트", "피콜로", "오보에", "클라리넷", "바순", "색소폰",
			"트럼펫", "호른", "트롬본", "튜바", "하프", "팀파니", "실로폰",
			"오르간", "하프시코드", "아코디언", "밴도네온", "첼레스타",
			"드럼", "타악기", "심벌즈", "마림바", "비브라폰",

			// 장르/형식
			"클래식", "클래식 음악", "바로크", "고전주의", "낭만주의", "인상주의", "현대음악",
			"교향곡", "협주곡", "소나타", "실내악", "현악 4중주", "피아노 트리오",
			"오페라", "오페레타", "뮤지컬", "발레", "왈츠", "폴로네즈", "마주르카",
			"녹턴", "에튀드", "즉흥곡", "전주곡", "푸가", "인벤션", "파르티타",
			"재즈", "블루스", "스윙", "비밥", "쿨재즈", "퓨전", "라틴재즈",
			"팝", "록", "힙합", "R&B", "컨트리", "포크", "월드뮤직",

			// 유명 작품
			"운명 교향곡", "미완성 교향곡", "놀라운 교향곡", "합창 교향곡",
			"월광 소나타", "비창 소나타", "황제 협주곡", "사계", "아이네 클라이네 나흐트무지크",
			"카르멘", "라보엠", "투란도트", "마탄의 사수", "피가로의 결혼",
			"백조의 호수", "호두까기 인형", "잠자는 숲속의 미녀", "지젤",
			"볼레로", "랩소디 인 블루", "신세계 교향곡", "마법피리",

			// 음악 용어
			"알레그로", "안단테", "아다지오", "라르고", "프레스토", "안다티노",
			"포르테", "피아노", "크레센도", "디미누엔도", "스타카토", "레가토",
			"아르페지오", "글리산도", "트릴", "모르던트", "아포지아투라",
			"장조", "단조", "도미넌트", "서브도미넌트", "토닉", "세븐스",

			// 음악원/학교
			"줄리어드", "커티스", "왕립음대", "파리 음악원", "빈 음악원",
			"한국예술종합학교", "서울대 음대", "연세대 음대", "이화여대 음대",

			// 콩쿠르/상
			"쇼팽 콩쿠르", "차이콥스키 콩쿠르", "퀸 엘리자베스 콩쿠르", "반 클라이번 콩쿠르",
			"롱티보 콩쿠르", "부조니 콩쿠르", "그라미상", "에코상", "클래식브릿상",

			// 오케스트라/앙상블
			"베를린 필하모닉", "빈 필하모닉", "런던 심포니", "뉴욕 필하모닉",
			"시카고 심포니", "보스턴 심포니", "로얄 콘체르트헤바우",
			"KBS 교향악단", "서울시향", "코리안 심포니", "프라임 필하모닉",

			// 오페라하우스/콘서트홀
			"빈 슈타츠오퍼", "메트로폴리탄 오페라", "라 스칼라", "코벤트 가든",
			"카네기홀", "링컨센터", "베를린 콘체르트하우스",
			"예술의전당", "롯데콘서트홀", "세종문화회관",

			// 음반사/레이블
			"도이치 그라모폰", "EMI", "소니 클래시컬", "데카", "필립스",
			"노낙스", "하이페리온", "샹도스", "BIS"
		);

		return presetKeywords.stream()
			.filter(keyword -> isMatch(keyword, query))
			.limit(10)
			.collect(Collectors.toList());
	}

	/**
	 * ✅ 유연한 매칭 로직 (핵심!)
	 */
	private boolean isMatch(String keyword, String query) {
		if (keyword == null || query == null) return false;

		String cleanQuery = cleanText(query);
		String cleanKeyword = cleanText(keyword);

		// 1. 정확한 포함 관계
		if (cleanKeyword.contains(cleanQuery)) {
			return true;
		}

		// 2. 초성 검색 (ㅂ → 베토벤, 바흐 등)
		if (isInitialConsonant(cleanQuery)) {
			return matchesInitialConsonant(cleanKeyword, cleanQuery);
		}

		// 3. 부분 문자 매칭 (피아 → 피아노, 피아니스트)
		if (cleanQuery.length() >= 1) {
			return containsAllCharacters(cleanKeyword, cleanQuery);
		}

		return false;
	}

	/**
	 * ✅ 텍스트 정리 (특수문자, 공백 제거)
	 */
	private String cleanText(String text) {
		return text.toLowerCase()
			.replaceAll("[\\\\\\n\\r\\t\\s]", "")  // 백슬래시, 공백, 개행 제거
			.replaceAll("[^가-힣a-z0-9]", "");     // 한글, 영문, 숫자만 남기기
	}

	/**
	 * ✅ 초성인지 확인
	 */
	private boolean isInitialConsonant(String text) {
		return text.matches("[ㄱ-ㅎ]+");
	}

	/**
	 * ✅ 초성 매칭 검사
	 */
	private boolean matchesInitialConsonant(String keyword, String consonants) {
		String[] initialConsonants = {
			"ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
			"ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
		};

		StringBuilder keywordInitials = new StringBuilder();

		for (char c : keyword.toCharArray()) {
			if (c >= '가' && c <= '힣') {
				int unicode = c - '가';
				int initialIndex = unicode / (21 * 28);
				if (initialIndex < initialConsonants.length) {
					keywordInitials.append(initialConsonants[initialIndex]);
				}
			}
		}

		return keywordInitials.toString().contains(consonants);
	}

	/**
	 * ✅ 모든 문자가 포함되어 있는지 확인
	 */
	private boolean containsAllCharacters(String keyword, String query) {
		for (char c : query.toCharArray()) {
			if (!keyword.contains(String.valueOf(c))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * ✅ 관련성 기반 정렬
	 */
	private int compareRelevance(String a, String b, String query) {
		String cleanQuery = cleanText(query);

		// 1. 정확히 시작하는 것이 우선
		boolean aStarts = cleanText(a).startsWith(cleanQuery);
		boolean bStarts = cleanText(b).startsWith(cleanQuery);

		if (aStarts && !bStarts) return -1;
		if (!aStarts && bStarts) return 1;

		// 2. 짧은 것이 우선 (더 정확한 매칭)
		int lengthCompare = Integer.compare(a.length(), b.length());
		if (lengthCompare != 0) return lengthCompare;

		// 3. 알파벳 순서
		return a.compareTo(b);
	}



	/**
	 * ✅ 사용자 닉네임에서 자동완성 추가
	 */
	private List<String> getUserSuggestionsFromDb(String query) {
		try {
			return userRepository.findAll().stream()
				.map(User::getNickname)
				.filter(nickname -> nickname != null &&
					nickname.toLowerCase().contains(query.toLowerCase()))
				.distinct()
				.limit(3)
				.collect(Collectors.toList());
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * ✅ 검색된 키워드를 자동완성 인덱스에 추가 (비동기)
	 */
	@Async
	protected void updateAutocompleteKeywords(String query) {
		try {
			// 이미 존재하는지 확인
			var searchRequest = co.elastic.clients.elasticsearch.core.SearchRequest.of(s -> s
				.index("autocomplete-keywords")
				.query(q -> q
					.term(t -> t
						.field("keyword.keyword")
						.value(query)
					)
				)
				.size(1)
			);

			var existingResponse = elasticsearchClient.search(searchRequest, AutocompleteKeyword.class);

			if (existingResponse.hits().total().value() == 0) {
				// 새로운 키워드 추가
				AutocompleteKeyword newKeyword = new AutocompleteKeyword(query, "user_search", 10);

				elasticsearchClient.index(IndexRequest.of(i -> i
					.index("autocomplete-keywords")
					.document(newKeyword)
				));

				log.info("🆕 새로운 자동완성 키워드 추가: {}",query);
			}

		} catch (Exception e) {
			log.warn("자동완성 키워드 업데이트 실패: {}", e.getMessage());
		}
	}

	/**
	 * ✅ Elasticsearch 실패시 Fallback
	 */
	private SearchResponse.Autocomplete getFallbackAutocomplete(String query) {
		List<String> fallbackKeywords = Arrays.asList(
			"바흐", "베토벤", "모차르트", "쇼팽", "브람스", "리스트", "피아노", "바이올린", "클래식"
		);

		List<String> suggestions = fallbackKeywords.stream()
			.filter(keyword -> keyword.toLowerCase().contains(query.toLowerCase()))
			.limit(5)
			.collect(Collectors.toList());

		return SearchResponse.Autocomplete.builder()
			.suggestions(suggestions)
			.build();
	}

}
