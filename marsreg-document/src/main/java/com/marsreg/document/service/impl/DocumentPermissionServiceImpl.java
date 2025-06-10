package com.marsreg.document.service.impl;

import com.marsreg.common.annotation.Log;
import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentPermission;
import com.marsreg.document.repository.DocumentPermissionRepository;
import com.marsreg.document.service.DocumentPermissionService;
import com.marsreg.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPermissionServiceImpl implements DocumentPermissionService {

    private final DocumentPermissionRepository permissionRepository;
    private final DocumentService documentService;

    @Override
    @Log(module = "文档权限", operation = "授权", description = "授予文档权限")
    @Transactional
    public DocumentPermission grantPermission(Long documentId, String userId, String permission, String grantedBy) {
        DocumentEntity document = documentService.getDocumentEntity(documentId)
            .orElseThrow(() -> new BusinessException("文档不存在"));

        DocumentPermission documentPermission = DocumentPermission.builder()
            .documentId(documentId)
            .userId(userId)
            .permission(permission)
            .grantedBy(grantedBy)
            .grantedAt(LocalDateTime.now())
            .build();

        return permissionRepository.save(documentPermission);
    }

    @Override
    @Log(module = "文档权限", operation = "撤销", description = "撤销文档权限")
    @Transactional
    public void revokePermission(Long documentId, String userId) {
        permissionRepository.deleteByDocumentIdAndUserId(documentId, userId);
    }

    @Override
    @Log(module = "文档权限", operation = "更新", description = "更新文档权限")
    @Transactional
    public void updatePermission(Long documentId, String userId, String permission) {
        DocumentPermission documentPermission = permissionRepository.findByDocumentIdAndUserId(documentId, userId)
            .orElseThrow(() -> new BusinessException("权限记录不存在"));
        
        documentPermission.setPermission(permission);
        permissionRepository.save(documentPermission);
    }

    @Override
    @Log(module = "文档权限", operation = "查询", description = "查询文档权限列表")
    public List<DocumentPermission> getPermissionsByDocumentId(Long documentId) {
        return permissionRepository.findByDocumentId(documentId);
    }

    @Override
    @Log(module = "文档权限", operation = "查询", description = "分页查询文档权限")
    public Page<DocumentPermission> getPermissionsByDocumentId(Long documentId, Pageable pageable) {
        return permissionRepository.findByDocumentId(documentId, pageable);
    }

    @Override
    @Log(module = "文档权限", operation = "查询", description = "查询用户权限列表")
    public List<DocumentPermission> getPermissionsByUserId(String userId) {
        return permissionRepository.findByUserId(userId);
    }

    @Override
    @Log(module = "文档权限", operation = "检查", description = "检查文档权限")
    public boolean hasPermission(Long documentId, String userId, String permission) {
        return permissionRepository.existsByDocumentIdAndUserIdAndPermission(documentId, userId, permission);
    }

    @Override
    @Log(module = "文档权限", operation = "继承", description = "继承文档权限")
    @Transactional
    public void inheritPermissions(Long documentId, Long parentDocumentId) {
        List<DocumentPermission> parentPermissions = getPermissionsByDocumentId(parentDocumentId);
        
        for (DocumentPermission parentPermission : parentPermissions) {
            DocumentPermission newPermission = DocumentPermission.builder()
                .documentId(documentId)
                .userId(parentPermission.getUserId())
                .permission(parentPermission.getPermission())
                .grantedBy(parentPermission.getGrantedBy())
                .grantedAt(LocalDateTime.now())
                .isInherited(true)
                .build();
                
            permissionRepository.save(newPermission);
        }
    }

    @Override
    @Log(module = "文档权限", operation = "移除继承", description = "移除继承的文档权限")
    @Transactional
    public void removeInheritedPermissions(Long documentId) {
        permissionRepository.deleteByDocumentIdAndIsInheritedTrue(documentId);
    }
} 