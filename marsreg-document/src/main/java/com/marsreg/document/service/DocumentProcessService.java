package com.marsreg.document.service;

import com.marsreg.document.entity.Document;
import java.io.InputStream;
import java.util.List;

public interface DocumentProcessService {
    /**
     * 处理文档
     * @param document 文档对象
     * @return 处理后的文本内容
     */
    String processDocument(Document document);

    /**
     * 提取文本
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 提取的文本内容
     */
    String extractText(InputStream inputStream, String fileName);

    /**
     * 清洗文本
     * @param text 原始文本
     * @return 清洗后的文本
     */
    String cleanText(String text);

    /**
     * 检测语言
     * @param text 文本内容
     * @return 语言代码（如：zh-CN, en-US）
     */
    String detectLanguage(String text);

    /**
     * 基本分块
     * @param text 文本内容
     * @param chunkSize 分块大小
     * @param overlapSize 重叠大小
     * @return 分块列表
     */
    List<String> chunkText(String text, int chunkSize, int overlapSize);

    /**
     * 智能分块
     * @param text 文本内容
     * @param maxChunkSize 最大分块大小
     * @param minChunkSize 最小分块大小
     * @return 分块列表
     */
    List<String> smartChunkText(String text, int maxChunkSize, int minChunkSize);

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
} 