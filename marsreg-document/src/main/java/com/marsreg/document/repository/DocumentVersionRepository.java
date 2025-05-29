package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    
    List<DocumentVersion> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    
    Page<DocumentVersion> findByDocumentId(Long documentId, Pageable pageable);
} 