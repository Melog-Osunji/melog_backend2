package com.osunji.melog.search.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osunji.melog.review.entity.Post;
import com.osunji.melog.review.entity.PostComment;
import com.osunji.melog.user.domain.User;
import com.osunji.melog.user.UserRepository;
import com.osunji.melog.review.repository.PostRepository;
import com.osunji.melog.review.repository.CommentRepository;
import com.osunji.melog.elk.repository.ELKSearchRepository;
import com.osunji.melog.elk.service.SearchLogService;
import com.osunji.melog.search.preset.SearchPresetLoader;
import com.osunji.melog.search.dto.response.SearchResponse;

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
	private final CommentRepository commentRepository;
	private final SearchLogService searchLogService;
	private final SearchPresetLoader searchPresetLoader;
	private final ElasticsearchClient elasticsearchClient;

	/** 31번 통합 검색 데이터 조회 - /api/search/all */
	public SearchResponse.AllSearch getAllSearchData() {
		try {
			System.out.println("🔍 통합 검색 데이터 조회 시작");

			// ✅ 실제 ELK에서 인기 검색어 20개 조회 (최근 7일)
			List<String> livePopularSearch = getActualPopularSearchTerms();
			System.out.println("  - ELK에서 조회된 인기 검색어 수: " + livePopularSearch.size());

			// 추천 키워드 6개
			List<String> recommendKeywords = Arrays.asList(
				"베토벤", "모차르트", "쇼팽", "바흐", "브람스", "리스트"
			);

			// 현재 시간
			String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));
			System.out.println("✅ 통합 검색 데이터 조회 완료 - 현재 시간: " + nowTime);

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
			System.out.println("📊 실제 인기 검색어 집계 시작");
			List<String> popularTerms = elkSearchRepository.getPopularSearchTerms();
			System.out.println("🔍 ELK에서 받은 데이터: " + popularTerms.size() + "개");
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
			System.out.println("🎼 인기 작곡가 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode composerPreset = searchPresetLoader.composer();
			if (composerPreset == null || !composerPreset.has("name") || !composerPreset.has("imgLink")) {
				System.out.println("  - 사전 설정 파일 없음, 기본값 사용");
				return getDefaultComposers();
			}

			List<String> allComposers = new ObjectMapper().convertValue(
				composerPreset.get("name"), List.class);
			List<String> allImgLinks = new ObjectMapper().convertValue(
				composerPreset.get("imgLink"), List.class);

			System.out.println("  - 사전 설정 작곡가 수: " + allComposers.size());

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
			System.out.println("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, sortedComposers.size()); i++) {
				String composer = sortedComposers.get(i);
				Long count = searchCounts.getOrDefault(composer, 0L);
				System.out.println("    " + (i+1) + ". " + composer + " (검색 " + count + "회)");
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
			System.out.println("📊 작곡가별 검색 빈도 조회 시작");

			for (String composer : composers) {
				try {
					Long count = getSearchCountForKeyword(composer);
					searchCounts.put(composer, count);
					if (count > 0) {
						System.out.println("    " + composer + ": " + count + "회");
					}
				} catch (Exception e) {
					System.out.println("    " + composer + ": 조회 실패");
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
			System.out.println("🎹 인기 연주가 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode playerPreset = searchPresetLoader.player();
			if (playerPreset == null || !playerPreset.isArray()) {
				System.out.println("  - 사전 설정 파일 없음, ELK 조회");
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

			System.out.println("  - 사전 설정 연주가 수: " + allPlayers.size());

			// ✅ 2단계: 각 연주가별 검색 빈도 조회 후 정렬
			allPlayers.sort((p1, p2) -> {
				Long count1 = getSearchCountForKeyword(p1.getName());
				Long count2 = getSearchCountForKeyword(p2.getName());
				return Long.compare(count2, count1); // 내림차순
			});

			// ✅ 3단계: 정렬 결과 로그 출력
			System.out.println("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, allPlayers.size()); i++) {
				SearchResponse.Player player = allPlayers.get(i);
				Long count = getSearchCountForKeyword(player.getName());
				System.out.println("    " + (i+1) + ". " + player.getName() + " (검색 " + count + "회)");
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
			System.out.println("🎵 장르 데이터 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode genrePreset = searchPresetLoader.genre();
			if (genrePreset == null || !genrePreset.isArray()) {
				System.out.println("  - 사전 설정 파일 없음, 기본값 사용");
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

			System.out.println("  - 사전 설정 장르 수: " + allGenres.size());

			// ✅ 2단계: 각 장르별 검색 빈도 조회 후 정렬
			allGenres.sort((g1, g2) -> {
				Long count1 = getSearchCountForKeyword(g1.getGenre());
				Long count2 = getSearchCountForKeyword(g2.getGenre());
				return Long.compare(count2, count1); // 내림차순
			});

			// ✅ 3단계: 정렬 결과 로그 출력
			System.out.println("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, allGenres.size()); i++) {
				SearchResponse.Genre genre = allGenres.get(i);
				Long count = getSearchCountForKeyword(genre.getGenre());
				System.out.println("    " + (i+1) + ". " + genre.getGenre() + " (검색 " + count + "회)");
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
			System.out.println("⏰ 시대 데이터 조회 시작");

			// ✅ 1순위: 사전 설정값 사용
			JsonNode periodPreset = searchPresetLoader.period();
			if (periodPreset != null && periodPreset.has("era")) {
				System.out.println("  - 사전 설정 시대 데이터 사용");

				List<String> eras = new ObjectMapper().convertValue(
					periodPreset.get("era"), List.class);

				System.out.println("  - 사전 설정 시대 수: " + eras.size());

				return SearchResponse.Period.builder()
					.era(eras)
					.build();
			}

			// ✅ 2순위: Elasticsearch에서 인기 시대 조회
			List<String> popularPeriods = elkSearchRepository.getPopularPeriods();
			if (!popularPeriods.isEmpty()) {
				System.out.println("  - ELK에서 조회된 시대 수: " + popularPeriods.size());
				return SearchResponse.Period.builder()
					.era(popularPeriods)
					.build();
			}

		} catch (Exception e) {
			log.error("시대 조회 실패: {}", e.getMessage());
		}

		// ✅ 3순위: 기본값 반환
		System.out.println("  - 기본값 시대 반환");
		return SearchResponse.Period.builder()
			.era(Arrays.asList("바로크", "고전주의", "낭만주의", "근현대", "현대"))
			.build();
	}

	/** 36번 인기 악기 조회 - /api/search/instrument */
	public SearchResponse.Instrument getInstruments() {
		try {
			System.out.println("🎺 인기 악기 조회 시작 (검색량 순 정렬)");

			// ✅ 1단계: 사전 설정값 가져오기
			JsonNode instrumentPreset = searchPresetLoader.instrument();
			if (instrumentPreset == null || !instrumentPreset.has("instrument") || !instrumentPreset.has("imgLink")) {
				System.out.println("  - 사전 설정 파일 없음, 기본값 사용");
				return getDefaultInstruments();
			}

			List<String> allInstruments = new ObjectMapper().convertValue(
				instrumentPreset.get("instrument"), List.class);
			List<String> allImgLinks = new ObjectMapper().convertValue(
				instrumentPreset.get("imgLink"), List.class);

			System.out.println("  - 사전 설정 악기 수: " + allInstruments.size());

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
			System.out.println("  - 검색량 기준 정렬 완료:");
			for (int i = 0; i < Math.min(10, sortedInstruments.size()); i++) {
				String instrument = sortedInstruments.get(i);
				Long count = searchCounts.getOrDefault(instrument, 0L);
				System.out.println("    " + (i+1) + ". " + instrument + " (검색 " + count + "회)");
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
			System.out.println("📊 악기별 검색 빈도 조회 시작");

			// ✅ 각 악기별로 검색 로그에서 빈도 조회
			for (String instrument : instruments) {
				try {
					Long count = getSearchCountForKeyword(instrument);
					searchCounts.put(instrument, count);
					if (count > 0) {
						System.out.println("    " + instrument + ": " + count + "회");
					}
				} catch (Exception e) {
					System.out.println("    " + instrument + ": 조회 실패");
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
			System.out.println("    검색 키워드들: " + searchKeywords);

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
					System.out.println("      '" + searchKeyword + "': " + count + "회");
					totalCount += count;
				}
			}

			return totalCount;

		} catch (Exception e) {
			System.out.println("        키워드 '" + keyword + "' 검색 실패: " + e.getMessage());
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
		System.out.println("🔍 통합 검색 실행: '" + query + "'");

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
			System.out.println("  - ELK 검색 결과: " + postIdStrings.size() + "개");

			List<SearchResponse.PostResult> results;

			if (postIdStrings.isEmpty()) {
				System.out.println("  - ELK 결과 없음, DB 직접 검색 실행");

				// ✅ 2단계: DB에서 직접 검색 (제목, 내용, 태그)
				List<Post> posts = postRepository.findByTitleContainingOrContentContaining(query);
				System.out.println("  - DB 직접 검색 결과: " + posts.size() + "개");

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

				results = postRepository.findAllByIdIn(postIds).stream()
					.map(post -> SearchResponse.PostResult.builder()
						.post(post)
						.user(post.getUser())
						.build())
					.collect(Collectors.toList());
			}

			// 인기 미디어 조회
			List<SearchResponse.SearchResultAll.PopularMedia> popularMedia =
				getPopularMediaForSearch(currentUserId);

			System.out.println("✅ 통합 검색 완료 - 게시글: " + results.size() + "개, 미디어: " + popularMedia.size() + "개");

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
	public SearchResponse.SearchProfile searchProfile(String query, String authHeader) {  // ✅ 토큰 매개변수 추가
		System.out.println("👤 프로필 검색 실행: '" + query + "'");

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
			System.out.println("  - ELK 사용자 검색 결과: " + userIdStrings.size() + "개");

			List<SearchResponse.SearchProfile.UserProfile> profiles;

			if (userIdStrings.isEmpty()) {
				System.out.println("  - ELK 결과 없음, DB 직접 검색 실행");

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

				System.out.println("  - DB 직접 검색 결과: " + users.size() + "개");

				profiles = users.stream()
					.map(user -> SearchResponse.SearchProfile.UserProfile.builder()
						.userNickname(user.getNickname())
						.profileUrl(user.getProfileImageUrl())
						.intro(user.getIntro())
						.follow(checkFollowStatus(currentUserId, user.getId()))  // ✅ 팔로우 상태 확인
						.build())
					.collect(Collectors.toList());
			} else {
				// ✅ 3단계: ELK 결과가 있으면 해당 사용자들 조회
				List<UUID> userIds = userIdStrings.stream()
					.limit(10)
					.map(UUID::fromString)
					.filter(userId -> !userId.equals(currentUserId))  // ✅ 자기 자신 제외
					.collect(Collectors.toList());

				profiles = userRepository.findAllByIdIn(userIds)
					.stream()
					.map(user -> SearchResponse.SearchProfile.UserProfile.builder()
						.userNickname(user.getNickname())
						.profileUrl(user.getProfileImageUrl())
						.intro(user.getIntro())
						.follow(checkFollowStatus(currentUserId, user.getId()))  // ✅ 팔로우 상태 확인
						.build())
					.collect(Collectors.toList());
			}

			System.out.println("✅ 프로필 검색 완료: " + profiles.size() + "개");

			return SearchResponse.SearchProfile.builder()
				.user(profiles)
				.build();

		} catch (Exception e) {
			log.error("프로필 검색 실패: {}", e.getMessage());
			return SearchResponse.SearchProfile.builder().user(List.of()).build();
		}
	}

	/**
	 * ✅ 팔로우 상태 확인 메서드 추가
	 */
	private String checkFollowStatus(UUID currentUserId, UUID targetUserId) {
		try {
			// 현재 사용자가 대상 사용자를 팔로우하고 있는지 확인
			boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
			return isFollowing ? "T" : "F";
		} catch (Exception e) {
			log.warn("팔로우 상태 확인 실패: {}", e.getMessage());
			return "F";  // 오류 시 기본값
		}
	}

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
		System.out.println("📰 피드 검색 실행: '" + query + "'");

		UUID currentUserId = getCurrentUserId();
		String userIdString = convertUserIdForLogging(currentUserId);

		try {
			searchLogService.logSearch(query, "feed", userIdString);
		} catch (Exception e) {
			log.warn("검색 로그 저장 실패: {}", e.getMessage());
		}

		try {
			// ✅ 1단계: Elasticsearch에서 게시글 ID 검색
			List<String> postIdStrings = elkSearchRepository.searchPosts(query);
			System.out.println("  - ELK 피드 검색 결과: " + postIdStrings.size() + "개");

			List<Post> posts;

			if (postIdStrings.isEmpty()) {
				// ✅ ELK 결과 없으면 DB 직접 검색
				System.out.println("  - ELK 결과 없음, DB 직접 검색 실행");
				posts = postRepository.findByTitleContainingOrContentContaining(query).stream()
					.limit(50)
					.collect(Collectors.toList());
				System.out.println("  - DB 직접 검색 결과: " + posts.size() + "개");

				if (posts.isEmpty()) {
					return getEmptySearchFeed();
				}
			} else {
				List<UUID> postIds = postIdStrings.stream()
					.limit(50)
					.map(UUID::fromString)
					.collect(Collectors.toList());
				posts = postRepository.findAllByIdIn(postIds);
			}

			// ✅ 최신순 정렬 - Entity 기반 (기존과 동일)
			List<SearchResponse.PostResult> resultsRecent = posts.stream()
				.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
				.map(post -> SearchResponse.PostResult.builder()
					.post(post)
					.user(post.getUser())
					.build())
				.collect(Collectors.toList());

			// ✅ 인기순 정렬 - likes.size() 기준으로 수정
			List<SearchResponse.PostResult> resultsPopular = posts.stream()
				.sorted((a, b) -> {
					int likesA = a.getLikes() != null ? a.getLikes().size() : 0;
					int likesB = b.getLikes() != null ? b.getLikes().size() : 0;
					return Integer.compare(likesB, likesA); // 좋아요 내림차순
				})
				.map(post -> SearchResponse.PostResult.builder()
					.post(post)
					.user(post.getUser())
					.build())
				.collect(Collectors.toList());

			System.out.println("✅ 피드 검색 완료 - 최신: " + resultsRecent.size() + "개, 인기: " + resultsPopular.size() + "개");

			return SearchResponse.SearchFeed.builder()
				.resultsRecent(resultsRecent)
				.resultPopular(resultsPopular)
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
			System.out.println("📺 인기 YouTube 미디어 조회 시작");

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

			System.out.println("  - YouTube 미디어 " + youtubePosts.size() + "개 발견");

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



	/** 현재 사용자 ID 가져오기 */
	private UUID getCurrentUserId() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated()) {
				return null;
			}

			Object principal = auth.getPrincipal();
			if (principal instanceof UserDetails) {
				return UUID.fromString(((UserDetails) principal).getUsername());
			} else if (principal instanceof String) {
				return UUID.fromString((String) principal);
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("현재 사용자 ID 조회 실패: {}", e.getMessage());
			return null;
		}
	}

	/** UUID를 String으로 안전하게 변환 */
	private String convertUserIdForLogging(UUID userId) {
		return (userId != null) ? userId.toString() : "anonymous";
	}

	/** 팔로우 상태 확인 */
	private String checkFollowStatus(UUID currentUserId, UUID targetUserId) {
		if (currentUserId == null || targetUserId == null) {
			return "N";
		}

		// TODO: 팔로우 관계 확인 로직 구현
		return "N"; // 임시로 N 반환
	}

	/** LocalDateTime 기준 며칠 전인지 계산  */
	private int calculateDaysAgo(LocalDateTime createdAt) {
		if (createdAt == null) return 0;
		return (int) ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
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
		if (createdAt == null) return 0;
		return (int) ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDateTime.now().toLocalDate());
	}

	/** LocalDateTime 기준 시간 계산 (다른 용도) ✅ */
	private int calculateHoursAgoFromDateTime(LocalDateTime createdAt) {
		if (createdAt == null) return 0;
		return (int) ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
	}


	/**
	 * ✅ 팔로우 상태 확인 메서드 추가
	 */
	private String checkFollowStatus(UUID currentUserId, UUID targetUserId) {
		try {
			// 현재 사용자가 대상 사용자를 팔로우하고 있는지 확인
			boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
			return isFollowing ? "T" : "F";
		} catch (Exception e) {
			log.warn("팔로우 상태 확인 실패: {}", e.getMessage());
			return "F";  // 오류 시 기본값
		}
	}

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

}
