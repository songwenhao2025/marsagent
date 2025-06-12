package com.marsreg.document.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class FileValidator {
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "text/markdown"
    };

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制");
        }

        String contentType = file.getContentType();
        boolean isAllowedType = false;
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                isAllowedType = true;
                break;
            }
        }

        if (!isAllowedType) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
    }
} 