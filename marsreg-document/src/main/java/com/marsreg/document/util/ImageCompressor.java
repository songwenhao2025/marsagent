package com.marsreg.document.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class ImageCompressor {

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final float QUALITY = 0.8f;

    public static InputStream compress(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return file.getInputStream();
        }

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            return file.getInputStream();
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算新的尺寸
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > MAX_WIDTH) {
            newWidth = MAX_WIDTH;
            newHeight = (int) ((float) originalHeight * MAX_WIDTH / originalWidth);
        }

        if (newHeight > MAX_HEIGHT) {
            newHeight = MAX_HEIGHT;
            newWidth = (int) ((float) newWidth * MAX_HEIGHT / newHeight);
        }

        // 如果尺寸没有变化，直接返回原图
        if (newWidth == originalWidth && newHeight == originalHeight) {
            return file.getInputStream();
        }

        // 创建新图片
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // 输出压缩后的图片
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String formatName = contentType.substring(contentType.lastIndexOf("/") + 1);
        ImageIO.write(resizedImage, formatName, outputStream);

        return new ByteArrayInputStream(outputStream.toByteArray());
    }
} 