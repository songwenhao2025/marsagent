package com.marsreg.search.integration;

import com.marsreg.common.model.Document;
import com.marsreg.search.config.IntegrationTestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResponse;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.SearchService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(IntegrationTestConfig.class)
@ActiveProfiles("test")
public class SearchServiceIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @MockBean
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

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
        when(vectorStorageService.searchSimilar(any(), anyInt(), anyFloat()))
            .thenReturn(Map.of(TEST_DOC_ID, 0.8f));
        
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
    void testEndToEndSearch() {
        // 创建检索请求
        SearchRequest request = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.HYBRID)
            .size(10)
            .minScore(0.5f)
            .build();
        
        // 执行检索
        SearchResponse results = searchService.search(request);
        
        // 验证结果
        assertNotNull(results);
        assertFalse(results.getResults().isEmpty());
        assertEquals(TEST_DOC_ID, results.getResults().get(0).getId());
        
        // 验证服务调用
        verify(vectorizationService).vectorize(TEST_QUERY);
        verify(vectorStorageService).searchSimilar(TEST_VECTOR, 10, 0.5f);
    }

    @Test
    void testCacheIntegration() {
        // 第一次检索
        SearchRequest request = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.VECTOR)
            .size(10)
            .minScore(0.5f)
            .build();
        
        SearchResponse firstResults = searchService.search(request);
        
        // 第二次检索（应该使用缓存）
        SearchResponse secondResults = searchService.search(request);
        
        // 验证结果
        assertEquals(firstResults, secondResults);
        
        // 验证服务调用次数（应该只调用一次）
        verify(vectorizationService, times(1)).vectorize(TEST_QUERY);
        verify(vectorStorageService, times(1)).searchSimilar(TEST_VECTOR, 10, 0.5f);
    }

    @Test
    void testMultipleSearchTypes() {
        // 测试向量检索
        SearchRequest vectorRequest = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.VECTOR)
            .size(10)
            .minScore(0.5f)
            .build();
        SearchResponse vectorResults = searchService.search(vectorRequest);
        assertNotNull(vectorResults);
        assertFalse(vectorResults.getResults().isEmpty());
        
        // 测试关键词检索
        SearchRequest keywordRequest = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.KEYWORD)
            .size(10)
            .build();
        SearchResponse keywordResults = searchService.search(keywordRequest);
        assertNotNull(keywordResults);
        assertFalse(keywordResults.getResults().isEmpty());
        
        // 测试混合检索
        SearchRequest hybridRequest = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchRequest.SearchType.HYBRID)
            .size(10)
            .minScore(0.5f)
            .build();
        SearchResponse hybridResults = searchService.search(hybridRequest);
        assertNotNull(hybridResults);
        assertFalse(hybridResults.getResults().isEmpty());
        
        // 验证所有检索类型都返回了结果
        assertTrue(vectorResults.getResults().size() > 0);
        assertTrue(keywordResults.getResults().size() > 0);
        assertTrue(hybridResults.getResults().size() > 0);
    }
} 