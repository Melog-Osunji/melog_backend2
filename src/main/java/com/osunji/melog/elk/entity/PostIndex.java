package com.osunji.melog.elk.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "posts")
public class PostIndex {
	@Id
	private String id;

	@Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")
	private String title;

	@Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")
	private String content;

	@Field(type = FieldType.Keyword)
	private List<String> tags;

	@Field(type = FieldType.Keyword)
	private String userId;

	@Field(type = FieldType.Integer)
	private Integer likeCount;

	@Field(type = FieldType.Date)
	private LocalDateTime createdAt;

	@Field(type = FieldType.Keyword)
	private String mediaType;

	@Field(type = FieldType.Keyword)
	private String mediaUrl;
}
