package com.marsreg.search.model;

import com.marsreg.common.model.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Document(indexName = "documents")
@EqualsAndHashCode(callSuper = true)
public class SearchDocument extends BaseDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String objectName;

    @Field(type = FieldType.Keyword)
    private String bucket;

    @Field(type = FieldType.Keyword)
    private String storagePath;

    @Field(type = FieldType.Keyword)
    private String md5;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Text)
    private String errorMessage;

    @Field(type = FieldType.Date)
    private LocalDateTime processedTime;

    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
} 