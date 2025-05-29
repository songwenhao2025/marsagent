package com.marsreg.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "marsreg.vector")
public class VectorizationConfig {
    
    private Model model = new Model();
    private Integer threadPoolSize = 4;
    private Cache cache = new Cache();
    private Milvus milvus = new Milvus();
    
    @Data
    public static class Model {
        private String name = "sentence-transformers/all-MiniLM-L6-v2";
        private String version = "1.0.0";
        private Integer dimension = 384;
        private String path;
        private String device = "cpu";
        private Integer batchSize = 32;
    }
    
    @Data
    public static class Cache {
        private Boolean enabled = true;
        private Integer expire = 3600;
    }
    
    @Data
    public static class Milvus {
        private String host = "localhost";
        private Integer port = 19530;
        private String collection = "marsreg_vectors";
        private Integer dimension = 384;
        private Auth auth = new Auth();
        private Index index = new Index();
        
        @Data
        public static class Auth {
            private Boolean enabled = true;
            private String token;
        }
        
        @Data
        public static class Index {
            private String type = "IVF_SQ8";
            private Integer nlist = 1024;
            private String metricType = "COSINE";
        }
    }
} 