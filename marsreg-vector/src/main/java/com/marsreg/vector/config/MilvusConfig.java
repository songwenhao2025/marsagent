package com.marsreg.vector.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.client.MilvusClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${marsreg.vector.milvus.host:localhost}")
    private String host;

    @Value("${marsreg.vector.milvus.port:19530}")
    private int port;

    @Value("${marsreg.vector.milvus.auth.enabled:false}")
    private boolean authEnabled;

    @Value("${marsreg.vector.milvus.auth.username:root}")
    private String username;

    @Value("${marsreg.vector.milvus.auth.password:Milvus}")
    private String password;

    @Value("${marsreg.vector.milvus.auth.token:}")
    private String token;

    @Bean
    public MilvusClient milvusClient() {
        try {
            ConnectParam.Builder builder = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port);

            // 如果启用了认证
            if (authEnabled) {
                if (token != null && !token.isEmpty()) {
                    // 使用token认证
                    builder.withToken(token);
                } else {
                    // 使用用户名密码认证
                    builder.withAuthorization(username, password);
                }
            }

            MilvusClient client = new MilvusServiceClient(builder.build());
            log.info("Milvus客户端初始化成功: {}:{}", host, port);
            return client;
        } catch (Exception e) {
            log.error("Milvus客户端初始化失败", e);
            throw new RuntimeException("Milvus客户端初始化失败", e);
        }
    }
} 