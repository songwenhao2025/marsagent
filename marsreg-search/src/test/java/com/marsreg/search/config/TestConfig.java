package com.marsreg.search.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

@TestConfiguration
@EnableElasticsearchRepositories(basePackages = "com.marsreg.search.repository")
public class TestConfig extends ElasticsearchConfiguration {

    private static final ElasticsearchContainer elasticsearchContainer;

    static {
        elasticsearchContainer = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.1")
                .asCompatibleSubstituteFor("elasticsearch")
        );
        elasticsearchContainer.withExposedPorts(9200);
        elasticsearchContainer.withEnv("discovery.type", "single-node");
        elasticsearchContainer.withEnv("xpack.security.enabled", "false");
        elasticsearchContainer.start();
    }

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo(elasticsearchContainer.getHost() + ":" + elasticsearchContainer.getMappedPort(9200))
            .build();
    }

    @Bean
    @Primary
    public ElasticsearchOperations elasticsearchOperations() {
        RestClient restClient = RestClient.builder(
            new HttpHost(elasticsearchContainer.getHost(), elasticsearchContainer.getMappedPort(9200), "http")
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());

        ElasticsearchClient client = new ElasticsearchClient(transport);
        return new ElasticsearchTemplate(client);
    }
} 