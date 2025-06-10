package com.marsreg.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.json.JsonData;
import com.marsreg.search.exception.SearchException;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResponse;
import com.marsreg.search.service.SearchService;
import com.marsreg.vector.service.VectorizationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private static final float KEYWORD_WEIGHT = 0.6f;
    private static final float VECTOR_WEIGHT = 0.4f;
    private static final int MAX_RESULTS = 1000;
    private static final int AGGREGATION_SIZE = 20;

    private final ElasticsearchClient elasticsearchClient;
    private final MeterRegistry meterRegistry;
    private final VectorizationService vectorizationService;
    private final Map<String, Long> searchStats = new ConcurrentHashMap<>();

    public SearchServiceImpl(
            ElasticsearchClient elasticsearchClient,
            MeterRegistry meterRegistry,
            VectorizationService vectorizationService) {
        this.elasticsearchClient = elasticsearchClient;
        this.meterRegistry = meterRegistry;
        this.vectorizationService = vectorizationService;
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        return switch (request.getSearchType()) {
            case KEYWORD -> keywordSearch(request);
            case VECTOR -> vectorSearch(request);
            case HYBRID -> hybridSearch(request);
        };
    }

    @Override
    @Cacheable(value = "search", key = "#request.toString()", unless = "#result.total == 0")
    public SearchResponse keywordSearch(SearchRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Builder searchRequestBuilder = new Builder()
                .index(Arrays.asList(request.getDocumentTypes().toArray(new String[0])))
                .query(q -> q
                    .multiMatch(m -> m
                        .query(request.getQuery())
                        .fields(Arrays.asList(request.getFields().toArray(new String[0])))
                        .tieBreaker(0.3)
                        .operator(Operator.And)
                        .minimumShouldMatch("75%")
                        .fuzziness("AUTO")
                        .prefixLength(2)
                        .maxExpansions(50)
                        .lenient(true)
                    )
                )
                .from((request.getPage() - 1) * request.getSize())
                .size(request.getSize());

            if (request.getMinScore() != null) {
                searchRequestBuilder.minScore(request.getMinScore().doubleValue());
            }

            // 添加排序
            if (request.getSortField() != null) {
                searchRequestBuilder.sort(s -> s
                    .field(f -> f
                        .field(request.getSortField())
                        .order(request.getSortOrder() != null ? 
                            SortOrder.valueOf(request.getSortOrder().toUpperCase()) : 
                            SortOrder.Desc)
                    )
                );
            }

            // 添加聚合
            if (request.getAggregations() != null && !request.getAggregations().isEmpty()) {
                for (String field : request.getAggregations()) {
                    // 添加词条聚合
                    searchRequestBuilder.aggregations(field + "_terms", a -> a
                        .terms(t -> t
                            .field(field)
                            .size(AGGREGATION_SIZE)
                        )
                    );

                    // 添加数值统计聚合（如果字段是数值类型）
                    if (field.endsWith("Count") || field.endsWith("Amount") || field.endsWith("Price")) {
                        searchRequestBuilder.aggregations(field + "_stats", a -> a
                            .stats(s -> s
                                .field(field)
                            )
                        );
                    }
                }
            }

            co.elastic.clients.elasticsearch.core.SearchResponse<Map> response = elasticsearchClient.search(
                searchRequestBuilder.build(),
                Map.class
            );

            // 更新搜索统计
            updateSearchStats(request.getQuery());

            meterRegistry.counter("search.total", "type", "keyword").increment();
            meterRegistry.counter("search.results", "type", "keyword")
                .increment(response.hits().total().value());

            return convertToSearchResponse(response);
        } catch (Exception e) {
            meterRegistry.counter("search.errors", "type", "keyword").increment();
            log.error("Keyword search failed", e);
            throw new SearchException("SEARCH_FAILED", "Keyword search failed", e);
        } finally {
            sample.stop(meterRegistry.timer("search.duration", "type", "keyword"));
        }
    }

    @Override
    @Cacheable(value = "search", key = "'vector-' + #request.toString()", unless = "#result.total == 0")
    public SearchResponse vectorSearch(SearchRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            float[] queryVector = vectorizationService.vectorizeWithCache(request.getQuery());
            
            Builder searchRequestBuilder = new Builder()
                .index(Arrays.asList(request.getDocumentTypes().toArray(new String[0])))
                .query(q -> q
                    .scriptScore(s -> s
                        .query(q2 -> q2.matchAll(m -> m))
                        .script(sc -> sc
                            .inline(i -> i
                                .source("cosineSimilarity(params.query_vector, 'vector') + 1.0")
                                .params("query_vector", JsonData.of(queryVector))
                            )
                        )
                    )
                )
                .from((request.getPage() - 1) * request.getSize())
                .size(request.getSize());

            if (request.getMinScore() != null) {
                searchRequestBuilder.minScore(request.getMinScore().doubleValue());
            }

            co.elastic.clients.elasticsearch.core.SearchResponse<Map> response = elasticsearchClient.search(
                searchRequestBuilder.build(),
                Map.class
            );

            // 更新搜索统计
            updateSearchStats(request.getQuery());

            meterRegistry.counter("search.total", "type", "vector").increment();
            meterRegistry.counter("search.results", "type", "vector")
                .increment(response.hits().total().value());

            return convertToSearchResponse(response);
        } catch (Exception e) {
            meterRegistry.counter("search.errors", "type", "vector").increment();
            log.error("Vector search failed", e);
            throw new SearchException("SEARCH_FAILED", "Vector search failed", e);
        } finally {
            sample.stop(meterRegistry.timer("search.duration", "type", "vector"));
        }
    }

    @Override
    @Cacheable(value = "search", key = "'hybrid-' + #request.toString()", unless = "#result.total == 0")
    public SearchResponse hybridSearch(SearchRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SearchResponse keywordResults = keywordSearch(request);
            SearchResponse vectorResults = vectorSearch(request);
            
            Map<String, SearchResponse.SearchResult> resultMap = new HashMap<>();
            
            for (SearchResponse.SearchResult result : keywordResults.getResults()) {
                result.setScore(result.getScore() * KEYWORD_WEIGHT);
                resultMap.put(result.getId(), result);
            }
            
            for (SearchResponse.SearchResult result : vectorResults.getResults()) {
                SearchResponse.SearchResult existingResult = resultMap.get(result.getId());
                if (existingResult != null) {
                    existingResult.setScore(existingResult.getScore() + result.getScore() * VECTOR_WEIGHT);
                } else {
                    result.setScore(result.getScore() * VECTOR_WEIGHT);
                    resultMap.put(result.getId(), result);
                }
            }
            
            List<SearchResponse.SearchResult> mergedResults = new ArrayList<>(resultMap.values());
            mergedResults.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
            
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                mergedResults = applyFilters(mergedResults, request.getFilters());
            }
            
            if (mergedResults.size() > MAX_RESULTS) {
                mergedResults = mergedResults.subList(0, MAX_RESULTS);
            }
            
            // 更新搜索统计
            updateSearchStats(request.getQuery());
            
            meterRegistry.counter("search.total", "type", "hybrid").increment();
            meterRegistry.counter("search.results", "type", "hybrid")
                .increment(mergedResults.size());
            
            return SearchResponse.builder()
                .results(mergedResults)
                .total(mergedResults.size())
                .page(request.getPage())
                .size(request.getSize())
                .build();
        } catch (Exception e) {
            meterRegistry.counter("search.errors", "type", "hybrid").increment();
            log.error("Hybrid search failed", e);
            throw new SearchException("SEARCH_FAILED", "Hybrid search failed", e);
        } finally {
            sample.stop(meterRegistry.timer("search.duration", "type", "hybrid"));
        }
    }

    @Override
    public Map<String, Object> getSearchStats() {
        return new HashMap<>(searchStats);
    }

    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @CacheEvict(value = {"search", "suggestions"}, allEntries = true)
    public void clearCache() {
        log.info("Clearing search cache");
    }

    private void updateSearchStats(String query) {
        searchStats.merge(query, 1L, Long::sum);
    }

    private List<SearchResponse.SearchResult> applyFilters(
            List<SearchResponse.SearchResult> results,
            Map<String, Object> filters) {
        return results.stream()
            .filter(result -> {
                for (Map.Entry<String, Object> filter : filters.entrySet()) {
                    String field = filter.getKey();
                    Object value = filter.getValue();
                    Object resultValue = result.getMetadata().get(field);
                    
                    if (resultValue == null || !resultValue.toString().equals(value.toString())) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private SearchResponse convertToSearchResponse(co.elastic.clients.elasticsearch.core.SearchResponse<Map> response) {
        TotalHits totalHits = response.hits().total();
        List<SearchResponse.SearchResult> results = new ArrayList<>();

        for (Hit<Map> hit : response.hits().hits()) {
            if (hit.source() == null) {
                continue;
            }

            Map<String, Object> source = hit.source();
            Map<String, List<String>> highlights = hit.highlight();
            List<String> contentHighlights = highlights != null ? 
                highlights.getOrDefault("content", List.of()) : List.of();

            SearchResponse.SearchResult result = SearchResponse.SearchResult.builder()
                .id(hit.id())
                .title(getStringValue(source, "title"))
                .content(getStringValue(source, "content"))
                .type(getStringValue(source, "type"))
                .score(hit.score() != null ? hit.score().floatValue() : 0.0f)
                .metadata(source)
                .highlights(contentHighlights)
                .build();
            results.add(result);
        }

        return SearchResponse.builder()
            .results(results)
            .total(totalHits != null ? totalHits.value() : 0)
            .page(response.hits().hits().size() > 0 ? 
                (int) (response.hits().hits().get(0).index().hashCode() % 100) : 1)
            .size(response.hits().hits().size())
            .build();
    }

    private String getStringValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value != null ? value.toString() : "";
    }
} 