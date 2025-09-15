package com.osunji.melog.feed;

import java.util.LinkedHashMap;
import java.util.Map;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;

@Service
@RequiredArgsConstructor
public class SearchLogReader {

    private static final String SEARCH_INDEX = "search_logs";

    private final ElasticsearchOperations esOps;

    public Map<String, Long> topQueries(String userId, int days, int size) {
        return termsAgg(userId, days, "query.keyword", size, "top_queries");
    }

    public Map<String, Long> topCategories(String userId, int days, int size) {
        return termsAgg(userId, days, "category.keyword", size, "top_categories");
    }

    private Map<String, Long> termsAgg(String userId, int days, String field, int size, String aggName) {
        // 1) 기간/사용자 필터
        Query time = RangeQuery.of(r -> r
                .field("searchTime")
                .gte(JsonData.of("now-" + days + "d"))   // 날짜형이면 ES가 파싱
        )._toQuery();

        Query uid = TermQuery.of(t -> t.field("userId").value(userId))._toQuery();
        Query bool = BoolQuery.of(b -> b.filter(uid).filter(time))._toQuery();

        // 2) terms 집계 정의
        Aggregation termsAgg = Aggregation.of(ag -> ag
                .terms(t -> t.field(field).size(size))
        );

        // 3) NativeQuery 구성 (※ withAggregation 사용)
        NativeQuery nq = new NativeQueryBuilder()
                .withQuery(bool)
                .withAggregation(aggName, termsAgg)
                .withMaxResults(0)              // 문서 본문 0, 집계만
                // .withTrackTotalHits(false)    // 선택: 비용 절감
                .build();

        SearchHits<Object> resp = esOps.search(nq, Object.class, IndexCoordinates.of(SEARCH_INDEX));

        Map<String, Long> out = new LinkedHashMap<>();

        var aggs = resp.getAggregations();
        if (aggs instanceof ElasticsearchAggregations elcAggs) {
            var ea = elcAggs.aggregationsAsMap().get(aggName); // 이름으로 바로 접근
            if (ea != null) {
                Aggregate agg = ea.aggregation().getAggregate();

                if (agg != null && agg.isSterms()) {
                    var terms = agg.sterms();
                    if (terms != null && terms.buckets() != null && terms.buckets().isArray()) {
                        for (var b : terms.buckets().array()) {
                            out.put(b.key().stringValue(), b.docCount());
                        }
                    }
                }
            }
        }


        return out;
    }
}
