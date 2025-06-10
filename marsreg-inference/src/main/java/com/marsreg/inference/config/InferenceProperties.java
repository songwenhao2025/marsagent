package com.marsreg.inference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Validated
@ConfigurationProperties(prefix = "inference")
public class InferenceProperties {
    
    @NotNull(message = "上下文配置不能为空")
    private Context context = new Context();
    
    @NotNull(message = "模型配置不能为空")
    private Model model = new Model();
    
    @NotNull(message = "缓存配置不能为空")
    private Cache cache = new Cache();
    
    @NotNull(message = "重试配置不能为空")
    private Retry retry = new Retry();
    
    @Data
    public static class Context {
        @Min(value = 1, message = "最大文档数必须大于0")
        private Integer maxDocuments = 10;
        
        @Min(value = 1, message = "最大token数必须大于0")
        private Integer maxTokens = 2000;
        
        @Min(value = 1, message = "最小相似度必须大于0")
        private Float minSimilarity = 0.7f;
    }
    
    @Data
    public static class Model {
        @NotBlank(message = "模型名称不能为空")
        private String name = "gpt-3.5-turbo";
        
        @NotBlank(message = "模型版本不能为空")
        private String version = "1.0.0";
        
        @Min(value = 0, message = "温度必须大于等于0")
        private Double temperature = 0.7;
        
        @Min(value = 1, message = "最大token数必须大于0")
        private Integer maxTokens = 2000;
        
        @Min(value = 1, message = "超时时间必须大于0")
        private Integer timeout = 30;
        
        private Map<String, Object> parameters;
    }
    
    @Data
    public static class Cache {
        @Min(value = 1, message = "缓存过期时间必须大于0")
        private Integer expireSeconds = 3600;
        
        @Min(value = 1, message = "最大缓存大小必须大于0")
        private Integer maxSize = 1000;
        
        private Boolean enabled = true;
    }
    
    @Data
    public static class Retry {
        @Min(value = 1, message = "最大重试次数必须大于0")
        private Integer maxAttempts = 3;
        
        @Min(value = 1, message = "初始延迟时间必须大于0")
        private Integer initialDelay = 1000;
        
        @Min(value = 1, message = "最大延迟时间必须大于0")
        private Integer maxDelay = 10000;
        
        @Min(value = 1, message = "延迟倍数必须大于0")
        private Integer multiplier = 2;
    }
} 