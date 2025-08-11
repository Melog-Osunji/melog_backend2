package com.osunji.melog.search.service;

import com.osunji.melog.search.dto.response.SearchResponse;
import com.osunji.melog.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

	private final SearchRepository searchRepository;

	/**
	 * api(31번) 통합 검색 - 추천키워드 6개 + 실시간 인기 검색어 20개
	 */
	public SearchResponse.AllSearch getAllSearch() {
		return searchRepository.getAllSearchData();
	}

	/**
	 * api(32번) 인기 작곡가 조회 (검색량 상위 + 태그량 상위)
	 */
	public List<SearchResponse.Composer> getPopularComposers() {
		return searchRepository.getPopularComposers();
	}

	/**
	 * api(33번) 인기 연주가 + 관련 키워드 조회
	 */
	public List<SearchResponse.Player> getPopularPlayers() {
		return searchRepository.getPopularPlayers();
	}

	/**
	 * api(34번) 장르 + 관련 키워드 조회
	 */
	public List<SearchResponse.Genre> getGenres() {
		return searchRepository.getGenres();
	}

	/**
	 * api(35번) 인기 시대 조회 (검색량 상위 + 태그량 상위)
	 */
	public SearchResponse.Period getPeriods() {
		return searchRepository.getPeriods();
	}

	/**
	 * api(36번) 인기 악기 조회 (검색량 상위 + 태그량 상위)
	 */
	public SearchResponse.Instrument getInstruments() {
		return searchRepository.getInstruments();
	}

	/**
	 * api(37번) 검색결과 - 게시글 + 인기미디어
	 */
	public SearchResponse.SearchResultAll searchAll(String query) {
		return searchRepository.searchAll(query);
	}

	/**
	 * api(38번) 검색결과 - 프로필
	 */
	public SearchResponse.SearchProfile searchProfile(String query) {
		return searchRepository.searchProfile(query);
	}

	/**
	 * api(39번) 검색결과 - 피드 (최신순 + 인기순)
	 */
	public SearchResponse.SearchFeed searchFeed(String query) {
		return searchRepository.searchFeed(query);
	}
}
