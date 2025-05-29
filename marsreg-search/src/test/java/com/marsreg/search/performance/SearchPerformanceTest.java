package com.marsreg.search.performance;

import com.marsreg.document.model.Document;
import com.marsreg.document.service.DocumentService;
import com.marsreg.search.config.IntegrationTestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResult;
import com.marsreg.search.model.SearchType;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(IntegrationTestConfig.class)
@ActiveProfiles("test")
public class SearchPerformanceTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

    private static final int DOCUMENT_COUNT = 1000;
    private static final int CONCURRENT_USERS = 10;
    private static final int REQUESTS_PER_USER = 100;
    private static final float[] TEST_VECTOR = new float[384];

    @BeforeEach
    void setUp() {
        // 初始化测试向量
        Arrays.fill(TEST_VECTOR, 0.1f);
        
        // 清空文档索引
        documentIndexRepository.deleteAll();
        
        // 准备测试数据
        List<Document> documents = IntStream.range(0, DOCUMENT_COUNT)
            .mapToObj(i -> Document.builder()
                .id("test-doc-" + i)
                .title("测试文档" + i)
                .content("这是测试文档" + i + "的内容")
                .build())
            .collect(Collectors.toList());
        
        // 设置文档服务模拟行为
        documents.forEach(doc -> 
            when(documentService.getDocument(doc.getId())).thenReturn(Optional.of(doc)));
        
        // 设置向量化服务模拟行为
        when(vectorizationService.vectorize(anyString())).thenReturn(TEST_VECTOR);
        
        // 设置向量存储服务模拟行为
        when(vectorStorageService.search(any(), anyInt(), anyFloat()))
            .thenReturn(documents.stream()
                .map(doc -> Map.entry(doc.getId(), 0.8f))
                .collect(Collectors.toList()));
        
        // 创建测试文档索引
        List<DocumentIndex> indices = documents.stream()
            .map(doc -> {
                DocumentIndex index = new DocumentIndex();
                index.setId(doc.getId());
                index.setDocumentId(doc.getId());
                index.setTitle(doc.getTitle());
                index.setContent(doc.getContent());
                return index;
            })
            .collect(Collectors.toList());
        
        documentIndexRepository.saveAll(indices);
    }

    @Test
    void testConcurrentSearch() throws InterruptedException {
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        List<Future<List<Long>>> futures = new ArrayList<>();
        
        // 为每个用户创建搜索任务
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            futures.add(executorService.submit(() -> {
                List<Long> responseTimes = new ArrayList<>();
                try {
                    for (int j = 0; j < REQUESTS_PER_USER; j++) {
                        // 创建随机查询
                        String query = "测试查询" + new Random().nextInt(DOCUMENT_COUNT);
                        
                        // 创建检索请求
                        SearchRequest request = SearchRequest.builder()
                            .query(query)
                            .searchType(SearchType.HYBRID)
                            .size(10)
                            .minSimilarity(0.5f)
                            .build();
                        
                        // 执行检索并记录响应时间
                        long startTime = System.currentTimeMillis();
                        List<SearchResult> results = searchService.search(request);
                        long endTime = System.currentTimeMillis();
                        
                        responseTimes.add(endTime - startTime);
                        
                        // 验证结果
                        assertNotNull(results);
                        assertFalse(results.isEmpty());
                    }
                } finally {
                    latch.countDown();
                }
                return responseTimes;
            }));
        }
        
        // 等待所有任务完成
        latch.await();
        
        // 收集所有响应时间
        List<Long> allResponseTimes = futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return Collections.<Long>emptyList();
                }
            })
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        // 计算性能指标
        double avgResponseTime = allResponseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        long maxResponseTime = allResponseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        long minResponseTime = allResponseTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0);
        
        // 输出性能指标
        System.out.println("性能测试结果：");
        System.out.println("总请求数：" + (CONCURRENT_USERS * REQUESTS_PER_USER));
        System.out.println("平均响应时间：" + avgResponseTime + "ms");
        System.out.println("最大响应时间：" + maxResponseTime + "ms");
        System.out.println("最小响应时间：" + minResponseTime + "ms");
        
        // 验证性能要求
        assertTrue(avgResponseTime < 1000, "平均响应时间应小于1秒");
        assertTrue(maxResponseTime < 3000, "最大响应时间应小于3秒");
        
        // 关闭线程池
        executorService.shutdown();
    }

    @Test
    void testCachePerformance() {
        // 创建检索请求
        SearchRequest request = SearchRequest.builder()
            .query("测试查询")
            .searchType(SearchType.VECTOR)
            .size(10)
            .minSimilarity(0.5f)
            .build();
        
        // 第一次检索（无缓存）
        long startTime1 = System.currentTimeMillis();
        List<SearchResult> firstResults = searchService.search(request);
        long endTime1 = System.currentTimeMillis();
        long firstResponseTime = endTime1 - startTime1;
        
        // 第二次检索（有缓存）
        long startTime2 = System.currentTimeMillis();
        List<SearchResult> secondResults = searchService.search(request);
        long endTime2 = System.currentTimeMillis();
        long secondResponseTime = endTime2 - startTime2;
        
        // 验证结果
        assertEquals(firstResults, secondResults);
        
        // 验证缓存性能
        assertTrue(secondResponseTime < firstResponseTime, 
            "缓存检索的响应时间应小于首次检索");
        
        // 输出性能指标
        System.out.println("缓存性能测试结果：");
        System.out.println("首次检索响应时间：" + firstResponseTime + "ms");
        System.out.println("缓存检索响应时间：" + secondResponseTime + "ms");
        System.out.println("性能提升：" + 
            ((firstResponseTime - secondResponseTime) * 100.0 / firstResponseTime) + "%");
    }
} 