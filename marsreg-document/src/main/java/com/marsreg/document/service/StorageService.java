package com.marsreg.document.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    /**
     * 上传文件
     * @param file 文件
     * @return 文件存储路径
     * @throws IOException 上传异常
     */
    String uploadFile(MultipartFile file) throws IOException;

    /**
     * 删除文件
     * @param objectName 文件存储路径
     */
    void deleteFile(String objectName);

    /**
     * 获取文件访问URL
     * @param objectName 文件存储路径
     * @return 文件访问URL
     */
    String getFileUrl(String objectName);
} 