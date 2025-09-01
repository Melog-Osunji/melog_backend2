package com.osunji.melog.search.repository;
import com.osunji.melog.review.service.PostService;
import com.osunji.melog.elk.repository.ELKSearchRepository;
import com.osunji.melog.elk.service.SearchLogService;
import com.osunji.melog.search.dto.response.SearchResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor

public class SearchRepository {
	// ELK Repository 주입
	private final ELKSearchRepository elkSearchRepository;
	private final SearchLogService searchLogService;
	private final PostService postService;
	//private final UserService userService;

	/** 31번 통합 검색 데이터 조회	 */
	public SearchResponse.AllSearch getAllSearchData() {
		// TODO: 추천키워드조회(추천시스템개발이후)

		//Elasticsearch에서 인기 검색어 조회
		List<String> livePopularSearch = elkSearchRepository.getPopularSearchTerms();

		// 추천키워드 임시 고정값 (추후 추천키워드 리턴개발후넣기)
		List<String> recommendKeywords = Arrays.asList(
			"베토벤", "모차르트", "쇼팽", "바흐", "브람스", "리스트"
		);

		String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

		return SearchResponse.AllSearch.builder()
			.recommendKeyword(recommendKeywords)
			.livePopularSearch(livePopularSearch)
			.nowTime(nowTime)
			.build();
	}

	/**
	 * 32번 인기 작곡가 조회
	 */
	public List<SearchResponse.Composer> getPopularComposers() {
		// TODO: 엘라스틱 결과 작곡가 이름과 이미지 링크 매핑 로직 추가

		//Elasticsearch에서 인기 작곡가 조회
		List<String> popularComposers = elkSearchRepository.getPopularComposers();

		// 이미지 링크 매핑
		List<String> imgLinks = popularComposers.stream()
			.map(composer -> "/resources/static/img/composer/" + composer.toLowerCase() + ".jpg")
			.collect(Collectors.toList());

		return Arrays.asList(
			SearchResponse.Composer.builder()
				.name(popularComposers)
				.imgLink(imgLinks)
				.build()
		);
	}

	/**
	 * 33번 인기 연주가 + 관련 키워드 조회
	 */
	public List<SearchResponse.Player> getPopularPlayers() {
		// TODO:
		try {
			// Elasticsearch에서 인기 연주가 조회
			List<String> popularPlayers = elkSearchRepository.getPopularPlayers();

			return popularPlayers.stream()
				.map(playerName -> {
					// 각 연주가별 관련 키워드 조회
					List<String> keywords = elkSearchRepository.getPlayerRelatedKeywords(playerName);

					return SearchResponse.Player.builder()
						.name(playerName)
						.keyword(keywords)
						.build();
				})
				.collect(Collectors.toList());

		} catch (Exception e) {
			// 실패 시 기본값 반환
			return Arrays.asList(
				SearchResponse.Player.builder()
					.name("랑랑")
					.keyword(Arrays.asList("피아노", "협주곡", "중국", "베토벤"))
					.build(),
				SearchResponse.Player.builder()
					.name("조성진")
					.keyword(Arrays.asList("쇼팽", "피아노", "콩쿠르", "한국"))
					.build(),
				SearchResponse.Player.builder()
					.name("정명훈")
					.keyword(Arrays.asList("지휘", "오케스트라", "서울시향", "프랑스"))
					.build()
			);
		}
	}
	/**
	 * 34번 장르 + 관련 키워드 조회 (사전 설정)
	 */
	public List<SearchResponse.Genre> getGenres() {
		// TODO: 사전 설정된 장르 데이터 + Elasticsearch에서 관련 키워드 집계

		// 임시 데이터
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

	/**
	 * 35번 인기 시대 조회
	 */
	public SearchResponse.Period getPeriods() {
		// TODO:
		try {
			// Elasticsearch에서 인기 시대 조회
			List<String> popularPeriods = elkSearchRepository.getPopularPeriods();

			return SearchResponse.Period.builder()
				.era(popularPeriods)
				.build();

		} catch (Exception e) {
			// 실패 시 기본값 반환
			return SearchResponse.Period.builder()
				.era(Arrays.asList("바로크", "고전주의", "낭만주의", "근현대", "현대"))
				.build();
		}
	}


	/**
	 * 36번 인기 악기 조회
	 */
	public SearchResponse.Instrument getInstruments() {
		// TODO:
		try {
			// Elasticsearch에서 인기 악기 조회
			List<String> popularInstruments = elkSearchRepository.getPopularInstruments();

			// 이미지 링크 매핑
			List<String> imgLinks = popularInstruments.stream()
				.map(instrument -> "/resources/static/img/instrument/" + instrument.toLowerCase() + ".jpg")
				.collect(Collectors.toList());

			return SearchResponse.Instrument.builder()
				.instrument(popularInstruments)
				.imgLink(imgLinks)
				.build();

		} catch (Exception e) {
			// 실패 시 기본값 반환
			return SearchResponse.Instrument.builder()
				.instrument(Arrays.asList("피아노", "바이올린", "첼로", "플루트", "클라리넷"))
				.imgLink(Arrays.asList(
					"/resources/static/img/instrument/piano.jpg",
					"/resources/static/img/instrument/violin.jpg",
					"/resources/static/img/instrument/cello.jpg",
					"/resources/static/img/instrument/flute.jpg",
					"/resources/static/img/instrument/clarinet.jpg"
				))
				.build();
		}
	}





	private String getCurrentUserId() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated()) {
				return "anonymous";
			}

			Object principal = auth.getPrincipal();
			if (principal instanceof UserDetails) {
				return ((UserDetails) principal).getUsername();
			} else if (principal instanceof String) {
				return (String) principal;
			} else {
				return "anonymous";
			}
		} catch (Exception e) {
			return "anonymous";
		}
	}
	/**
	 * 37번 검색결과 - 게시글 + 인기미디어
	 */
	public SearchResponse.SearchResultAll searchAll(String query) {
		// 일반 검색 로그 저장 (카테고리 null)
		searchLogService.logGeneralSearch(query, getCurrentUserId());

		try {
			List<String> postIds = elkSearchRepository.searchPosts(query);
			return SearchResponse.SearchResultAll.builder()
				.results(Arrays.asList())
				.popularMedia(Arrays.asList())
				.build();
		} catch (Exception e) {
			return SearchResponse.SearchResultAll.builder()
				.results(Arrays.asList())
				.popularMedia(Arrays.asList())
				.build();
		}
	}

	/**
	 * 38번 검색결과 - 프로필
	 */
	public SearchResponse.SearchProfile searchProfile(String query) {
		// 프로필 검색 로그 저장
		searchLogService.logSearch(query, "profile", getCurrentUserId());

		try {
			List<String> userIds = elkSearchRepository.searchUsers(query);
			return SearchResponse.SearchProfile.builder()
				.user(Arrays.asList())
				.build();
		} catch (Exception e) {
			return SearchResponse.SearchProfile.builder()
				.user(Arrays.asList())
				.build();
		}
	}

	/**
	 * 39번 검색결과 - 피드
	 */
	public SearchResponse.SearchFeed searchFeed(String query) {
		// 피드 검색 로그 저장
		searchLogService.logSearch(query, "feed", getCurrentUserId());

		try {
			List<String> recentPostIds = elkSearchRepository.searchPosts(query);
			return SearchResponse.SearchFeed.builder()
				.resultsRecent(Arrays.asList())
				.resultPopular(Arrays.asList())
				.build();
		} catch (Exception e) {
			return SearchResponse.SearchFeed.builder()
				.resultsRecent(Arrays.asList())
				.resultPopular(Arrays.asList())
				.build();
		}
	}

}
