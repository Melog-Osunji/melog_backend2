package com.osunji.melog.search.repository;

import com.osunji.melog.search.dto.response.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

	// TODO: Elasticsearch client 주입 필요

	/**
	 * 31번 통합 검색 데이터 조회
	 */
	public SearchResponse.AllSearch getAllSearchData() {
		// TODO: Elasticsearch에서 추천키워드 + 실시간 인기검색어 조회

		// 임시 데이터
		List<String> recommendKeywords = Arrays.asList(
			"베토벤", "모차르트", "쇼팽", "바흐", "브람스", "리스트"
		);

		List<String> livePopularSearch = Arrays.asList(
			"피아노", "교향곡", "협주곡", "소나타", "바이올린", "첼로",
			"오페라", "실내악", "바로크", "낭만주의", "클래식", "재즈",
			"현대음악", "성악", "오케스트라", "지휘", "연주회", "콘서트",
			"음악감상", "클래식기타"
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
		// TODO: Elasticsearch 집계 쿼리로 검색량 상위 + 태그량 상위 작곡가 조회

		// 임시 데이터
		return Arrays.asList(
			SearchResponse.Composer.builder()
				.name(Arrays.asList("베토벤", "모차르트", "쇼팽", "바흐", "브람스"))
				.imgLink(Arrays.asList(
					"/resources/static/img/composer/beethoven.jpg",
					"/resources/static/img/composer/mozart.jpg",
					"/resources/static/img/composer/chopin.jpg",
					"/resources/static/img/composer/bach.jpg",
					"/resources/static/img/composer/brahms.jpg"
				))
				.build()
		);
	}

	/**
	 * 33번 인기 연주가 + 관련 키워드 조회
	 */
	public List<SearchResponse.Player> getPopularPlayers() {
		// TODO: Elasticsearch에서 인기 연주가 조회 후 관련 키워드 집계

		// 임시 데이터
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
		// TODO: Elasticsearch에서 시대별 검색량 + 태그량 집계

		// 임시 데이터
		return SearchResponse.Period.builder()
			.era(Arrays.asList("바로크", "고전주의", "낭만주의", "근현대", "현대"))
			.build();
	}

	/**
	 * 36번 인기 악기 조회
	 */
	public SearchResponse.Instrument getInstruments() {
		// TODO: Elasticsearch에서 악기별 검색량 + 태그량 집계

		// 임시 데이터
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

	/**
	 * 37번 검색결과 - 게시글 + 인기미디어
	 */
	public SearchResponse.SearchResultAll searchAll(String query) {
		// TODO: Elasticsearch에서 쿼리 기반 검색

		// 임시 데이터
		return SearchResponse.SearchResultAll.builder()
			.results(Arrays.asList())
			.popularMedia(Arrays.asList())
			.build();
	}

	/**
	 * 38번 검색결과 - 프로필
	 */
	public SearchResponse.SearchProfile searchProfile(String query) {
		// TODO: Elasticsearch에서 사용자 프로필 검색

		// 임시 데이터
		return SearchResponse.SearchProfile.builder()
			.user(Arrays.asList())
			.build();
	}

	/**
	 * 39번 검색결과 - 피드
	 */
	public SearchResponse.SearchFeed searchFeed(String query) {
		// TODO: Elasticsearch에서 최신순 + 인기순 피드 검색

		// 임시 데이터
		return SearchResponse.SearchFeed.builder()
			.resultsRecent(Arrays.asList())
			.resultPopular(Arrays.asList())
			.build();
	}

}
