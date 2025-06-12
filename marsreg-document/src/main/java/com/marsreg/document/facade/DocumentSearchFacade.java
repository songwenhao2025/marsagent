package com.marsreg.document.facade;

import com.marsreg.document.dto.DocumentMetadataDTO;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DocumentSearchFacade {
    
    public List<DocumentMetadataDTO> searchDocuments(String query) {
        // TODO: 实现搜索逻辑
        return List.of();
    }
    
    public List<DocumentMetadataDTO> searchByVector(String query) {
        // TODO: 实现向量搜索逻辑
        return List.of();
    }
    
    public List<DocumentMetadataDTO> hybridSearch(String query) {
        // TODO: 实现混合搜索逻辑
        return List.of();
    }
} 