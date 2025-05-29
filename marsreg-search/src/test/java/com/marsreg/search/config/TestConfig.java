package com.marsreg.search.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.elasticsearch.client.RestHighLevelClient;

@TestConfiguration
public class TestConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .build();
        return RestClients.create(clientConfiguration).rest();
    }

    @Bean
    public ElasticsearchOperations elasticsearchOperations(RestHighLevelClient restHighLevelClient) {
        return new ElasticsearchRestTemplate(restHighLevelClient);
    }
} 