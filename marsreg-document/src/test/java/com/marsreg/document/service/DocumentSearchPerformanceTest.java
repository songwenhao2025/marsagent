package com.marsreg.document.service;

import com.marsreg.document.entity.Document;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.impl.DocumentSearchServiceImpl;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentSearchPerformanceTest {

    private DocumentSearchService searchService;
    private DocumentRepository documentRepository;
    private Directory directory;
    private IndexWriter indexWriter;
    private static final int DOCUMENT_COUNT = 1000;
    private static final int THREAD_COUNT = 10;
    private static final int SEARCH_COUNT = 100;
    private static final String TEST_INDEX_PATH = "test-index";

    @BeforeEach
    void setUp() throws IOException {
        // 清理 test-index 目录
        Path testIndexPath = Paths.get(TEST_INDEX_PATH);
        if (Files.exists(testIndexPath)) {
            Files.walk(testIndexPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(testIndexPath);

        // 初始化测试环境
        documentRepository = mock(DocumentRepository.class);
        directory = FSDirectory.open(testIndexPath);
        searchService = new DocumentSearchServiceImpl(documentRepository, TEST_INDEX_PATH);

        // 创建测试索引
        IndexWriterConfig config = new IndexWriterConfig();
        indexWriter = new IndexWriter(directory, config);

        // 添加测试数据
        addTestDocuments();
    }

    private void addTestDocuments() throws IOException {
        List<Document> documents = new ArrayList<>();
        // 生成测试文档
        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            Document doc = new Document();
            doc.setId((long) i);
            doc.setOriginalName("测试文档" + i);
            documents.add(doc);
            // 创建Lucene文档
            org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
            luceneDoc.add(new StringField("id", String.valueOf(i), Field.Store.YES));
            luceneDoc.add(new TextField("title", "测试文档" + i, Field.Store.YES));
            luceneDoc.add(new TextField("content", generateRandomContent(), Field.Store.YES));
            indexWriter.addDocument(luceneDoc);
        }
        indexWriter.commit();
        // 设置模拟返回值
        when(documentRepository.findAllById(any())).thenReturn(documents);
    }

    private String generateRandomContent() {
        String[] keywords = {"测试", "文档", "搜索", "性能", "优化", "系统", "开发", "项目", "代码", "实现"};
        StringBuilder content = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                content.append(keywords[random.nextInt(keywords.length)]);
                content.append(" ");
            }
            content.append("\n\n");
        }
        return content.toString();
    }

    @Test
    void testSearchPerformance() throws InterruptedException {
        // 准备测试数据
        List<String> searchTerms = Arrays.asList("测试", "文档", "搜索", "性能", "优化");
        Pageable pageable = PageRequest.of(0, 10);
        
        // 测试单线程性能
        System.out.println("开始单线程性能测试...");
        long singleThreadStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < SEARCH_COUNT; i++) {
            String term = searchTerms.get(i % searchTerms.size());
            searchService.search(term, pageable);
        }
        
        long singleThreadEndTime = System.currentTimeMillis();
        long singleThreadDuration = singleThreadEndTime - singleThreadStartTime;
        System.out.println("单线程测试完成，总耗时: " + singleThreadDuration + "ms");
        System.out.println("平均每次搜索耗时: " + (singleThreadDuration / (double)SEARCH_COUNT) + "ms");
        
        // 测试多线程性能
        System.out.println("\n开始多线程性能测试...");
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(SEARCH_COUNT);
        long multiThreadStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < SEARCH_COUNT; i++) {
            final String term = searchTerms.get(i % searchTerms.size());
            executorService.submit(() -> {
                try {
                    searchService.search(term, pageable);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long multiThreadEndTime = System.currentTimeMillis();
        long multiThreadDuration = multiThreadEndTime - multiThreadStartTime;
        
        System.out.println("多线程测试完成，总耗时: " + multiThreadDuration + "ms");
        System.out.println("平均每次搜索耗时: " + (multiThreadDuration / (double)SEARCH_COUNT) + "ms");
        
        // 测试缓存性能
        System.out.println("\n开始缓存性能测试...");
        long cacheStartTime = System.currentTimeMillis();
        
        // 第一次搜索（无缓存）
        searchService.search("测试", pageable);
        long firstSearchTime = System.currentTimeMillis() - cacheStartTime;
        
        // 第二次搜索（有缓存）
        long cacheSearchStartTime = System.currentTimeMillis();
        searchService.search("测试", pageable);
        long cacheSearchTime = System.currentTimeMillis() - cacheSearchStartTime;
        
        System.out.println("首次搜索耗时: " + firstSearchTime + "ms");
        System.out.println("缓存搜索耗时: " + cacheSearchTime + "ms");
        System.out.println("缓存提升比例: " + (1 - (double)cacheSearchTime / firstSearchTime) * 100 + "%");
        
        executorService.shutdown();
    }

    @Test
    void testConcurrentSearch() throws InterruptedException {
        int concurrentUsers = 50;
        int searchesPerUser = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<String> searchTerms = Arrays.asList("测试", "文档", "搜索", "性能", "优化");
        Pageable pageable = PageRequest.of(0, 10);
        
        System.out.println("开始并发性能测试...");
        System.out.println("并发用户数: " + concurrentUsers);
        System.out.println("每用户搜索次数: " + searchesPerUser);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentUsers; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < searchesPerUser; j++) {
                        String term = searchTerms.get(new Random().nextInt(searchTerms.size()));
                        searchService.search(term, pageable);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        int totalSearches = concurrentUsers * searchesPerUser;
        
        System.out.println("并发测试完成");
        System.out.println("总耗时: " + totalDuration + "ms");
        System.out.println("总搜索次数: " + totalSearches);
        System.out.println("平均每次搜索耗时: " + (totalDuration / (double)totalSearches) + "ms");
        System.out.println("每秒处理搜索数: " + (totalSearches * 1000.0 / totalDuration));
        
        executorService.shutdown();
    }

    @Test
    void testSearchWithDifferentPageSizes() {
        List<Integer> pageSizes = Arrays.asList(10, 20, 50, 100);
        String searchTerm = "测试";
        
        System.out.println("开始不同页面大小性能测试...");
        
        for (Integer size : pageSizes) {
            Pageable pageable = PageRequest.of(0, size);
            long startTime = System.currentTimeMillis();
            
            searchService.search(searchTerm, pageable);
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("页面大小 " + size + " 的搜索耗时: " + duration + "ms");
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
        if (directory != null) {
            directory.close();
        }
        // 删除锁文件
        Path lockFile = Paths.get(TEST_INDEX_PATH, "write.lock");
        try {
            Files.deleteIfExists(lockFile);
        } catch (IOException e) {
            System.err.println("删除write.lock失败: " + e.getMessage());
        }
    }
} 