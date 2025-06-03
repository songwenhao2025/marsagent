package com.marsreg.document.upload;

public interface UploadProgressListener {
    void onProgress(long bytesTransferred, long totalBytes);
    void onSuccess();
    void onFailure(Exception e);
} 