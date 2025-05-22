package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentContentRepository extends JpaRepository<DocumentContent, Long> {
    Optional<DocumentContent> findByDocumentId(Long documentId);
} 