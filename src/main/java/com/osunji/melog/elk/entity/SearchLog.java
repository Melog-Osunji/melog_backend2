package com.osunji.melog.elk.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "search_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchLog {

	@Id
	private String id;

	@Field(type = FieldType.Keyword)
	private String query;

	@Field(type = FieldType.Keyword)
	private String category;

	@Field(type = FieldType.Date)
	private LocalDateTime searchTime;

	@Field(type = FieldType.Keyword)
	private String userId;
}
