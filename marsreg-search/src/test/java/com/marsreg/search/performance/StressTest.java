package com.marsreg.search.performance;

import com.marsreg.common.model.Document;
import com.marsreg.search.config.IntegrationTestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResponse;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.DocumentIndexSyncService;
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
public class StressTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DocumentIndexSyncService syncService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @MockBean
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

    private static final int DOCUMENT_COUNT = 5000;
    private static final int CONCURRENT_USERS = 50;
    private static final int REQUESTS_PER_USER = 200;
    private static final int TEST_DURATION_SECONDS = 300;
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
                .id((long) i)
                .name("测试文档" + i)
                .content("这是测试文档" + i + "的内容")
                .build())
            .collect(Collectors.toList());
        
        // 设置向量化服务模拟行为
        when(vectorizationService.vectorize(anyString())).thenReturn(TEST_VECTOR);
        
        // 设置向量存储服务模拟行为
        when(vectorStorageService.searchSimilar(any(), anyInt(), anyFloat()))
            .thenReturn(documents.stream()
                .collect(Collectors.toMap(
                    doc -> doc.getId().toString(),
                    doc -> 0.8f
                )));
        
        // 创建测试文档索引
        List<DocumentIndex> indices = documents.stream()
            .map(doc -> DocumentIndex.builder()
                .id(doc.getId().toString())
                .documentId(doc.getId().toString())
                .title(doc.getName())
                .content(doc.getContent())
                .build())
            .collect(Collectors.toList());
        
        documentIndexRepository.saveAll(indices);
    }

    @Test
    void testSystemUnderLoad() throws InterruptedException {
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        List<Future<TestResult>> futures = new ArrayList<>();
        
        // 创建测试结果收集器
        TestResultCollector resultCollector = new TestResultCollector();
        
        // 为每个用户创建测试任务
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            futures.add(executorService.submit(() -> {
                TestResult result = new TestResult();
                try {
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + (TEST_DURATION_SECONDS * 1000);
                    
                    while (System.currentTimeMillis() < endTime) {
                        // 随机选择测试类型
                        TestType testType = TestType.random();
                        long requestStartTime = System.currentTimeMillis();
                        
                        try {
                            switch (testType) {
                                case SEARCH:
                                    performSearch(result);
                                    break;
                                case SYNC:
                                    performSync(result);
                                    break;
                                case REBUILD:
                                    performRebuild(result);
                                    break;
                            }
                            
                            result.recordSuccess(System.currentTimeMillis() - requestStartTime);
                        } catch (Exception e) {
                            result.recordFailure(System.currentTimeMillis() - requestStartTime);
                        }
                        
                        // 随机等待一段时间，模拟真实用户行为
                        Thread.sleep(new Random().nextInt(100));
                    }
                } finally {
                    latch.countDown();
                }
                return result;
            }));
        }
        
        // 等待所有任务完成
        latch.await();
        
        // 收集所有测试结果
        futures.forEach(future -> {
            try {
                resultCollector.addResult(future.get());
            } catch (Exception e) {
                // 忽略异常
            }
        });
        
        // 输出测试结果
        System.out.println("压力测试结果：");
        System.out.println("测试持续时间：" + TEST_DURATION_SECONDS + "秒");
        System.out.println("并发用户数：" + CONCURRENT_USERS);
        System.out.println("总请求数：" + resultCollector.getTotalRequests());
        System.out.println("成功请求数：" + resultCollector.getSuccessfulRequests());
        System.out.println("失败请求数：" + resultCollector.getFailedRequests());
        System.out.println("平均响应时间：" + resultCollector.getAverageResponseTime() + "ms");
        System.out.println("最大响应时间：" + resultCollector.getMaxResponseTime() + "ms");
        System.out.println("最小响应时间：" + resultCollector.getMinResponseTime() + "ms");
        System.out.println("95%响应时间：" + resultCollector.getPercentileResponseTime(95) + "ms");
        System.out.println("99%响应时间：" + resultCollector.getPercentileResponseTime(99) + "ms");
        
        // 验证性能要求
        assertTrue(resultCollector.getAverageResponseTime() < 1000, 
            "平均响应时间应小于1秒");
        assertTrue(resultCollector.getPercentileResponseTime(95) < 2000, 
            "95%的请求响应时间应小于2秒");
        assertTrue(resultCollector.getPercentileResponseTime(99) < 3000, 
            "99%的请求响应时间应小于3秒");
        assertTrue(resultCollector.getFailedRequests() < resultCollector.getTotalRequests() * 0.01, 
            "失败率应小于1%");
        
        // 关闭线程池
        executorService.shutdown();
    }
    
    private void performSearch(TestResult result) {
        String query = "测试查询" + new Random().nextInt(DOCUMENT_COUNT);
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .searchType(SearchRequest.SearchType.HYBRID)
            .size(10)
            .minScore(0.5f)
            .build();
        
        SearchResponse results = searchService.search(request);
        assertNotNull(results);
        assertFalse(results.getResults().isEmpty());
    }
    
    private void performSync(TestResult result) {
        List<Document> documents = IntStream.range(0, 10)
            .mapToObj(i -> Document.builder()
                .id((long) i)
                .name("同步文档" + i)
                .content("这是同步文档" + i + "的内容")
                .build())
            .collect(Collectors.toList());
        
        for (Document doc : documents) {
            syncService.indexDocument(doc);
        }
    }
    
    private void performRebuild(TestResult result) {
        syncService.reindexAll();
    }
    
    private enum TestType {
        SEARCH, SYNC, REBUILD;
        
        private static final Random RANDOM = new Random();
        
        public static TestType random() {
            TestType[] values = values();
            return values[RANDOM.nextInt(values.length)];
        }
    }
    
    private static class TestResult {
        private final List<Long> responseTimes = new ArrayList<>();
        private int successfulRequests = 0;
        private int failedRequests = 0;
        
        public void recordSuccess(long responseTime) {
            responseTimes.add(responseTime);
            successfulRequests++;
        }
        
        public void recordFailure(long responseTime) {
            responseTimes.add(responseTime);
            failedRequests++;
        }
        
        public int getTotalRequests() {
            return successfulRequests + failedRequests;
        }
        
        public int getSuccessfulRequests() {
            return successfulRequests;
        }
        
        public int getFailedRequests() {
            return failedRequests;
        }
        
        public double getAverageResponseTime() {
            return responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        }
        
        public long getMaxResponseTime() {
            return responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
        }
        
        public long getMinResponseTime() {
            return responseTimes.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0);
        }
        
        public long getPercentileResponseTime(int percentile) {
            if (responseTimes.isEmpty()) {
                return 0;
            }
            
            List<Long> sortedTimes = new ArrayList<>(responseTimes);
            Collections.sort(sortedTimes);
            
            int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
            return sortedTimes.get(index);
        }
    }
    
    private static class TestResultCollector {
        private final List<TestResult> results = new ArrayList<>();
        
        public void addResult(TestResult result) {
            results.add(result);
        }
        
        public int getTotalRequests() {
            return results.stream()
                .mapToInt(TestResult::getTotalRequests)
                .sum();
        }
        
        public int getSuccessfulRequests() {
            return results.stream()
                .mapToInt(TestResult::getSuccessfulRequests)
                .sum();
        }
        
        public int getFailedRequests() {
            return results.stream()
                .mapToInt(TestResult::getFailedRequests)
                .sum();
        }
        
        public double getAverageResponseTime() {
            return results.stream()
                .mapToDouble(TestResult::getAverageResponseTime)
                .average()
                .orElse(0);
        }
        
        public long getMaxResponseTime() {
            return results.stream()
                .mapToLong(TestResult::getMaxResponseTime)
                .max()
                .orElse(0);
        }
        
        public long getMinResponseTime() {
            return results.stream()
                .mapToLong(TestResult::getMinResponseTime)
                .min()
                .orElse(0);
        }
        
        public long getPercentileResponseTime(int percentile) {
            List<Long> allResponseTimes = results.stream()
                .flatMap(result -> result.responseTimes.stream())
                .collect(Collectors.toList());
            
            if (allResponseTimes.isEmpty()) {
                return 0;
            }
            
            Collections.sort(allResponseTimes);
            int index = (int) Math.ceil(percentile / 100.0 * allResponseTimes.size()) - 1;
            return allResponseTimes.get(index);
        }
    }
} 