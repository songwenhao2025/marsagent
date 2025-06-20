package com.marsreg.document.config;

import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.service.impl.DocumentServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DocumentJpaConfig.class)
public class DocumentServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DocumentService documentService(DocumentRepository documentRepository, DocumentStorageService documentStorageService) {
        return new DocumentServiceImpl(documentRepository, documentStorageService);
    }
} 