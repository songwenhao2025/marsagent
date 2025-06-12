package com.marsreg.search.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@EnableElasticsearchRepositories(basePackages = "com.marsreg.search.repository")
public class IntegrationTestConfig extends ElasticsearchConfiguration {

    private static final ElasticsearchContainer elasticsearchContainer;

    static {
        elasticsearchContainer = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.1")
                .asCompatibleSubstituteFor("elasticsearch")
        );
        elasticsearchContainer.withExposedPorts(9200);
        elasticsearchContainer.withEnv("discovery.type", "single-node");
        elasticsearchContainer.withEnv("xpack.security.enabled", "false");
        elasticsearchContainer.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
        elasticsearchContainer.start();
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> 
            "http://" + elasticsearchContainer.getHost() + ":" + elasticsearchContainer.getMappedPort(9200));
    }

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo(elasticsearchContainer.getHost() + ":" + elasticsearchContainer.getMappedPort(9200))
            .withSocketTimeout(30000)
            .withConnectTimeout(1000)
            .build();
    }
} 