package com.osunji.melog.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import com.osunji.melog.search.dto.AutocompleteKeyword;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;  // ✅ javax → jakarta 변경
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutocompleteIndexManager {

	private final ElasticsearchClient elasticsearchClient;

	@PostConstruct
	public void initializeAutocompleteIndex() {
		try {
			createAutocompleteIndex();
			populateInitialKeywords();
		} catch (Exception e) {
			log.error("자동완성 인덱스 초기화 실패: {}", e.getMessage());
		}
	}

	/**
	 * ✅ 자동완성용 인덱스 생성 (ELKUserRepository 스타일 적용)
	 */
	private void createAutocompleteIndex() {
		String indexName = "autocomplete-keywords";

		try {
			// 인덱스가 이미 존재하는지 확인
			GetIndexRequest getRequest = GetIndexRequest.of(g -> g.index(indexName));
			elasticsearchClient.indices().get(getRequest);
			System.out.println("📋 자동완성 인덱스가 이미 존재합니다: " + indexName);
			return;
		} catch (Exception e) {
			// 인덱스가 없으면 생성
			createNewAutocompleteIndex(indexName);
		}
	}

	/**
	 * ✅ 새 자동완성 인덱스 생성
	 */
	private void createNewAutocompleteIndex(String indexName) {
		try {
			CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
				.index(indexName)
				.settings(s -> s
					.index(idx -> idx
						.analysis(a -> a
							// Edge N-Gram 필터 설정
							.filter("f_edge_ngram", f -> f
								.definition(d -> d
									.edgeNgram(en -> en
										.minGram(1)
										.maxGram(10)
									)
								)
							)
							// 색인용 Analyzer
							.analyzer("a_autocomplete_index", an -> an
								.custom(ca -> ca
									.tokenizer("standard")
									.filter("lowercase", "f_edge_ngram")
								)
							)
							// 검색용 Analyzer
							.analyzer("a_autocomplete_search", an -> an
								.custom(ca -> ca
									.tokenizer("standard")
									.filter("lowercase")
								)
							)
						)
					)
				)
				.mappings(m -> m
					.properties("keyword", p -> p
						.text(t -> t
							.analyzer("a_autocomplete_index")
							.searchAnalyzer("a_autocomplete_search")
						)
					)
					.properties("type", p -> p.keyword(k -> k))
					.properties("priority", p -> p.integer(i -> i))
				)
			);

			elasticsearchClient.indices().create(createRequest);
			System.out.println("✅ 자동완성 인덱스 생성 완료: " + indexName);
		} catch (Exception e) {
			log.error("자동완성 인덱스 생성 실패: {}", e.getMessage());
		}
	}

	/**
	 * ✅ 초기 키워드 데이터 삽입
	 */
	private void populateInitialKeywords() throws Exception {
		List<AutocompleteKeyword> initialKeywords = getInitialKeywords();

		BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

		for (AutocompleteKeyword keyword : initialKeywords) {
			bulkBuilder.operations(op -> op
				.index(IndexOperation.of(io -> io
					.index("autocomplete-keywords")
					.document(keyword)
				))
			);
		}

		BulkResponse bulkResponse = elasticsearchClient.bulk(bulkBuilder.build());

		if (bulkResponse.errors()) {
			System.out.println("❌ 일부 키워드 삽입 실패");
		} else {
			System.out.println("✅ " + initialKeywords.size() + "개 초기 키워드 삽입 완료");
		}
	}

	/**
	 * ✅ 초기 키워드 목록 생성
	 */
	private List<AutocompleteKeyword> getInitialKeywords() {
		List<AutocompleteKeyword> keywords = new ArrayList<>();

		// 작곡가 키워드 (우선순위 높음)
		String[] composers = {
			"바흐", "베토벤", "모차르트", "쇼팽", "브람스", "리스트", "슈베르트", "하이든",
			"슈만", "드뷔시", "라벨", "차이콥스키", "라흐마니노프", "베르디", "푸치니", "바그너",
			"멘델스존", "그리그", "드보르자크", "시벨리우스", "스트라빈스키", "프로코피예프",
			"쇼스타코비치", "말러", "리하르트 슈트라우스", "생상스", "포레", "비제", "무소르그스키",
			"림스키-코르사코프", "스메타나", "브루크너", "요한 슈트라우스 2세", "거슈윈", "바르톡",
			"쉰베르크", "메시앙", "윤이상", "비발디", "헨델", "텔레만", "라모", "글루크", "베버",
			"로시니", "베를리오즈", "프랑크", "에릭 사티", "조스캥", "팔레스트리나", "몬테베르디"
		};

		for (String composer : composers) {
			keywords.add(new AutocompleteKeyword(composer, "composer", 100));
		}

		// 악기 키워드
		String[] instruments = {
			"피아노", "바이올린", "첼로", "플루트", "클라리넷", "트럼펫", "호른", "트롬본",
			"튜바", "오보에", "바순", "색소폰", "하프", "기타", "드럼", "심벌즈"
		};

		for (String instrument : instruments) {
			keywords.add(new AutocompleteKeyword(instrument, "instrument", 80));
		}

		// 장르 키워드
		String[] genres = {
			"클래식", "바로크", "낭만주의", "현대음악", "재즈", "블루스", "오페라",
			"교향곡", "협주곡", "소나타", "실내악", "발레"
		};

		for (String genre : genres) {
			keywords.add(new AutocompleteKeyword(genre, "genre", 60));
		}

		// 연주가 키워드
		String[] performers = {
			"조성진", "임윤찬", "정명훈", "백혜선", "김덕수", "양인모",
			"랑랑", "마르타 아르헤리치", "다니엘 바렌보임", "요요마", "이츠하크 펄만",
			"안네 조피 무터", "플라시도 도밍고", "루치아노 파바로티"
		};

		for (String performer : performers) {
			keywords.add(new AutocompleteKeyword(performer, "performer", 70));
		}

		return keywords;
	}
}
