package com.osunji.melog.search.preset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
@Component
public class SearchPresetLoader {
	private final ObjectMapper om = new ObjectMapper();

	private JsonNode composer;
	private JsonNode player;
	private JsonNode genre;
	private JsonNode period;
	private JsonNode instrument;
	private JsonNode recommendSeed;

	@PostConstruct
	public void load() {
		System.out.println("📂 검색 사전 데이터 로딩 시작...");

		try {
			composer = read("search/composer.json");
			System.out.println("✅ composer.json 로드 완료");

			player = read("search/player.json");
			System.out.println("✅ player.json 로드 완료");

			genre = read("search/genre.json");
			System.out.println("✅ genre.json 로드 완료");

			period = read("search/period.json");
			System.out.println("✅ period.json 로드 완료");

			instrument = read("search/instrument.json");
			System.out.println("✅ instrument.json 로드 완료");

			recommendSeed = readOptional("search/recommend_keywords.json");
			System.out.println("✅ recommend_keywords.json 로드 완료");

			System.out.println("🎉 모든 검색 사전 데이터 로딩 완료!");

		} catch (Exception e) {
			System.out.println("❌ 사전 데이터 로딩 실패: " + e.getMessage());
			throw e;
		}
	}

	private JsonNode read(String path) {
		try (InputStream is = new ClassPathResource(path).getInputStream()) {
			JsonNode node = om.readTree(is);
			System.out.println("  - " + path + " 파일 크기: " + node.size());
			return node;
		} catch (Exception e) {
			System.out.println("❌ 파일 읽기 실패: " + path);
			throw new IllegalStateException("Preset load failed: " + path, e);
		}
	}

	private JsonNode readOptional(String path) {
		try (InputStream is = new ClassPathResource(path).getInputStream()) {
			JsonNode node = om.readTree(is);
			System.out.println("  - " + path + " (선택사항) 로드 성공");
			return node;
		} catch (Exception e) {
			System.out.println("  - " + path + " (선택사항) 파일 없음 - 빈 객체 반환");
			return om.createObjectNode();
		}
	}

	// Getters
	public JsonNode composer() { return composer; }
	public JsonNode player() { return player; }
	public JsonNode genre() { return genre; }
	public JsonNode period() { return period; }
	public JsonNode instrument() { return instrument; }

	public List<String> recommendSeed() {
		if (recommendSeed.has("recommendKeyword")) {
			List<String> seeds = new ObjectMapper().convertValue(recommendSeed.get("recommendKeyword"), List.class);
			System.out.println("📝 추천 키워드 시드 수: " + seeds.size());
			return seeds;
		}
		System.out.println("📝 추천 키워드 시드 없음 - 빈 리스트 반환");
		return List.of();
	}

	// ✅ 디버깅용 메서드 추가
	public void printLoadedData() {
		System.out.println("=== 로드된 검색 사전 데이터 ===");
		System.out.println("작곡가: " + (composer != null ? composer.size() + "개" : "없음"));
		System.out.println("연주가: " + (player != null ? player.size() + "개" : "없음"));
		System.out.println("장르: " + (genre != null ? genre.size() + "개" : "없음"));
		System.out.println("시대: " + (period != null ? period.size() + "개" : "없음"));
		System.out.println("악기: " + (instrument != null ? instrument.size() + "개" : "없음"));
		System.out.println("추천 시드: " + recommendSeed().size() + "개");
	}
}
