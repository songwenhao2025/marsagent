package com.marsreg.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Entity
@Table(name = "document_categories")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentCategory extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column
    private Long parentId;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private Integer sort;

    @Column(nullable = false)
    private String createdBy;
} 