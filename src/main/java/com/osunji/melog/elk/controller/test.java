package com.osunji.melog.elk.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osunji.melog.elk.service.SearchLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class test {

	private final SearchLogService searchLogService;

	/**
	 * 테스트 로그 저장 - 인덱스 생성을 위해
	 */
	@PostMapping("/create-index")
	public ResponseEntity<String> createSearchLogsIndex() {
		try {
			// 여러 개의 테스트 데이터 저장
			searchLogService.logSearch("베토벤", "composer", "testUser1");
			searchLogService.logSearch("모차르트", "composer", "testUser2");
			searchLogService.logSearch("피아노", "instrument", "testUser3");
			searchLogService.logSearch("바이올린", "instrument", "testUser1");
			searchLogService.logSearch("클래식", "genre", "testUser4");
			searchLogService.logSearch("바로크", "period", "testUser2");

			return ResponseEntity.ok("ㅊㅊ");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("오류 발생: " + e.getMessage());
		}
	}
}
