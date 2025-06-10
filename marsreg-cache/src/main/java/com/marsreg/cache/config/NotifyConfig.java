package com.marsreg.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "cache.notify")
public class NotifyConfig {
    
    /**
     * 是否启用通知
     */
    private boolean enabled = true;
    
    /**
     * 通知类型：log, email, webhook
     */
    @NotBlank(message = "通知类型不能为空")
    private String type = "log";
    
    /**
     * 邮件配置
     */
    @NotNull(message = "邮件配置不能为空")
    private EmailConfig email = new EmailConfig();
    
    /**
     * Webhook配置
     */
    @NotNull(message = "Webhook配置不能为空")
    private WebhookConfig webhook = new WebhookConfig();
    
    @Data
    public static class EmailConfig {
        /**
         * SMTP服务器地址
         */
        @NotBlank(message = "SMTP服务器地址不能为空")
        private String host = "smtp.example.com";
        
        /**
         * SMTP服务器端口
         */
        @Min(value = 1, message = "SMTP服务器端口必须大于0")
        private int port = 25;
        
        /**
         * SMTP用户名
         */
        @NotBlank(message = "SMTP用户名不能为空")
        private String username;
        
        /**
         * SMTP密码
         */
        @NotBlank(message = "SMTP密码不能为空")
        private String password;
        
        /**
         * 发件人地址
         */
        @NotBlank(message = "发件人地址不能为空")
        private String from;
        
        /**
         * 收件人地址列表
         */
        @NotNull(message = "收件人地址列表不能为空")
        private String[] to = new String[0];
        
        /**
         * 是否启用SSL
         */
        private boolean ssl = false;
    }
    
    @Data
    public static class WebhookConfig {
        /**
         * Webhook URL
         */
        @NotBlank(message = "Webhook URL不能为空")
        private String url;
        
        /**
         * 请求方法
         */
        @NotBlank(message = "请求方法不能为空")
        private String method = "POST";
        
        /**
         * 内容类型
         */
        @NotBlank(message = "内容类型不能为空")
        private String contentType = "application/json";
        
        /**
         * 超时时间（毫秒）
         */
        @Min(value = 1, message = "超时时间必须大于0")
        private int timeout = 5000;
        
        /**
         * 请求头
         */
        private String[] headers = new String[0];
    }
} 