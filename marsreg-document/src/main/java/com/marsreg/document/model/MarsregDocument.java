package com.marsreg.document.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Accessors(chain = true)
@Document(indexName = "marsreg_documents")
public class MarsregDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Object)
    private String metadata;

    @Field(type = FieldType.Date)
    private String createTime;

    @Field(type = FieldType.Date)
    private String updateTime;
} 