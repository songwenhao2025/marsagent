package com.marsreg.document.service.impl;

import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentContent;
import com.marsreg.document.repository.DocumentContentRepository;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentProcessService;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentProcessService documentProcessService;

    @Override
    @Transactional
    public Document upload(MultipartFile file) {
        // 上传到存储服务
        Document document = documentStorageService.upload(file);
        // 保存到数据库
        document = documentRepository.save(document);
        // 处理文档
        processDocument(document);
        return document;
    }

    @Override
    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在"));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Document document = getDocument(id);
        // 删除文档内容
        documentContentRepository.findByDocumentId(id).ifPresent(documentContentRepository::delete);
        // 从存储服务删除
        documentStorageService.delete(document);
        // 从数据库删除
        documentRepository.delete(document);
    }

    @Override
    public Page<Document> list(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    @Override
    public String getDocumentUrl(Long id, int expirySeconds) {
        Document document = getDocument(id);
        return documentStorageService.getDocumentUrl(document, expirySeconds);
    }

    private void processDocument(Document document) {
        try {
            // 提取文本
            String text = documentProcessService.extractText(document);
            
            // 清洗文本
            String cleanedText = documentProcessService.cleanText(text);
            
            // 检测语言
            String language = documentProcessService.detectLanguage(cleanedText);
            
            // 智能分块
            List<String> chunks = documentProcessService.smartChunkText(cleanedText, 1000, 200);
            
            // 创建文档内容
            DocumentContent content = new DocumentContent();
            content.setDocumentId(document.getId());
            content.setOriginalText(text);
            content.setCleanedText(cleanedText);
            content.setLanguage(language);
            content.setWordCount(countWords(cleanedText));
            content.setParagraphCount(countParagraphs(cleanedText));
            content.setChunks(chunks);
            
            // 保存文档内容
            documentContentRepository.save(content);
            
            // 更新文档状态
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
        } catch (Exception e) {
            log.error("文档处理失败", e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            throw new BusinessException("文档处理失败: " + e.getMessage());
        }
    }

    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    private int countParagraphs(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\n\\s*\n").length;
    }
} 