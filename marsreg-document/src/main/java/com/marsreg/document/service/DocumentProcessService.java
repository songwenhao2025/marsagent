package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import java.io.InputStream;
import java.util.List;

public interface DocumentProcessService {
    /**
     * 处理文档
     * @param document 文档实体
     * @return 处理后的文档实体
     */
    DocumentEntity process(DocumentEntity document);

    /**
     * 批量处理文档
     * @param documents 文档实体列表
     */
    void processBatch(List<DocumentEntity> documents);

    /**
     * 处理文档并返回文本内容
     * @param document 文档对象
     * @return 处理后的文本内容
     */
    String processDocument(DocumentEntity document);

    /**
     * 提取文档文本
     * @param document 文档实体
     * @return 提取的文本
     */
    String extractText(DocumentEntity document);

    /**
     * 提取文本
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 提取的文本内容
     */
    String extractText(InputStream inputStream, String fileName);

    /**
     * 清洗文本
     * @param text 输入文本
     * @return 清洗后的文本
     */
    String cleanText(String text);

    /**
     * 检测文本语言
     * @param text 输入文本
     * @return 语言代码
     */
    String detectLanguage(String text);

    /**
     * 文本分块
     * @param text 输入文本
     * @param maxChunkSize 最大块大小
     * @param overlap 重叠大小
     * @return 文本块列表
     */
    List<String> chunkText(String text, int maxChunkSize, int overlap);

    /**
     * 智能文本分块
     * @param text 输入文本
     * @param maxChunkSize 最大块大小
     * @param overlap 重叠大小
     * @return 文本块列表
     */
    List<String> smartChunkText(String text, int maxChunkSize, int overlap);

    /**
     * 保存文档分块
     * @param documentId 文档ID
     * @param chunks 分块列表
     * @param language 语言
     */
    void saveChunks(Long documentId, List<String> chunks, String language);

    /**
     * 获取文档分块
     * @param documentId 文档ID
     * @return 分块列表
     */
    List<String> getChunks(Long documentId);

    /**
     * 清除文档分块缓存
     * @param documentId 文档ID
     */
    void clearChunksCache(Long documentId);

    /**
     * 清除文档缓存
     * @param documentId 文档ID
     */
    void clearDocumentCache(Long documentId);
} 