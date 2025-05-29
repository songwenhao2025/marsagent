package com.marsreg.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache.notify")
public class NotifyConfig {
    
    private boolean enabled = true;
    private String type = "log"; // log, email, webhook
    private EmailConfig email = new EmailConfig();
    private WebhookConfig webhook = new WebhookConfig();
    
    @Data
    public static class EmailConfig {
        private String host;
        private int port = 25;
        private String username;
        private String password;
        private String from;
        private String[] to;
        private boolean ssl = false;
    }
    
    @Data
    public static class WebhookConfig {
        private String url;
        private String method = "POST";
        private String contentType = "application/json";
        private int timeout = 5000;
        private String[] headers;
    }
} 