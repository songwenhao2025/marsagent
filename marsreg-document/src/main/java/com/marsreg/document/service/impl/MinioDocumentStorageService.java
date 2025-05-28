package com.marsreg.document.service.impl;

import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentStatus;
import com.marsreg.document.service.DocumentStorageService;
import io.minio.*;
import io.minio.messages.DeleteObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class MinioDocumentStorageService implements DocumentStorageService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new BusinessException("初始化MinIO存储失败", e);
        }
    }

    @Override
    public Document upload(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long size = file.getSize();
            
            // 计算MD5
            String md5 = DigestUtils.md5Hex(file.getInputStream());
            
            // 生成存储路径
            String objectName = generateObjectName(originalFilename);
            
            // 上传到MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), size, -1)
                    .contentType(contentType)
                    .build());

            // 创建文档记录
            Document document = new Document();
            document.setName(UUID.randomUUID().toString());
            document.setOriginalName(originalFilename);
            document.setContentType(contentType);
            document.setSize(size);
            document.setStoragePath(objectName);
            document.setMd5(md5);
            document.setStatus(DocumentStatus.PENDING);
            document.setBucket(bucket);
            document.setObjectName(objectName);

            return document;
        } catch (Exception e) {
            throw new BusinessException("文件上传失败", e);
        }
    }

    @Override
    public InputStream getDocument(Document document) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(document.getBucket())
                    .object(document.getObjectName())
                    .build());
        } catch (Exception e) {
            throw new BusinessException("获取文件失败", e);
        }
    }

    @Override
    public void delete(Document document) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(document.getBucket())
                    .object(document.getObjectName())
                    .build());
        } catch (Exception e) {
            throw new BusinessException("删除文件失败", e);
        }
    }

    @Override
    public String getDocumentUrl(Document document, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(document.getBucket())
                    .object(document.getObjectName())
                    .expiry(expirySeconds)
                    .build());
        } catch (Exception e) {
            throw new BusinessException("获取文件URL失败", e);
        }
    }

    @Override
    public String getBucketName() {
        return bucket;
    }

    @Override
    public void storeFile(MultipartFile file, String objectName) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new BusinessException("存储文件失败", e);
        }
    }

    @Override
    public void deleteFile(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new BusinessException("删除文件失败", e);
        }
    }

    @Override
    public String getFileUrl(String bucket, String objectName, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expirySeconds)
                    .build());
        } catch (Exception e) {
            throw new BusinessException("获取文件URL失败", e);
        }
    }

    private String generateObjectName(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID().toString() + extension;
    }
} 