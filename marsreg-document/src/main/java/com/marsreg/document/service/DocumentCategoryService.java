package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentCategoryService {
    
    DocumentCategory createCategory(String name, String description, Long parentId, String createdBy);
    
    DocumentCategory updateCategory(Long categoryId, String name, String description);
    
    void deleteCategory(Long categoryId);
    
    DocumentCategory getCategory(Long categoryId);
    
    List<DocumentCategory> getRootCategories();
    
    List<DocumentCategory> getChildCategories(Long parentId);
    
    List<DocumentCategory> getAncestors(Long categoryId);
    
    List<DocumentCategory> getDescendants(Long categoryId);
    
    Page<DocumentCategory> searchCategories(String keyword, Pageable pageable);
    
    void moveCategory(Long categoryId, Long newParentId);
    
    void updateCategorySort(Long categoryId, Integer newSort);
} 