package com.marsreg.document.entity;

import com.marsreg.document.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "documents")
@Document(indexName = "documents")
@Slf4j
public class DocumentEntity {
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
    
    @Field(type = FieldType.Date)
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Date)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "created_by")
    private String createdBy;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Field(type = FieldType.Keyword)
    @Column(name = "category")
    private String category;
    
    @Field(type = FieldType.Keyword)
    @ElementCollection
    @ManyToMany
    @JoinTable(
        name = "document_tags",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<String> tags;
    
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

    public LocalDateTime getProcessedTime() {
        return processedTime;
    }

    public void setProcessedTime(LocalDateTime processedTime) {
        this.processedTime = processedTime;
    }
} 