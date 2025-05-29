package com.marsreg.search.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@EnableElasticsearchRepositories(basePackages = "com.marsreg.search.repository")
public class IntegrationTestConfig {

    private static final ElasticsearchContainer elasticsearchContainer;

    static {
        elasticsearchContainer = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
        );
        elasticsearchContainer.start();
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.rest.uris", elasticsearchContainer::getHttpHostAddress);
    }

    @Bean
    @Primary
    public ElasticsearchOperations elasticsearchOperations() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
            .connectedTo(elasticsearchContainer.getHttpHostAddress())
            .build();
        return new ElasticsearchRestTemplate(RestClients.create(clientConfiguration).rest());
    }
} 