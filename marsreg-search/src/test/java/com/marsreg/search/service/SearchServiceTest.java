package com.marsreg.search.service;

import com.marsreg.search.cache.SearchCacheKeyGenerator;
import com.marsreg.search.config.TestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResponse;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.impl.SearchServiceImpl;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public class SearchServiceTest {

    @Autowired
    private SearchServiceImpl searchService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @MockBean
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

    @MockBean
    private SearchCacheKeyGenerator cacheKeyGenerator;

    private static final String TEST_QUERY = "测试查询";
    private static final float[] TEST_VECTOR = new float[384];
    private static final String TEST_DOC_ID = "test-doc-1";

    @BeforeEach
    void setUp() {
        // 初始化测试向量
        Arrays.fill(TEST_VECTOR, 0.1f);
        
        // 清空文档索引
        documentIndexRepository.deleteAll();
        
        // 设置向量化服务模拟行为
        when(vectorizationService.vectorize(anyString())).thenReturn(TEST_VECTOR);
        
        // 设置向量存储服务模拟行为
        Map<String, Float> searchResults = new HashMap<>();
        searchResults.put(TEST_DOC_ID, 0.8f);
        when(vectorStorageService.searchSimilar(any(), anyInt(), anyFloat()))
            .thenReturn(searchResults);
        
        // 创建测试文档索引
        DocumentIndex index = DocumentIndex.builder()
            .id(TEST_DOC_ID)
            .documentId(TEST_DOC_ID)
            .title("测试文档")
            .content("这是一个测试文档的内容")
            .build();
        documentIndexRepository.save(index);
    }

    @Test
    void testVectorSearch() {
        // 创建检索请求
        SearchRequest request = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.VECTOR)
            .size(10)
            .minScore(0.5f)
            .build();
            
        // 执行向量检索
        SearchResponse response = searchService.search(request);
        
        // 验证结果
        assertNotNull(response);
        assertFalse(response.getResults().isEmpty());
        assertEquals(TEST_DOC_ID, response.getResults().get(0).getId());
        assertEquals(0.8f, response.getResults().get(0).getScore());
        
        // 验证服务调用
        verify(vectorizationService).vectorize(TEST_QUERY);
        verify(vectorStorageService).searchSimilar(TEST_VECTOR, 10, 0.5f);
    }

    @Test
    void testKeywordSearch() {
        // 创建检索请求
        SearchRequest request = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.KEYWORD)
            .size(10)
            .build();
            
        // 执行关键词检索
        SearchResponse response = searchService.search(request);
        
        // 验证结果
        assertNotNull(response);
        assertFalse(response.getResults().isEmpty());
        assertEquals(TEST_DOC_ID, response.getResults().get(0).getId());
    }

    @Test
    void testHybridSearch() {
        // 创建检索请求
        SearchRequest request = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.HYBRID)
            .size(10)
            .minScore(0.5f)
            .build();
            
        // 执行混合检索
        SearchResponse response = searchService.search(request);
        
        // 验证结果
        assertNotNull(response);
        assertFalse(response.getResults().isEmpty());
        assertEquals(TEST_DOC_ID, response.getResults().get(0).getId());
        
        // 验证服务调用
        verify(vectorizationService).vectorize(TEST_QUERY);
        verify(vectorStorageService).searchSimilar(TEST_VECTOR, 10, 0.5f);
    }

    @Test
    void testSearchWithRequest() {
        // 创建检索请求
        SearchRequest request = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.VECTOR)
            .size(10)
            .minScore(0.5f)
            .build();
        
        // 执行检索
        SearchResponse response = searchService.search(request);
        
        // 验证结果
        assertNotNull(response);
        assertFalse(response.getResults().isEmpty());
        assertEquals(TEST_DOC_ID, response.getResults().get(0).getId());
    }
} 