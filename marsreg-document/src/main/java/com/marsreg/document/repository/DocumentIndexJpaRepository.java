package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentIndexJpaRepository extends JpaRepository<DocumentEntity, Long> {
    // 添加自定义方法以支持文档索引和检索
    DocumentEntity findByName(String name);
    DocumentEntity findByContentType(String contentType);
    DocumentEntity findByStatus(String status);
} 