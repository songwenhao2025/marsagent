package com.marsreg.document.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Slf4j
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private DocumentEntity document;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "vector", columnDefinition = "TEXT")
    private String vector;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "language")
    private String language;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
} 