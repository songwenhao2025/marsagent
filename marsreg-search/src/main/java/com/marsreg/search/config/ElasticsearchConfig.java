package com.marsreg.search.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.marsreg.search.repository")
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${spring.data.elasticsearch.cluster-nodes}")
    private String clusterNodes;

    @Value("${spring.data.elasticsearch.cluster-name}")
    private String clusterName;

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(clusterNodes)
                .build();
        return RestClients.create(clientConfiguration).rest();
    }
} 