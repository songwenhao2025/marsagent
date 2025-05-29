package com.marsreg.search.service;

import com.marsreg.document.model.Document;
import com.marsreg.document.service.DocumentService;
import com.marsreg.search.cache.SearchCacheKeyGenerator;
import com.marsreg.search.config.TestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResult;
import com.marsreg.search.model.SearchType;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private DocumentService documentService;

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
        
        // 设置文档服务模拟行为
        Document testDocument = Document.builder()
            .id(TEST_DOC_ID)
            .title("测试文档")
            .content("这是一个测试文档的内容")
            .build();
        when(documentService.getDocument(TEST_DOC_ID)).thenReturn(Optional.of(testDocument));
        
        // 设置向量化服务模拟行为
        when(vectorizationService.vectorize(anyString())).thenReturn(TEST_VECTOR);
        
        // 设置向量存储服务模拟行为
        when(vectorStorageService.search(any(), anyInt(), anyFloat()))
            .thenReturn(List.of(Map.entry(TEST_DOC_ID, 0.8f)));
        
        // 创建测试文档索引
        DocumentIndex index = new DocumentIndex();
        index.setId(TEST_DOC_ID);
        index.setDocumentId(TEST_DOC_ID);
        index.setTitle("测试文档");
        index.setContent("这是一个测试文档的内容");
        documentIndexRepository.save(index);
    }

    @Test
    void testVectorSearch() {
        // 执行向量检索
        List<SearchResult> results = searchService.vectorSearch(TEST_QUERY, 10, 0.5f);
        
        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(TEST_DOC_ID, results.get(0).getDocumentId());
        assertEquals(0.8f, results.get(0).getScore());
        
        // 验证服务调用
        verify(vectorizationService).vectorize(TEST_QUERY);
        verify(vectorStorageService).search(TEST_VECTOR, 10, 0.5f);
        verify(documentService).getDocument(TEST_DOC_ID);
    }

    @Test
    void testKeywordSearch() {
        // 执行关键词检索
        List<SearchResult> results = searchService.keywordSearch(TEST_QUERY, 10);
        
        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(TEST_DOC_ID, results.get(0).getDocumentId());
    }

    @Test
    void testHybridSearch() {
        // 执行混合检索
        List<SearchResult> results = searchService.hybridSearch(TEST_QUERY, 10, 0.5f);
        
        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(TEST_DOC_ID, results.get(0).getDocumentId());
        
        // 验证服务调用
        verify(vectorizationService).vectorize(TEST_QUERY);
        verify(vectorStorageService).search(TEST_VECTOR, 10, 0.5f);
        verify(documentService).getDocument(TEST_DOC_ID);
    }

    @Test
    void testSearchWithRequest() {
        // 创建检索请求
        SearchRequest request = SearchRequest.builder()
            .query(TEST_QUERY)
            .searchType(SearchType.VECTOR)
            .size(10)
            .minSimilarity(0.5f)
            .build();
        
        // 执行检索
        List<SearchResult> results = searchService.search(request);
        
        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(TEST_DOC_ID, results.get(0).getDocumentId());
    }
} 