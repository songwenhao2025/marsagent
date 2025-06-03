package com.marsreg.document.validation;

import com.marsreg.document.exception.FileValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class FileValidator {

    @Value("${aliyun.oss.maxFileSize}")
    private long maxFileSize;

    @Value("${aliyun.oss.allowedFileTypes}")
    private String allowedFileTypes;

    public void validate(MultipartFile file) {
        validateFileSize(file);
        validateFileType(file);
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            log.error("文件大小超过限制: {} > {}", file.getSize(), maxFileSize);
            throw new FileValidationException("文件大小超过限制");
        }
    }

    private void validateFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new FileValidationException("文件名不能为空");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedTypes = Arrays.asList(allowedFileTypes.split(","));

        if (!allowedTypes.contains(extension)) {
            log.error("不支持的文件类型: {}", extension);
            throw new FileValidationException("不支持的文件类型");
        }
    }
} 