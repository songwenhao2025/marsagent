package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentVersionService {
    
    DocumentVersion createVersion(Long documentId, String comment, String createdBy);
    
    DocumentVersion getVersion(Long versionId);
    
    List<DocumentVersion> getVersionsByDocumentId(Long documentId);
    
    Page<DocumentVersion> getVersionsByDocumentId(Long documentId, Pageable pageable);
    
    void deleteVersion(Long versionId);
    
    DocumentVersion restoreVersion(Long versionId);
    
    String getVersionUrl(Long versionId, int expirySeconds);
} 