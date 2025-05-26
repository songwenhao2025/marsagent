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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentSearchServiceTest {

    private DocumentSearchService searchService;
    private DocumentRepository documentRepository;
    private Directory directory;
    private IndexWriter indexWriter;

    @BeforeEach
    void setUp() throws IOException {
        // 初始化测试环境
        documentRepository = mock(DocumentRepository.class);
        directory = FSDirectory.open(Paths.get("test-index"));
        searchService = new DocumentSearchServiceImpl(documentRepository);
        
        // 创建测试索引
        IndexWriterConfig config = new IndexWriterConfig();
        indexWriter = new IndexWriter(directory, config);
        
        // 添加测试数据
        addTestDocuments();
    }

    private void addTestDocuments() throws IOException {
        // 创建测试文档
        Document doc1 = new Document();
        doc1.setId(1L);
        doc1.setOriginalName("测试文档1");
        
        Document doc2 = new Document();
        doc2.setId(2L);
        doc2.setOriginalName("测试文档2");
        
        // 添加到索引
        org.apache.lucene.document.Document luceneDoc1 = new org.apache.lucene.document.Document();
        luceneDoc1.add(new StringField("id", "1", Field.Store.YES));
        luceneDoc1.add(new TextField("title", "测试文档1", Field.Store.YES));
        luceneDoc1.add(new TextField("content", "这是一个测试文档，用于测试搜索功能。", Field.Store.YES));
        
        org.apache.lucene.document.Document luceneDoc2 = new org.apache.lucene.document.Document();
        luceneDoc2.add(new StringField("id", "2", Field.Store.YES));
        luceneDoc2.add(new TextField("title", "测试文档2", Field.Store.YES));
        luceneDoc2.add(new TextField("content", "这是另一个测试文档，包含一些关键词。", Field.Store.YES));
        
        indexWriter.addDocument(luceneDoc1);
        indexWriter.addDocument(luceneDoc2);
        indexWriter.commit();
        
        // 设置模拟返回值
        when(documentRepository.findAllById(any())).thenReturn(Arrays.asList(doc1, doc2));
    }

    @Test
    void testBasicSearch() {
        // 测试基本搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> results = searchService.search("测试", pageable);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.getTotalElements());
    }

    @Test
    void testHighlightSearch() {
        // 测试高亮搜索
        Pageable pageable = PageRequest.of(0, 10);
        Page<Map<String, Object>> results = searchService.searchWithHighlight("测试", pageable);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(2, results.getTotalElements());
        
        // 验证高亮结果
        results.getContent().forEach(result -> {
            assertTrue(result.containsKey("highlights"));
            Map<String, String[]> highlights = (Map<String, String[]>) result.get("highlights");
            assertTrue(highlights.containsKey("title") || highlights.containsKey("content"));
        });
    }

    @Test
    void testSearchSuggestions() {
        // 测试搜索建议
        List<String> suggestions = searchService.getSuggestions("测试", 5);
        
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("测试")));
    }

    @Test
    void testPerformance() {
        // 测试搜索性能
        Pageable pageable = PageRequest.of(0, 10);
        
        // 第一次搜索（无缓存）
        long startTime1 = System.currentTimeMillis();
        searchService.search("测试", pageable);
        long endTime1 = System.currentTimeMillis();
        
        // 第二次搜索（有缓存）
        long startTime2 = System.currentTimeMillis();
        searchService.search("测试", pageable);
        long endTime2 = System.currentTimeMillis();
        
        // 验证缓存效果
        assertTrue((endTime2 - startTime2) < (endTime1 - startTime1));
    }

    @Test
    void testFieldSpecificSearch() {
        // 测试特定字段搜索
        Pageable pageable = PageRequest.of(0, 10);
        
        // 测试标题搜索
        Page<Document> titleResults = searchService.searchTitle("测试", pageable);
        assertNotNull(titleResults);
        assertFalse(titleResults.isEmpty());
        
        // 测试内容搜索
        Page<Document> contentResults = searchService.searchContent("关键词", pageable);
        assertNotNull(contentResults);
        assertFalse(contentResults.isEmpty());
    }
} 