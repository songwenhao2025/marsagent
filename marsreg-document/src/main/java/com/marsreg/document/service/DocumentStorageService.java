package com.marsreg.document.service;

import com.marsreg.document.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DocumentStorageService {
    /**
     * 上传文档
     * @param file 文件
     * @return 文档信息
     */
    Document upload(MultipartFile file);

    /**
     * 获取文档
     * @param document 文档信息
     * @return 文档输入流
     */
    InputStream getDocument(Document document);

    /**
     * 删除文档
     * @param document 文档信息
     */
    void delete(Document document);

    /**
     * 获取文档URL
     * @param document 文档信息
     * @param expirySeconds 过期时间（秒）
     * @return 文档URL
     */
    String getDocumentUrl(Document document, int expirySeconds);
} 