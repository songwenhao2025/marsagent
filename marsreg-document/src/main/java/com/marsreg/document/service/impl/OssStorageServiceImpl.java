package com.marsreg.document.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.marsreg.document.annotation.RateLimit;
import com.marsreg.document.entity.Document;
import com.marsreg.document.metrics.UploadMetrics;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.upload.DefaultUploadProgressListener;
import com.marsreg.document.upload.UploadProgressListener;
import com.marsreg.document.util.ImageCompressor;
import com.marsreg.document.validation.FileValidator;
import lombok.extern.slf4j.Slf4j;
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

    private final OSS ossClient;
    private final FileValidator fileValidator;
    private final UploadMetrics uploadMetrics;
    
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    
    @Value("${aliyun.oss.urlExpiration}")
    private int urlExpiration;
    
    public OssStorageServiceImpl(OSS ossClient, FileValidator fileValidator, UploadMetrics uploadMetrics) {
        this.ossClient = ossClient;
        this.fileValidator = fileValidator;
        this.uploadMetrics = uploadMetrics;
    }

    @Override
    @RateLimit(limit = 5)  // 每分钟最多5次上传
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Document upload(MultipartFile file) {
        uploadMetrics.recordUploadAttempt();
        long startTime = System.currentTimeMillis();
        
        try {
            fileValidator.validate(file);
            
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            
            // 如果是图片，进行压缩
            InputStream inputStream;
            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                inputStream = ImageCompressor.compress(file);
                metadata.setContentLength(inputStream.available());
            } else {
                inputStream = file.getInputStream();
                metadata.setContentLength(file.getSize());
            }
            
            // 创建进度监听器
            UploadProgressListener progressListener = new DefaultUploadProgressListener(originalFilename);
            
            try (inputStream) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream, metadata);
                putObjectRequest.setProgressListener(new com.aliyun.oss.event.ProgressListener() {
                    @Override
                    public void progressChanged(com.aliyun.oss.event.ProgressEvent progressEvent) {
                        if (progressEvent.getEventType() == com.aliyun.oss.event.ProgressEventType.TRANSFER_STARTED_EVENT) {
                            progressListener.onProgress(0, file.getSize());
                        } else if (progressEvent.getEventType() == com.aliyun.oss.event.ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                            progressListener.onProgress(file.getSize(), file.getSize());
                        } else if (progressEvent.getEventType() == com.aliyun.oss.event.ProgressEventType.TRANSFER_FAILED_EVENT) {
                            progressListener.onProgress(0, file.getSize());
                        }
                    }
                });
                
                ossClient.putObject(putObjectRequest);
                progressListener.onSuccess();
            }
            
            Document document = new Document();
            document.setName(UUID.randomUUID().toString());
            document.setOriginalName(originalFilename);
            document.setContentType(file.getContentType());
            document.setSize(metadata.getContentLength());
            document.setStoragePath(objectName);
            document.setBucket(bucketName);
            document.setObjectName(objectName);
            
            uploadMetrics.recordUploadSuccess();
            uploadMetrics.recordUploadTime(System.currentTimeMillis() - startTime);
            uploadMetrics.recordUploadSize(metadata.getContentLength());
            
            return document;
        } catch (IOException e) {
            uploadMetrics.recordUploadFailure();
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public InputStream getDocument(Document document) {
        try {
            return ossClient.getObject(bucketName, document.getObjectName()).getObjectContent();
        } catch (Exception e) {
            log.error("获取文件失败: {}", document.getObjectName(), e);
            throw new RuntimeException("获取文件失败", e);
        }
    }

    @Override
    public void delete(Document document) {
        try {
            ossClient.deleteObject(bucketName, document.getObjectName());
        } catch (Exception e) {
            log.error("删除文件失败: {}", document.getObjectName(), e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    @Override
    public String getDocumentUrl(Document document, int expirySeconds) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expirySeconds * 1000L);
            return ossClient.generatePresignedUrl(bucketName, document.getObjectName(), expiration).toString();
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", document.getObjectName(), e);
            throw new RuntimeException("获取文件URL失败", e);
        }
    }

    @Override
    public void storeFile(MultipartFile file, String objectName) {
        fileValidator.validate(file);
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            
            // 如果是图片，进行压缩
            InputStream inputStream;
            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                inputStream = ImageCompressor.compress(file);
                metadata.setContentLength(inputStream.available());
            } else {
                inputStream = file.getInputStream();
                metadata.setContentLength(file.getSize());
            }
            
            try (inputStream) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream, metadata);
                ossClient.putObject(putObjectRequest);
            }
            
            log.info("文件上传成功: bucket={}, object={}", bucketName, objectName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void deleteFile(String bucket, String objectName) {
        try {
            ossClient.deleteObject(bucket, objectName);
            log.info("文件删除成功: bucket={}, object={}", bucket, objectName);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public String getFileUrl(String bucket, String objectName, int expirySeconds) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expirySeconds * 1000L);
            return ossClient.generatePresignedUrl(bucket, objectName, expiration).toString();
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            throw new RuntimeException("获取文件URL失败", e);
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }
} 