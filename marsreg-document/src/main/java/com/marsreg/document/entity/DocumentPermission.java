package com.marsreg.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Entity
@Table(name = "document_permissions")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentPermission extends BaseEntity {

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String permission; // READ, WRITE, ADMIN

    @Column(nullable = false)
    private String grantedBy;

    @Column(nullable = false)
    private Boolean inherited;
} 