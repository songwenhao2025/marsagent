package com.marsreg.document.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "document_content")
public class DocumentContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "cleaned_text", columnDefinition = "TEXT")
    private String cleanedText;

    @Column(name = "language")
    private String language;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "paragraph_count")
    private Integer paragraphCount;

    @Column(name = "chunks", columnDefinition = "TEXT")
    private String chunks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
} 