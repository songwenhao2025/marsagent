package com.marsreg.document.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String author;

    private String category;

    @Column(length = 500)
    private String tags;

    private String language;

    private String source;

    @Column(columnDefinition = "TEXT")
    private String customMetadata;

    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<DocumentChunk> chunks;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String contentType;

    private Long size;
} 