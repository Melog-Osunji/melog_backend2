package com.osunji.melog.elk.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "posts")
@Data
@Builder
public class PostIndex {
	@Id
	private String id;

	@Field(type = FieldType.Text, analyzer = "standard")
	private String title;

	@Field(type = FieldType.Text, analyzer = "standard")
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
