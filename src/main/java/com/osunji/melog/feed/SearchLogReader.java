package com.osunji.melog.feed;

import java.time.*;
import java.util.*;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

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
        Query time = RangeQuery.of(r -> r
                .field("searchTime")
                .gte(JsonData.of("now-" + days + "d"))
        )._toQuery();        Query uid  = TermQuery.of(t -> t.field("userId").value(userId))._toQuery();
        Query bool = BoolQuery.of(b -> b.filter(uid).filter(time))._toQuery();

        NativeQuery nq = new NativeQueryBuilder()
                .withQuery(bool)
                .withAggregations(a -> a.aggregation(aggName,
                        Aggregation.of(ag -> ag.terms(t -> t.field(field).size(size)))))
                .withMaxResults(0)
                .build();

        SearchHits<Object> resp =
                esOps.search(nq, Object.class, IndexCoordinates.of(SEARCH_INDEX));

        var out = new LinkedHashMap<String, Long>();
        if (resp.getAggregations() instanceof ElasticsearchAggregations elcAggs) {
            var aggMap = elcAggs.aggregations();
            StringTermsAggregate terms = aggMap.get(aggName).sterms();
            if (terms != null && terms.buckets() != null) {
                for (StringTermsBucket b : terms.buckets().array()) {
                    out.put(b.key().stringValue(), b.docCount());
                }
            }
        }
        return out;
    }
}