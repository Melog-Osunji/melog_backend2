package com.osunji.melog.search.controller;
import com.osunji.melog.search.dto.response.SearchResponse;
import com.osunji.melog.global.dto.ApiMessage;
import com.osunji.melog.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	//---------------통합 검색-----------------//
	/**
	 * [31번] 통합 검색 - 추천키워드 6개 + 실시간 인기 검색어 20개
	 * GET /api/search/all
	 */
	@GetMapping("/search/all")
	public ResponseEntity<ApiMessage<SearchResponse.AllSearch>> getAllSearch() {
		System.out.println("🔍 통합 검색 API 호출됨");
		SearchResponse.AllSearch response = searchService.getAllSearch();
		ApiMessage<SearchResponse.AllSearch> apiResponse = ApiMessage.success(200, "통합 검색 데이터 조회 성공", response);
		return ResponseEntity.ok(apiResponse);
	}

	//---------------검색 전 페이지-----------------//
	/**
	 * [32번] 인기 작곡가 조회
	 * GET /api/search/composer
	 */
	/**
	 * [32번] 인기 작곡가 조회
	 * GET /api/search/composer
	 */
	@GetMapping("/search/composer")
	public ResponseEntity<ApiMessage<SearchResponse.Composer>> getPopularComposers() {
		System.out.println("🎼 인기 작곡가 API 호출됨");
		SearchResponse.Composer response = (SearchResponse.Composer)searchService.getPopularComposers();  // ✅ 캐스팅 필요없음
		ApiMessage<SearchResponse.Composer> apiResponse = ApiMessage.success(200, "인기 작곡가 조회 성공", response);
		return ResponseEntity.ok(apiResponse);
	}


	/**
	 * [33번] 인기 연주가 + 관련 키워드 조회
	 * GET /api/search/player
	 */
	@GetMapping("/search/player")
	public ResponseEntity<ApiMessage<List<SearchResponse.Player>>> getPopularPlayers() {
		System.out.println("🎹 인기 연주가 API 호출됨");
		List<SearchResponse.Player> response = searchService.getPopularPlayers();
		ApiMessage<List<SearchResponse.Player>> apiResponse = ApiMessage.success(200, "인기 연주가 조회 성공", response);
		return ResponseEntity.ok(apiResponse);
	}

	/**
	 * [34번] 장르 + 관련 키워드 조회
	 * GET /api/search/genre
	 */
	@GetMapping("/search/genre")
	public ResponseEntity<ApiMessage<List<SearchResponse.Genre>>> getGenres() {
		System.out.println("🎵 장르 API 호출됨");
		List<SearchResponse.Genre> response = searchService.getGenres();
		ApiMessage<List<SearchResponse.Genre>> apiResponse = ApiMessage.success(200, "장르 목록 조회 성공", response);
		return ResponseEntity.ok(apiResponse);
	}

	/**
	 * [35번] 인기 시대 조회
	 * GET /api/search/period
	 */
	@GetMapping("/search/period")
	public ResponseEntity<ApiMessage<SearchResponse.Period>> getPeriods() {
		System.out.println("⏰ 인기 시대 API 호출됨");
		SearchResponse.Period response = searchService.getPeriods();
		ApiMessage<SearchResponse.Period> apiResponse = ApiMessage.success(200, "인기 시대 조회 성공", response);
		return ResponseEntity.ok(apiResponse);
	}

	/**
	 * [36번] 인기 악기 조회
	 * GET /api/search/instrument
	 */
	@GetMapping("/search/instrument")
	public ResponseEntity<ApiMessage<SearchResponse.Instrument>> getInstruments() {
		System.out.println("🎺 인기 악기 API 호출됨");
		SearchResponse.Instrument response = searchService.getInstruments();
		ApiMessage<SearchResponse.Instrument> apiResponse = ApiMessage.success(200, "인기 악기 조회 성공", response);
		return ResponseEntity.ok(apiResponse);
	}

	//---------------검색 결과-----------------//
	/**
	 * [37번] 검색결과 - 게시글 + 인기미디어
	 * GET /api/search?q=검색어 (기본 검색)
	 */
	@GetMapping("/search")
	public ResponseEntity<ApiMessage<SearchResponse.SearchResultAll>> searchAll(
		@RequestParam String q) {
		System.out.println("🔍 통합 검색 실행: '" + q + "'");
		SearchResponse.SearchResultAll response = searchService.searchAll(q);
		ApiMessage<SearchResponse.SearchResultAll> apiResponse = ApiMessage.success(200, "검색 완료", response);
		return ResponseEntity.ok(apiResponse);
	}

	/**
	 * [38번] 검색결과 - 프로필
	 * GET /api/search/profile?q=검색어
	 */

	@GetMapping("/search/profile")
	public ResponseEntity<ApiMessage<SearchResponse.SearchProfile>> searchProfile(
		@RequestParam String q,
		@RequestHeader("Authorization") String authHeader) {

		System.out.println("👤 프로필 검색 실행: '" + q + "'");
		SearchResponse.SearchProfile response = searchService.searchProfile(q, authHeader);
		ApiMessage<SearchResponse.SearchProfile> apiResponse = ApiMessage.success(200, "프로필 검색 완료", response);
		return ResponseEntity.ok(apiResponse);
	}


	/**
	 * [39번] 검색결과 - 피드
	 * GET /api/search/feed?q=검색어
	 */
	@GetMapping("/search/feed")
	public ResponseEntity<ApiMessage<SearchResponse.SearchFeed>> searchFeed(
		@RequestParam String q) {
		System.out.println("📰 피드 검색 실행: '" + q + "'");
		SearchResponse.SearchFeed response = searchService.searchFeed(q);
		ApiMessage<SearchResponse.SearchFeed> apiResponse = ApiMessage.success(200, "피드 검색 완료", response);
		return ResponseEntity.ok(apiResponse);
	}
}
