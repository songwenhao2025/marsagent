package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DocumentPermissionService {
    
    DocumentPermission grantPermission(Long documentId, String userId, String permission, String grantedBy);
    
    void revokePermission(Long documentId, String userId);
    
    void updatePermission(Long documentId, String userId, String permission);
    
    List<DocumentPermission> getPermissionsByDocumentId(Long documentId);
    
    Page<DocumentPermission> getPermissionsByDocumentId(Long documentId, Pageable pageable);
    
    List<DocumentPermission> getPermissionsByUserId(String userId);
    
    boolean hasPermission(Long documentId, String userId, String permission);
    
    void inheritPermissions(Long documentId, Long parentDocumentId);
    
    void removeInheritedPermissions(Long documentId);
} 