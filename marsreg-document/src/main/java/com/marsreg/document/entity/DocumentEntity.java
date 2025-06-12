package com.marsreg.document.entity;

import com.marsreg.common.enums.DocumentStatus;
import com.marsreg.common.model.BaseDocument;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "documents")
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "documents")
public class DocumentEntity extends BaseDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    @Column(name = "name")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    @Column(name = "original_name")
    private String originalName;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "content_type")
    private String contentType;
    
    @Field(type = FieldType.Long)
    @Column(name = "size")
    private Long size;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "object_name")
    private String objectName;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "storage_path")
    private String storagePath;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "bucket")
    private String bucket;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "md5")
    private String md5;
    
    @Field(type = FieldType.Keyword)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DocumentStatus status;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "category")
    private String category;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "tags")
    private String tags;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Field(type = FieldType.Text)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<DocumentChunk> chunks;
    
    @Field(type = FieldType.Dense_Vector, dims = 384)
    @Column(name = "vector", columnDefinition = "TEXT")
    private float[] vector;
    
    @Field(type = FieldType.Date)
    @Column(name = "processed_time")
    private LocalDateTime processedTime;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    public LocalDateTime getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(LocalDateTime processedTime) {
        this.processedTime = processedTime;
    }
} 