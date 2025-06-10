package com.marsreg.document.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.service.DocumentStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

@Service
public class DocumentStorageServiceImpl implements DocumentStorageService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Override
    public DocumentEntity saveDocument(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String objectName = generateObjectName(originalFilename);

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            ossClient.putObject(bucketName, objectName, file.getInputStream(), metadata);

            DocumentEntity document = new DocumentEntity();
            document.setName(objectName);
            document.setOriginalName(originalFilename);
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setObjectName(objectName);
            document.setMd5(calculateMD5(file.getInputStream()));

            return document;
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void deleteDocument(String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.deleteObject(bucketName, objectName);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String getDocumentUrl(String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            Date expiration = new Date(System.currentTimeMillis() + 3600L * 1000);
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            return url.toString();
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public DocumentEntity upload(MultipartFile file) throws IOException {
        return saveDocument(file);
    }

    @Override
    public void deleteFile(String objectName) {
        deleteDocument(objectName);
    }
    @Override
    public void deleteFile(String bucketName, String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.deleteObject(bucketName, objectName);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String getFileUrl(String bucketName, String objectName, int expiration) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            Date expiryDate = new Date(System.currentTimeMillis() + expiration * 1000L);
            URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiryDate);
            return url.toString();
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public DocumentEntity storeFile(MultipartFile file, String objectName) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            ossClient.putObject(bucketName, objectName, file.getInputStream(), metadata);

            DocumentEntity document = new DocumentEntity();
            document.setName(objectName);
            document.setOriginalName(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setSize(file.getSize());
            document.setObjectName(objectName);
            document.setMd5(calculateMD5(file.getInputStream()));

            return document;
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public InputStream getDocument(DocumentEntity document) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            return ossClient.getObject(bucketName, document.getObjectName()).getObjectContent();
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void saveDocument(DocumentEntity document, InputStream inputStream) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(document.getContentType());
            metadata.setContentLength(document.getSize());

            ossClient.putObject(bucketName, document.getObjectName(), inputStream, metadata);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void deleteDocument(DocumentEntity document) {
        deleteDocument(document.getObjectName());
    }

    @Override
    public String getDocumentUrl(DocumentEntity document) {
        return getDocumentUrl(document.getObjectName());
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
            return generateObjectName(file.getOriginalFilename());
        } catch (Exception e) {
            throw new RuntimeException("生成文件名失败", e);
        }
    }

    private String generateObjectName(String originalFilename) {
        return System.currentTimeMillis() + "_" + originalFilename;
    }

    private String calculateMD5(InputStream inputStream) throws IOException {
        // 这里应该实现实际的MD5计算逻辑
        return "md5_" + System.currentTimeMillis();
    }
} 