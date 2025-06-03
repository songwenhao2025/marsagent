package com.marsreg.document.service;

import com.marsreg.document.dto.DocumentDTO;
import com.marsreg.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    /**
     * 上传文档
     * @param file 文件
     * @return 文档信息
     */
    DocumentDTO uploadDocument(MultipartFile file);

    /**
     * 获取文档
     * @param id 文档ID
     * @return 文档信息
     */
    Document getDocument(Long id);

    /**
     * 删除文档
     * @param id 文档ID
     */
    void deleteDocument(Long id);

    /**
     * 获取文档内容
     * @param id 文档ID
     * @return 文档内容
     */
    String getContent(Long id);

    /**
     * 分页查询文档
     * @param pageable 分页参数
     * @return 文档分页信息
     */
    Page<Document> listDocuments(Pageable pageable);

    /**
     * 获取文档URL
     * @param id 文档ID
     * @param expirySeconds 过期时间（秒）
     * @return 文档URL
     */
    String getDocumentUrl(Long id, int expirySeconds);
} 