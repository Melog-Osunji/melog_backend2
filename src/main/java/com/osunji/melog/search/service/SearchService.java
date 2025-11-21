package com.osunji.melog.search.service;

import com.osunji.melog.search.dto.response.SearchResponse;
import com.osunji.melog.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
@Slf4j

@Service
@RequiredArgsConstructor
public class SearchService {
	private final CacheManager cacheManager;
	private final ObjectMapper objectMapper;
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
	public SearchResponse.Composer getPopularComposers() {  // ✅ List 제거
		List<SearchResponse.Composer> composers = searchRepository.getPopularComposers();
		// 첫 번째 요소만 반환하거나, 합쳐서 반환
		return composers.isEmpty() ?
			SearchResponse.Composer.builder().name(List.of()).imgLink(List.of()).build() :
			composers.get(0);
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
	public SearchResponse.SearchProfile searchProfile(String query, String authHeader) {
		return searchRepository.searchProfile(query, authHeader);
	}


	/**
	 * api(39번) 검색결과 - 피드 (최신순 + 인기순)
	 */

	public SearchResponse.SearchFeed searchFeed(String query) {
		Cache cache = cacheManager.getCache("searchFeedCache");

		if (cache != null) {
			Cache.ValueWrapper cached = cache.get(query);
			if (cached != null) {
				try {
					String json = (String) cached.get();
					return objectMapper.readValue(json, SearchResponse.SearchFeed.class);
				} catch (Exception e) {
					// 캐시 역직렬화 실패 시 캐시 무시 후 새로 조회
					log.warn("캐시 역직렬화 실패, 새로 검색 진행: {}", e.getMessage());
				}
			}
		}

		// 캐시 미스일 경우 실제 검색 수행
		SearchResponse.SearchFeed result = searchRepository.searchFeed(query);

		if (cache != null) {
			try {
				String json = objectMapper.writeValueAsString(result);
				cache.put(query, json);
			} catch (Exception e) {
				log.warn("캐시 저장 실패: {}", e.getMessage());
			}
		}

		return result;
	}

	public SearchResponse.Autocomplete getAutocomplete(String query) {
		Cache cache = cacheManager.getCache("autocompleteCache");
		if (cache != null) {
			Cache.ValueWrapper cached = cache.get(query);
			if (cached != null) {
				try {
					String json = (String) cached.get();
					return objectMapper.readValue(json, SearchResponse.Autocomplete.class);
				} catch (Exception e) {
					// JSON 역직렬화 실패 시 캐시 무시하고 새로 조회
				}
			}
		}

		SearchResponse.Autocomplete result = searchRepository.getAutocomplete(query);

		if (cache != null) {
			try {
				String json = objectMapper.writeValueAsString(result);
				cache.put(query, json);
			} catch (Exception e) {
				// 캐시 저장 실패 로그 기록
			}
		}

		return result;
	}
}
