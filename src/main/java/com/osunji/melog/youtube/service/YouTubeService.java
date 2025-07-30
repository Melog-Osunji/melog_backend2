package com.osunji.melog.youtube.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.osunji.melog.youtube.entity.YoutubeItem;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class YouTubeService {

	@Value("${youtube.api-key}")
	private String youtubeApiKey;

	// 필드 선언 추가
	private YouTube youtube;
	private static final String GOOGLE_YOUTUBE_URL = "https://www.youtube.com/watch?v=";
	private static final String YOUTUBE_SEARCH_FIELDS = "items(id/kind,id/videoId,snippet/title," +
		"snippet/description,snippet/channelTitle,snippet/thumbnails/default/url)";

	// 생성자에서는 YouTube 객체 초기화만
	public YouTubeService() {
		log.info("YouTubeService 생성자 호출");
	}

	// @PostConstruct에서 YouTube 객체 초기화
	@PostConstruct
	public void init() {
		log.info("YouTube API 키 확인: {}", youtubeApiKey != null ? "설정됨" : "설정안됨");
		log.info("YouTube API 키 앞 10자리: {}",
			youtubeApiKey != null && youtubeApiKey.length() > 10 ?
				youtubeApiKey.substring(0, 10) + "..." : "없음");

		// YouTube 객체 초기화
		this.youtube = new YouTube.Builder(
			new NetHttpTransport(),
			new JacksonFactory(),
			new HttpRequestInitializer() {
				public void initialize(HttpRequest request) throws IOException {
					log.debug("YouTube API 요청 초기화: {}", request.getUrl());
				}
			}
		).setApplicationName("melog-youtube-search").build();

		log.info("YouTubeService 초기화 완료");
	}

	public List<YoutubeItem> searchYouTube(String searchQuery, int maxResults) {
		log.info("=== YouTube 검색 시작 ===");
		log.info("검색어: {}", searchQuery);
		log.info("최대 결과 수: {}", maxResults);
		log.info("API 키 상태: {}", youtubeApiKey != null ? "있음" : "없음");

		List<YoutubeItem> results = new ArrayList<>();

		try {
			if (youtube == null) {
				log.error("YouTube 객체가 null입니다!");
				return results;
			}

			log.info("YouTube API 요청 생성 중...");
			YouTube.Search.List search = youtube.search()
				.list(Collections.singletonList("id,snippet"));

			search.setKey(youtubeApiKey);
			search.setQ(searchQuery);
			search.setType(Collections.singletonList("video"));
			search.setMaxResults((long) maxResults);
			search.setFields(YOUTUBE_SEARCH_FIELDS);

			log.info("YouTube API 실행 중...");
			SearchListResponse searchResponse = search.execute();
			log.info("YouTube API 응답 받음");

			List<SearchResult> searchResultList = searchResponse.getItems();

			if (searchResultList == null) {
				log.warn("검색 결과가 null입니다!");
				return results;
			}

			log.info("검색 결과 개수: {}", searchResultList.size());

			for (SearchResult result : searchResultList) {
				YoutubeItem item = new YoutubeItem(
					GOOGLE_YOUTUBE_URL + result.getId().getVideoId(),
					result.getSnippet().getTitle(),
					result.getSnippet().getThumbnails().getDefault().getUrl(),
					result.getSnippet().getDescription()
				);
				results.add(item);
				log.info("비디오 추가: {} - {}",
					result.getId().getVideoId(),
					result.getSnippet().getTitle());
			}

			log.info("=== YouTube 검색 완료: 총 {}개 ===", results.size());

		} catch (GoogleJsonResponseException e) {
			log.error("YouTube API 오류 - 코드: {}, 메시지: {}",
				e.getDetails().getCode(),
				e.getDetails().getMessage());
			log.error("전체 에러: ", e);
		} catch (IOException e) {
			log.error("IO 에러: {}", e.getMessage());
			log.error("전체 IO 에러: ", e);
		} catch (Exception e) {
			log.error("예상치 못한 에러: {}", e.getMessage());
			log.error("전체 예외: ", e);
		}

		return results;
	}

	/**
	 * 클래식 음악 검색 (자동으로 "클래식" 키워드 추가)
	 */
	public List<YoutubeItem> searchClassicalMusic(String query, int maxResults) {
		String searchQuery = query.trim().isEmpty() ? "클래식 음악" : query + " 클래식";
		return searchYouTube(searchQuery, maxResults);
	}

	/**
	 * 인기 클래식 음악 조회
	 */
	public List<YoutubeItem> getTrendingClassical(int maxResults) {
		return searchYouTube("클래식 음악 인기", maxResults);
	}
}
