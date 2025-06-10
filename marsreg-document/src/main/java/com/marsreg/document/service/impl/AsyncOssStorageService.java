package com.marsreg.document.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.metrics.UploadMetrics;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.upload.DefaultUploadProgressListener;
import com.marsreg.document.upload.UploadProgressListener;
import com.marsreg.document.util.ImageCompressor;
import com.marsreg.document.validation.FileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AsyncOssStorageService {

    private final OSS ossClient;
    private final FileValidator fileValidator;
    private final UploadMetrics uploadMetrics;
    private final DocumentStorageService documentStorageService;
    
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    
    public AsyncOssStorageService(OSS ossClient, FileValidator fileValidator, 
                                UploadMetrics uploadMetrics, DocumentStorageService documentStorageService) {
        this.ossClient = ossClient;
        this.fileValidator = fileValidator;
        this.uploadMetrics = uploadMetrics;
        this.documentStorageService = documentStorageService;
    }

    @Async
    public CompletableFuture<DocumentEntity> uploadAsync(MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
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
                long originalSize = file.getSize();
                if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                    inputStream = ImageCompressor.compress(file);
                    metadata.setContentLength(inputStream.available());
                    uploadMetrics.recordCompression(originalSize, inputStream.available());
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
                
                DocumentEntity document = new DocumentEntity();
                document.setName(UUID.randomUUID().toString());
                document.setOriginalName(originalFilename);
                document.setContentType(file.getContentType());
                document.setSize(metadata.getContentLength());
                // document.setStoragePath(objectName);
                // document.setBucket(bucketName);
                document.setObjectName(objectName);
                
                uploadMetrics.recordUploadSuccess();
                uploadMetrics.recordUploadTime(System.currentTimeMillis() - startTime);
                uploadMetrics.recordUploadSize(metadata.getContentLength());
                uploadMetrics.recordFileType(file.getContentType());
                
                return document;
            } catch (IOException e) {
                uploadMetrics.recordUploadFailure();
                log.error("文件上传失败", e);
                throw new RuntimeException("文件上传失败", e);
            }
        });
    }

    @Async
    public void asyncSaveDocument(DocumentEntity document, InputStream inputStream) {
        documentStorageService.saveDocument(document, inputStream);
    }

    @Async
    public void asyncDeleteDocument(DocumentEntity document) {
        documentStorageService.deleteDocument(document);
    }
} 