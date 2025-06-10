package com.marsreg.document.service.impl;

import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.service.DocumentSearchService;
import com.marsreg.document.service.DocumentVectorService;
import com.marsreg.document.service.RagSystemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RagSystemServiceImpl implements RagSystemService {

    private final DocumentSearchService documentSearchService;
    private final DocumentVectorService documentVectorService;

    public RagSystemServiceImpl(DocumentSearchService documentSearchService, DocumentVectorService documentVectorService) {
        this.documentSearchService = documentSearchService;
        this.documentVectorService = documentVectorService;
    }

    @Override
    public Page<DocumentEntity> search(String query, Pageable pageable) {
        return documentSearchService.search(query, pageable);
    }

    @Override
    public String generateResponse(String query) {
        // 搜索相关文档
        Page<DocumentEntity> searchResults = documentSearchService.search(query, Pageable.ofSize(5));
        if (searchResults.isEmpty()) {
            return "未找到相关文档";
        }

        // 获取文档内容
        StringBuilder context = new StringBuilder();
        for (DocumentEntity doc : searchResults) {
            context.append(doc.getContent()).append("\n");
        }

        // 生成响应
        return "根据搜索结果，我为您找到以下信息：\n" + context.toString();
    }
} 