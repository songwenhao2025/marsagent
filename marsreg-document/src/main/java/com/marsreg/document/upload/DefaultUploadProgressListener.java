package com.marsreg.document.upload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultUploadProgressListener implements UploadProgressListener {
    
    private final String fileName;
    private long lastProgress = 0;
    
    public DefaultUploadProgressListener(String fileName) {
        this.fileName = fileName;
    }
    
    @Override
    public void onProgress(long bytesTransferred, long totalBytes) {
        int progress = (int) (bytesTransferred * 100 / totalBytes);
        if (progress > lastProgress) {
            lastProgress = progress;
            log.info("文件 {} 上传进度: {}%", fileName, progress);
        }
    }
    
    @Override
    public void onSuccess() {
        log.info("文件 {} 上传完成", fileName);
    }
    
    @Override
    public void onFailure(Exception e) {
        log.error("文件 {} 上传失败: {}", fileName, e.getMessage());
    }
} 