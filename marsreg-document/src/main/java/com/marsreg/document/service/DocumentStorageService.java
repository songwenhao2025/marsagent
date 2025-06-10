package com.marsreg.document.service;

import com.marsreg.document.entity.DocumentEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档存储服务接口
 */
public interface DocumentStorageService {
    /**
     * 保存文档
     *
     * @param file 文件
     * @return 文档实体
     * @throws IOException IO异常
     */
    DocumentEntity saveDocument(MultipartFile file) throws IOException;

    /**
     * 删除文档
     *
     * @param objectName 对象名称
     */
    void deleteDocument(String objectName);

    /**
     * 获取文档URL
     *
     * @param objectName 对象名称
     * @return 文档URL
     */
    String getDocumentUrl(String objectName);

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    String getBucketName();

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文档实体
     * @throws IOException IO异常
     */
    DocumentEntity upload(MultipartFile file) throws IOException;

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    void deleteFile(String bucketName, String objectName);

    /**
     * 获取文件URL
     *
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param expiration 过期时间（秒）
     * @return 文件URL
     */
    String getFileUrl(String bucketName, String objectName, int expiration);

    /**
     * 存储文件
     *
     * @param file 文件
     * @param objectName 对象名称
     * @return 文档实体
     * @throws IOException IO异常
     */
    DocumentEntity storeFile(MultipartFile file, String objectName) throws IOException;

    /**
     * 获取文档
     *
     * @param document 文档实体
     * @return 文档输入流
     */
    InputStream getDocument(DocumentEntity document);

    /**
     * 保存文档
     *
     * @param document 文档实体
     * @param inputStream 输入流
     */
    void saveDocument(DocumentEntity document, InputStream inputStream);

    /**
     * 删除文档
     *
     * @param document 文档实体
     */
    void deleteDocument(DocumentEntity document);

    /**
     * 获取文档URL
     *
     * @param document 文档实体
     * @return 文档URL
     */
    String getDocumentUrl(DocumentEntity document);

    /**
     * 存储文件
     * @param file 文件
     * @return 对象名称
     */
    String storeFile(MultipartFile file);
    
    /**
     * 删除文件
     * @param objectName 对象名称
     */
    void deleteFile(String objectName);
    
    
    /**
     * 获取存储桶名称
     * @return 存储桶名称
     */
    String getBucket();
    
    /**
     * 获取存储路径
     * @return 存储路径
     */
    String getStoragePath();
} 