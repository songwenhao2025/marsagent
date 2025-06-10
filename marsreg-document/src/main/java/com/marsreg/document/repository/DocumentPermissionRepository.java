package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    
    List<DocumentPermission> findByDocumentId(Long documentId);
    
    Page<DocumentPermission> findByDocumentId(Long documentId, Pageable pageable);
    
    List<DocumentPermission> findByUserId(String userId);
    
    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, String userId);
    
    boolean existsByDocumentIdAndUserIdAndPermission(Long documentId, String userId, String permission);
    
    void deleteByDocumentIdAndUserId(Long documentId, String userId);
    
    void deleteByDocumentIdAndIsInheritedTrue(Long documentId);
} 