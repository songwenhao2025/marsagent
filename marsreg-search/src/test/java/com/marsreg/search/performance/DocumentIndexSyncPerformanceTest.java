package com.marsreg.search.performance;

import com.marsreg.document.model.Document;
import com.marsreg.document.service.DocumentService;
import com.marsreg.search.config.IntegrationTestConfig;
import com.marsreg.search.model.DocumentIndex;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.DocumentIndexSyncService;
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
public class DocumentIndexSyncPerformanceTest {

    @Autowired
    private DocumentIndexSyncService syncService;

    @Autowired
    private DocumentIndexRepository documentIndexRepository;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private VectorizationService vectorizationService;

    @MockBean
    private VectorStorageService vectorStorageService;

    private static final int DOCUMENT_COUNT = 1000;
    private static final int BATCH_SIZE = 100;
    private static final int CONCURRENT_SYNCS = 5;
    private static final float[] TEST_VECTOR = new float[384];

    @BeforeEach
    void setUp() {
        // 初始化测试向量
        Arrays.fill(TEST_VECTOR, 0.1f);
        
        // 清空文档索引
        documentIndexRepository.deleteAll();
        
        // 设置向量化服务模拟行为
        when(vectorizationService.vectorize(anyString())).thenReturn(TEST_VECTOR);
        
        // 设置向量存储服务模拟行为
        doNothing().when(vectorStorageService).store(anyString(), any());
    }

    @Test
    void testBatchSyncPerformance() {
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
        
        // 执行批量同步
        long startTime = System.currentTimeMillis();
        syncService.batchSyncDocuments(documents);
        long endTime = System.currentTimeMillis();
        
        // 验证结果
        List<DocumentIndex> indices = documentIndexRepository.findAll();
        assertEquals(DOCUMENT_COUNT, indices.size());
        
        // 输出性能指标
        System.out.println("批量同步性能测试结果：");
        System.out.println("文档数量：" + DOCUMENT_COUNT);
        System.out.println("总耗时：" + (endTime - startTime) + "ms");
        System.out.println("平均每个文档耗时：" + 
            ((endTime - startTime) * 1.0 / DOCUMENT_COUNT) + "ms");
        
        // 验证性能要求
        assertTrue((endTime - startTime) < 30000, 
            "批量同步1000个文档应在30秒内完成");
    }

    @Test
    void testConcurrentSyncPerformance() throws InterruptedException {
        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_SYNCS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_SYNCS);
        List<Future<Long>> futures = new ArrayList<>();
        
        // 为每个线程创建同步任务
        for (int i = 0; i < CONCURRENT_SYNCS; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> {
                try {
                    // 准备测试数据
                    List<Document> documents = IntStream.range(0, BATCH_SIZE)
                        .mapToObj(j -> Document.builder()
                            .id("test-doc-" + threadId + "-" + j)
                            .title("测试文档" + threadId + "-" + j)
                            .content("这是测试文档" + threadId + "-" + j + "的内容")
                            .build())
                        .collect(Collectors.toList());
                    
                    // 设置文档服务模拟行为
                    documents.forEach(doc -> 
                        when(documentService.getDocument(doc.getId()))
                            .thenReturn(Optional.of(doc)));
                    
                    // 执行同步
                    long startTime = System.currentTimeMillis();
                    syncService.batchSyncDocuments(documents);
                    long endTime = System.currentTimeMillis();
                    
                    return endTime - startTime;
                } finally {
                    latch.countDown();
                }
            }));
        }
        
        // 等待所有任务完成
        latch.await();
        
        // 收集所有响应时间
        List<Long> responseTimes = futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return 0L;
                }
            })
            .collect(Collectors.toList());
        
        // 计算性能指标
        double avgResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        // 验证结果
        List<DocumentIndex> indices = documentIndexRepository.findAll();
        assertEquals(CONCURRENT_SYNCS * BATCH_SIZE, indices.size());
        
        // 输出性能指标
        System.out.println("并发同步性能测试结果：");
        System.out.println("并发数：" + CONCURRENT_SYNCS);
        System.out.println("每批文档数：" + BATCH_SIZE);
        System.out.println("平均响应时间：" + avgResponseTime + "ms");
        System.out.println("最大响应时间：" + maxResponseTime + "ms");
        
        // 验证性能要求
        assertTrue(avgResponseTime < 5000, 
            "平均每批同步时间应小于5秒");
        assertTrue(maxResponseTime < 10000, 
            "最大同步时间应小于10秒");
        
        // 关闭线程池
        executorService.shutdown();
    }

    @Test
    void testRebuildIndexPerformance() {
        // 准备测试数据
        List<Document> documents = IntStream.range(0, DOCUMENT_COUNT)
            .mapToObj(i -> Document.builder()
                .id("test-doc-" + i)
                .title("测试文档" + i)
                .content("这是测试文档" + i + "的内容")
                .build())
            .collect(Collectors.toList());
        
        // 设置文档服务模拟行为
        when(documentService.getAllDocuments()).thenReturn(documents);
        documents.forEach(doc -> 
            when(documentService.getDocument(doc.getId())).thenReturn(Optional.of(doc)));
        
        // 执行重建索引
        long startTime = System.currentTimeMillis();
        syncService.rebuildIndex();
        long endTime = System.currentTimeMillis();
        
        // 验证结果
        List<DocumentIndex> indices = documentIndexRepository.findAll();
        assertEquals(DOCUMENT_COUNT, indices.size());
        
        // 输出性能指标
        System.out.println("重建索引性能测试结果：");
        System.out.println("文档数量：" + DOCUMENT_COUNT);
        System.out.println("总耗时：" + (endTime - startTime) + "ms");
        System.out.println("平均每个文档耗时：" + 
            ((endTime - startTime) * 1.0 / DOCUMENT_COUNT) + "ms");
        
        // 验证性能要求
        assertTrue((endTime - startTime) < 60000, 
            "重建1000个文档的索引应在60秒内完成");
    }
} 