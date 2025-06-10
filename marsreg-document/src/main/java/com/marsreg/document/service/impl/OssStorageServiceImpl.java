package com.marsreg.document.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.validation.FileValidator;
import com.marsreg.document.metrics.UploadMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class OssStorageServiceImpl implements DocumentStorageService {

    @Autowired
    private OSS ossClient;

    private final FileValidator fileValidator;
    private final UploadMetrics uploadMetrics;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.urlExpiration}")
    private int urlExpiration;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    public OssStorageServiceImpl(FileValidator fileValidator, UploadMetrics uploadMetrics) {
        this.fileValidator = fileValidator;
        this.uploadMetrics = uploadMetrics;
    }

    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public DocumentEntity upload(MultipartFile file) throws IOException {
        try {
            // 验证文件
            fileValidator.validate(file);

            // 生成文件名
            String objectName = generateObjectName(file);

            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, file.getInputStream());
            ossClient.putObject(putObjectRequest);

            // 创建文档实体
            DocumentEntity document = new DocumentEntity();
            document.setName(file.getOriginalFilename());
            document.setOriginalName(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setObjectName(objectName);
            document.setStoragePath(endpoint + "/" + bucketName + "/" + objectName);
            document.setBucket(bucketName);

            // 记录上传指标
            uploadMetrics.recordUploadSuccess();
            uploadMetrics.recordUploadSize(file.getSize());
            uploadMetrics.recordFileType(file.getContentType());

            return document;
        } catch (Exception e) {
            uploadMetrics.recordUploadFailure();
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public InputStream getDocument(DocumentEntity document) {
        try {
            return ossClient.getObject(bucketName, document.getObjectName()).getObjectContent();
        } catch (Exception e) {
            log.error("获取文档失败", e);
            throw new RuntimeException("获取文档失败", e);
        }
    }

    @Override
    public void deleteDocument(DocumentEntity document) {
        try {
            ossClient.deleteObject(bucketName, document.getObjectName());
        } catch (Exception e) {
            log.error("删除文档失败", e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    @Override
    public String getDocumentUrl(DocumentEntity document) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + urlExpiration * 1000L);
            return ossClient.generatePresignedUrl(bucketName, document.getObjectName(), expiration).toString();
        } catch (Exception e) {
            log.error("生成文档URL失败", e);
            throw new RuntimeException("生成文档URL失败", e);
        }
    }

    @Override
    public DocumentEntity saveDocument(MultipartFile file) throws IOException {
        return upload(file);
    }

    @Override
    public void deleteDocument(String objectName) {
        try {
            ossClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    @Override
    public String getDocumentUrl(String objectName) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + urlExpiration * 1000L);
            return ossClient.generatePresignedUrl(bucketName, objectName, expiration).toString();
        } catch (Exception e) {
            log.error("生成文档URL失败", e);
            throw new RuntimeException("生成文档URL失败", e);
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            ossClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    @Override
    public String getFileUrl(String bucketName, String objectName, int expiration) {
        try {
            Date expirationDate = new Date(System.currentTimeMillis() + expiration * 1000L);
            return ossClient.generatePresignedUrl(bucketName, objectName, expirationDate).toString();
        } catch (Exception e) {
            log.error("生成文件URL失败", e);
            throw new RuntimeException("生成文件URL失败", e);
        }
    }

    @Override
    public DocumentEntity storeFile(MultipartFile file, String objectName) throws IOException {
        try {
            // 验证文件
            fileValidator.validate(file);

            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, file.getInputStream());
            ossClient.putObject(putObjectRequest);

            // 创建文档实体
            DocumentEntity document = new DocumentEntity();
            document.setName(file.getOriginalFilename());
            document.setOriginalName(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setObjectName(objectName);
            document.setStoragePath(endpoint + "/" + bucketName + "/" + objectName);
            document.setBucket(bucketName);

            // 记录上传指标
            uploadMetrics.recordUploadSuccess();
            uploadMetrics.recordUploadSize(file.getSize());
            uploadMetrics.recordFileType(file.getContentType());

            return document;
        } catch (Exception e) {
            uploadMetrics.recordUploadFailure();
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void saveDocument(DocumentEntity document, InputStream inputStream) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, document.getObjectName(), inputStream);
            ossClient.putObject(putObjectRequest);
            uploadMetrics.recordUploadSuccess();
        } catch (Exception e) {
            uploadMetrics.recordUploadFailure();
            log.error("保存文档失败", e);
            throw new RuntimeException("保存文档失败", e);
        }
    }

    @Override
    public String getBucket() {
        return bucketName;
    }

    @Override
    public String getStoragePath() {
        return endpoint + "/" + bucketName;
    }

    @Override
    public String storeFile(MultipartFile file) {
        try {
            return generateObjectName(file);
        } catch (Exception e) {
            log.error("生成文件名失败", e);
            throw new RuntimeException("生成文件名失败", e);
        }
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        try {
            ossClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    private String generateObjectName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID().toString() + extension;
    }
} 