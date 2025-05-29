package com.marsreg.search.service.impl;

import com.marsreg.document.model.Document;
import com.marsreg.document.service.DocumentService;
import com.marsreg.search.cache.SearchCacheKeyGenerator;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResult;
import com.marsreg.search.model.SearchType;
import com.marsreg.search.query.SearchQueryBuilder;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.SearchHighlightService;
import com.marsreg.search.service.SearchService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final DocumentService documentService;
    private final VectorizationService vectorizationService;
    private final VectorStorageService vectorStorageService;
    private final DocumentIndexRepository documentIndexRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchQueryBuilder searchQueryBuilder;
    private final SearchCacheKeyGenerator cacheKeyGenerator;
    private final SearchHighlightService searchHighlightService;

    @Value("${search.vector-weight:0.7}")
    private float vectorWeight;

    @Value("${search.keyword-weight:0.3}")
    private float keywordWeight;

    @Value("${search.cache.enabled}")
    private boolean cacheEnabled;

    @Override
    @Cacheable(value = "searchResults", keyGenerator = "searchCacheKeyGenerator", unless = "#result == null")
    public List<SearchResult> search(SearchRequest request) {
        switch (request.getSearchType()) {
            case VECTOR:
                return vectorSearch(request);
            case KEYWORD:
                return keywordSearch(request);
            case HYBRID:
                return hybridSearch(request);
            default:
                throw new IllegalArgumentException("Unsupported search type: " + request.getSearchType());
        }
    }

    @Override
    @Cacheable(value = "vectorResults", keyGenerator = "searchCacheKeyGenerator", unless = "#result == null")
    public List<SearchResult> vectorSearch(SearchRequest request) {
        // 将查询文本转换为向量
        float[] queryVector = vectorizationService.vectorize(request.getQuery());
        
        // 执行向量搜索
        List<Map.Entry<String, Float>> vectorResults = vectorStorageService.search(
            queryVector, request.getSize(), request.getMinSimilarity());
        
        // 获取文档详情
        List<SearchResult> results = vectorResults.stream()
            .map(entry -> {
                String documentId = entry.getKey();
                float score = entry.getValue();
                
                Optional<Document> document = documentService.getDocument(documentId);
                if (document.isPresent()) {
                    return SearchResult.builder()
                        .documentId(documentId)
                        .title(document.get().getTitle())
                        .content(document.get().getContent())
                        .score(score)
                        .build();
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
            
        // 处理高亮和摘要
        return searchHighlightService.processHighlights(results, request.getQuery());
    }

    @Override
    @Cacheable(value = "keywordResults", keyGenerator = "searchCacheKeyGenerator", unless = "#result == null")
    public List<SearchResult> keywordSearch(SearchRequest request) {
        // 构建并执行搜索查询
        SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(
            searchQueryBuilder.buildQuery(request), DocumentIndex.class);
        
        // 转换搜索结果
        List<SearchResult> results = searchHits.getSearchHits().stream()
            .map(hit -> {
                DocumentIndex index = hit.getContent();
                SearchResult result = SearchResult.builder()
                    .documentId(index.getDocumentId())
                    .title(index.getTitle())
                    .content(index.getContent())
                    .score(hit.getScore())
                    .documentType(index.getDocumentType())
                    .tags(index.getTags())
                    .createTime(index.getCreateTime().toString())
                    .updateTime(index.getUpdateTime().toString())
                    .metadata(index.getMetadata())
                    .build();
                    
                // 处理高亮字段
                Map<String, List<String>> highlightFields = hit.getHighlightFields();
                if (highlightFields != null) {
                    if (highlightFields.containsKey("title")) {
                        result.setHighlightedTitle(String.join("", highlightFields.get("title")));
                    }
                    if (highlightFields.containsKey("content")) {
                        result.setHighlightedContents(highlightFields.get("content"));
                    }
                }
                
                return result;
            })
            .collect(Collectors.toList());
            
        // 处理高亮和摘要
        return searchHighlightService.processHighlights(results, request.getQuery());
    }

    @Override
    @Cacheable(value = "hybridResults", keyGenerator = "searchCacheKeyGenerator", unless = "#result == null")
    public List<SearchResult> hybridSearch(SearchRequest request) {
        // 执行向量搜索
        List<SearchResult> vectorResults = vectorSearch(request);
        
        // 执行关键词搜索
        List<SearchResult> keywordResults = keywordSearch(request);
        
        // 合并结果
        Map<String, SearchResult> mergedResults = new HashMap<>();
        
        // 添加向量搜索结果
        vectorResults.forEach(result -> {
            result.setScore(result.getScore() * vectorWeight);
            mergedResults.put(result.getDocumentId(), result);
        });
        
        // 添加关键词搜索结果
        keywordResults.forEach(result -> {
            if (mergedResults.containsKey(result.getDocumentId())) {
                // 如果文档已存在，更新分数
                SearchResult existingResult = mergedResults.get(result.getDocumentId());
                existingResult.setScore(existingResult.getScore() + result.getScore() * keywordWeight);
            } else {
                // 如果文档不存在，添加新结果
                result.setScore(result.getScore() * keywordWeight);
                mergedResults.put(result.getDocumentId(), result);
            }
        });
        
        // 按分数排序并返回结果
        return mergedResults.values().stream()
            .sorted(Comparator.comparing(SearchResult::getScore).reversed())
            .limit(request.getSize())
            .collect(Collectors.toList());
    }

    /**
     * 清除所有缓存
     */
    @CacheEvict(value = {"searchResults", "vectorResults", "keywordResults", "hybridResults"}, allEntries = true)
    public void clearCache() {
        log.info("Cleared all search caches");
    }
} 