package com.osunji.melog.youtube.Controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.osunji.melog.youtube.service.YouTubeService;
import com.osunji.melog.youtube.entity.YoutubeItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
public class YouTubeController {

	private final YouTubeService youTubeService;

	@GetMapping("/search")
	public ResponseEntity<List<YoutubeItem>> searchYouTube(
		@RequestParam(value = "word", required = true) String search,
		@RequestParam(value = "items", required = false, defaultValue = "5") int items) {

		log.info("=== Controller: YouTube 검색 요청 ===");
		log.info("요청 파라미터 - word: {}, items: {}", search, items);

		try {
			List<YoutubeItem> results = youTubeService.searchYouTube(search, items);
			log.info("Controller: 검색 결과 {}개 반환", results.size());
			return ResponseEntity.ok(results);

		} catch (Exception e) {
			log.error("Controller에서 예외 발생: {}", e.getMessage());
			log.error("전체 예외: ", e);
			return ResponseEntity.status(500).body(new ArrayList<>());
		}
	}
}
