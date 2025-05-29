package com.marsreg.inference.service.impl;

import com.marsreg.inference.model.InferenceRequest;
import com.marsreg.inference.model.InferenceResponse;
import com.marsreg.inference.service.InferenceService;
import com.marsreg.inference.service.LLMService;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResult;
import com.marsreg.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InferenceServiceImpl implements InferenceService {

    private final SearchService searchService;
    private final LLMService llmService;

    @Value("${inference.context.max-documents}")
    private Integer maxDocuments;

    @Value("${inference.context.max-tokens}")
    private Integer maxTokens;

    @Override
    @Cacheable(value = "inferenceResults", key = "#request.hashCode()", unless = "#result == null")
    public InferenceResponse infer(InferenceRequest request) {
        // 1. 执行文档检索
        List<SearchResult> searchResults = searchService.search(buildSearchRequest(request));
        
        // 2. 提取上下文
        List<String> context = extractContext(searchResults);
        
        // 3. 生成回答
        String answer = llmService.generateAnswer(request.getQuestion(), context, request.getParameters());
        
        // 4. 构建响应
        return InferenceResponse.builder()
            .answer(answer)
            .references(buildReferences(searchResults))
            .metadata(buildMetadata(request, searchResults))
            .build();
    }

    @Override
    public void inferStream(InferenceRequest request, StreamCallback callback) {
        // 1. 执行文档检索
        List<SearchResult> searchResults = searchService.search(buildSearchRequest(request));
        
        // 2. 提取上下文
        List<String> context = extractContext(searchResults);
        
        // 3. 流式生成回答
        llmService.generateAnswerStream(request.getQuestion(), context, request.getParameters(),
            new LLMService.StreamCallback() {
                @Override
                public void onToken(String token) {
                    callback.onToken(token);
                }
                
                @Override
                public void onComplete() {
                    callback.onComplete();
                }
                
                @Override
                public void onError(Throwable error) {
                    callback.onError(error);
                }
            });
    }

    private SearchRequest buildSearchRequest(InferenceRequest request) {
        return SearchRequest.builder()
            .query(request.getQuestion())
            .searchType(request.getSearchType())
            .size(request.getMaxDocuments() != null ? request.getMaxDocuments() : maxDocuments)
            .minSimilarity(request.getMinSimilarity())
            .documentTypes(request.getDocumentTypes())
            .build();
    }

    private List<String> extractContext(List<SearchResult> searchResults) {
        return searchResults.stream()
            .map(result -> String.format("标题：%s\n内容：%s", 
                result.getTitle(), result.getContent()))
            .collect(Collectors.toList());
    }

    private List<InferenceResponse.DocumentReference> buildReferences(List<SearchResult> searchResults) {
        return searchResults.stream()
            .map(result -> InferenceResponse.DocumentReference.builder()
                .documentId(result.getDocumentId())
                .title(result.getTitle())
                .content(result.getContent())
                .relevance(result.getScore())
                .metadata(result.getMetadata())
                .build())
            .collect(Collectors.toList());
    }

    private Map<String, Object> buildMetadata(InferenceRequest request, List<SearchResult> searchResults) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("searchType", request.getSearchType());
        metadata.put("documentCount", searchResults.size());
        metadata.put("averageRelevance", searchResults.stream()
            .mapToDouble(SearchResult::getScore)
            .average()
            .orElse(0.0));
        return metadata;
    }
} 