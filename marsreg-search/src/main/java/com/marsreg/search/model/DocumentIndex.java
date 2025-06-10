package com.marsreg.search.model;

import lombok.Data;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.CompletionField;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Document(indexName = "documents")
public class DocumentIndex {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String documentId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    @CompletionField(maxInputLength = 100)
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    @Field(type = FieldType.Keyword)
    private String documentType;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String contentType;

    @Field(type = FieldType.Keyword)
    private String originalName;

    @Field(type = FieldType.Long)
    private Long size;

    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;

    @Field(type = FieldType.Keyword)
    private String createBy;

    @Field(type = FieldType.Keyword)
    private String updateBy;

    @Field(type = FieldType.Keyword)
    @CompletionField(maxInputLength = 50)
    private List<String> tags;

    @Field(type = FieldType.Integer)
    private int viewCount;

    @Field(type = FieldType.Integer)
    private int searchCount;

    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
} 