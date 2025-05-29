package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentTagService {
    
    DocumentTag createTag(Long documentId, String name, String description, String color, String createdBy);
    
    DocumentTag updateTag(Long tagId, String name, String description, String color);
    
    void deleteTag(Long tagId);
    
    List<DocumentTag> getTagsByDocumentId(Long documentId);
    
    Page<DocumentTag> getTagsByDocumentId(Long documentId, Pageable pageable);
    
    List<DocumentTag> searchTags(String keyword);
    
    void addTagToDocument(Long documentId, Long tagId);
    
    void removeTagFromDocument(Long documentId, Long tagId);
} 