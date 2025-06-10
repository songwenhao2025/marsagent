package com.marsreg.document.service;

import com.marsreg.document.dto.DocumentDTO;
import com.marsreg.document.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 文档服务接口
 * 负责文档的基本操作，不包含核心业务逻辑
 */
public interface DocumentService {
    /**
     * 创建文档
     * @param file 文件
     * @param metadata 元数据
     * @return 文档DTO
     */
    DocumentDTO createDocument(MultipartFile file, DocumentDTO metadata);
    
    /**
     * 更新文档
     * @param id 文档ID
     * @param metadata 元数据
     * @return 更新后的文档DTO
     */
    DocumentDTO updateDocument(Long id, DocumentDTO metadata);
    
    /**
     * 删除文档
     * @param id 文档ID
     */
    void deleteDocument(Long id);
    
    /**
     * 获取文档
     * @param id 文档ID
     * @return 文档DTO
     */
    Optional<DocumentDTO> getDocument(Long id);
    
    /**
     * 获取文档列表
     * @param pageable 分页参数
     * @return 文档DTO列表
     */
    List<DocumentDTO> listDocuments(Pageable pageable);
    
    /**
     * 获取文档实体
     * @param id 文档ID
     * @return 文档实体
     */
    Optional<DocumentEntity> getDocumentEntity(Long id);

    /**
     * 保存文档
     *
     * @param document 文档实体
     * @return 保存后的文档实体
     */
    DocumentEntity save(DocumentEntity document);

    /**
     * 上传文档
     *
     * @param file 文件
     * @return 文档实体
     * @throws IOException IO异常
     */
    DocumentEntity upload(MultipartFile file) throws IOException;

    /**
     * 批量上传文档
     *
     * @param files 文件列表
     * @return 文档实体列表
     * @throws IOException IO异常
     */
    List<DocumentEntity> batchUpload(List<MultipartFile> files) throws IOException;

    /**
     * 根据ID获取文档
     *
     * @param id 文档ID
     * @return 文档实体
     */
    DocumentEntity getById(Long id);

    /**
     * 批量删除文档
     *
     * @param ids 文档ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 分页查询文档
     *
     * @param pageable 分页参数
     * @return 文档实体分页结果
     */
    Page<DocumentEntity> page(Pageable pageable);

    /**
     * 获取文档URL
     *
     * @param id 文档ID
     * @param expiration 过期时间（秒）
     * @return 文档URL
     */
    String getDocumentUrl(Long id, int expiration);

    /**
     * 获取文档内容
     *
     * @param id 文档ID
     * @return 文档内容
     */
    String getContent(Long id);
} 