package com.marsreg.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "document_versions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentVersion extends BaseEntity {

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String objectName;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private String originalName;

    @Column
    private String comment;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;
} 