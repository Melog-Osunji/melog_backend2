package com.osunji.melog.elk.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "user_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ES 인덱스명 = search_logs
public class UserLog {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String eventType; // LOGIN, LOGOUT, SIGNUP, REFRESH, FOLLOW 등

    @Field(type = FieldType.Date)
    private LocalDateTime eventTime;

    @Field(type = FieldType.Keyword)
    private String ip;

    @Field(type = FieldType.Keyword)
    private String userAgent;

    @Field(type = FieldType.Object)
    private String metaJson; // JSON string, 추가 컨텍스트(예: provider, jti 등)
}
