package com.marsreg.document.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.metrics.UploadMetrics;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.upload.DefaultUploadProgressListener;
import com.marsreg.document.upload.UploadProgressListener;
import com.marsreg.document.util.ImageCompressor;
import com.marsreg.document.validation.FileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ResumableOssStorageService {

    private final OSS ossClient;
    private final FileValidator fileValidator;
    private final UploadMetrics uploadMetrics;
    private final DocumentStorageService documentStorageService;
    
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    
    @Value("${marsreg.document.upload.part-size:5242880}") // 默认5MB
    private long partSize;
    
    public ResumableOssStorageService(OSS ossClient, FileValidator fileValidator, 
                                    UploadMetrics uploadMetrics, DocumentStorageService documentStorageService) {
        this.ossClient = ossClient;
        this.fileValidator = fileValidator;
        this.uploadMetrics = uploadMetrics;
        this.documentStorageService = documentStorageService;
    }

    public DocumentEntity uploadWithResume(MultipartFile file) throws IOException {
        uploadMetrics.recordUploadAttempt();
        long startTime = System.currentTimeMillis();
        
        try {
            fileValidator.validate(file);
            
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;
            
            // 创建临时文件
            File tempFile = File.createTempFile("upload-", extension);
            file.transferTo(tempFile);
            
            // 初始化分片上传
            InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucketName, objectName);
            InitiateMultipartUploadResult initiateResult = ossClient.initiateMultipartUpload(initiateRequest);
            String uploadId = initiateResult.getUploadId();
            
            // 计算分片数量
            long fileLength = tempFile.length();
            int partCount = (int) Math.ceil((double) fileLength / partSize);
            List<PartETag> partETags = new ArrayList<>();
            
            // 创建进度监听器
            UploadProgressListener progressListener = new DefaultUploadProgressListener(originalFilename);
            
            // 上传分片
            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(objectName);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(new java.io.FileInputStream(tempFile));
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setProgressListener(new com.aliyun.oss.event.ProgressListener() {
                    @Override
                    public void progressChanged(com.aliyun.oss.event.ProgressEvent progressEvent) {
                        if (progressEvent.getEventType() == com.aliyun.oss.event.ProgressEventType.TRANSFER_STARTED_EVENT) {
                            progressListener.onProgress(startPos, fileLength);
                        } else if (progressEvent.getEventType() == com.aliyun.oss.event.ProgressEventType.TRANSFER_COMPLETED_EVENT) {
                            progressListener.onProgress(startPos + curPartSize, fileLength);
                        } else if (progressEvent.getEventType() == com.aliyun.oss.event.ProgressEventType.TRANSFER_FAILED_EVENT) {
                            progressListener.onProgress(startPos, fileLength);
                        }
                    }
                });
                
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResult.getPartETag());
            }
            
            // 完成分片上传
            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                bucketName, objectName, uploadId, partETags);
            ossClient.completeMultipartUpload(completeRequest);
            
            // 删除临时文件
            tempFile.delete();
            
            DocumentEntity document = new DocumentEntity();
            document.setName(UUID.randomUUID().toString());
            document.setOriginalName(originalFilename);
            document.setContentType(file.getContentType());
            document.setSize(fileLength);
            // document.setStoragePath(...);
            // document.setBucket(...);
            document.setObjectName(objectName);
            
            uploadMetrics.recordUploadSuccess();
            uploadMetrics.recordUploadTime(System.currentTimeMillis() - startTime);
            uploadMetrics.recordUploadSize(fileLength);
            uploadMetrics.recordFileType(file.getContentType());
            
            return document;
        } catch (Exception e) {
            uploadMetrics.recordUploadFailure();
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
} 