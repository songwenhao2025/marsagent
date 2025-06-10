package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RagSystemService {
    /**
     * 搜索文档
     *
     * @param query 搜索关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<DocumentEntity> search(String query, Pageable pageable);

    /**
     * 生成响应
     *
     * @param query 用户查询
     * @return 生成的响应
     */
    String generateResponse(String query);
} 