package com.marsreg.document.service.impl;

import com.marsreg.document.config.IndexConfig;
import com.marsreg.document.entity.Document;
import com.marsreg.document.service.DocumentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexServiceImpl implements DocumentIndexService {

    private final IndexConfig indexConfig;
    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private final Map<Long, List<String>> pendingIndexes = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);

    public DocumentIndexServiceImpl(IndexConfig indexConfig) {
        this.indexConfig = indexConfig;
        this.executorService = Executors.newFixedThreadPool(indexConfig.getThreadPoolSize());
    }

    @PostConstruct
    public void init() throws IOException {
        // 初始化Lucene索引
        directory = FSDirectory.open(Paths.get("index"));
        analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriter = new IndexWriter(directory, config);
    }

    @PreDestroy
    public void destroy() throws IOException {
        // 关闭资源
        if (indexWriter != null) {
            indexWriter.close();
        }
        if (analyzer != null) {
            analyzer.close();
        }
        if (directory != null) {
            directory.close();
        }
        executorService.shutdown();
    }

    @Override
    public void indexDocument(Document document, List<String> chunks) {
        if (!indexConfig.isEnabled()) {
            return;
        }

        if (indexConfig.isAsync()) {
            pendingIndexes.put(document.getId(), chunks);
            if (pendingIndexes.size() >= indexConfig.getBatchSize()) {
                processPendingIndexes();
            }
        } else {
            doIndexDocument(document, chunks);
        }
    }

    @Override
    public void indexDocuments(List<Document> documents, Map<Long, List<String>> chunksMap) {
        if (!indexConfig.isEnabled()) {
            return;
        }

        if (indexConfig.isAsync()) {
            pendingIndexes.putAll(chunksMap);
            if (pendingIndexes.size() >= indexConfig.getBatchSize()) {
                processPendingIndexes();
            }
        } else {
            for (Document document : documents) {
                List<String> chunks = chunksMap.get(document.getId());
                if (chunks != null) {
                    doIndexDocument(document, chunks);
                }
            }
        }
    }

    @Override
    public void deleteIndex(Long documentId) {
        try {
            indexWriter.deleteDocuments(new Term("id", documentId.toString()));
            indexWriter.commit();
        } catch (IOException e) {
            log.error("删除索引失败", e);
        }
    }

    @Override
    public List<Long> search(String query, int page, int size) {
        try {
            // 创建查询
            Query q = new FuzzyQuery(new Term("content", query.toLowerCase()));
            
            // 创建索引读取器
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                
                // 执行搜索
                TopDocs docs = searcher.search(q, page * size);
                ScoreDoc[] hits = docs.scoreDocs;
                
                // 收集结果
                List<Long> results = new ArrayList<>();
                int start = (page - 1) * size;
                int end = Math.min(start + size, hits.length);
                
                for (int i = start; i < end; i++) {
                    org.apache.lucene.document.Document luceneDoc = searcher.doc(hits[i].doc);
                    results.add(Long.parseLong(luceneDoc.get("id")));
                }
                
                return results;
            }
        } catch (IOException e) {
            log.error("搜索失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${marsreg.document.index.refresh-interval:300000}")
    public void refreshIndex() {
        if (!indexConfig.isEnabled() || pendingIndexes.isEmpty()) {
            return;
        }
        processPendingIndexes();
    }

    private void processPendingIndexes() {
        if (isIndexing.compareAndSet(false, true)) {
            executorService.submit(() -> {
                try {
                    Map<Long, List<String>> batch = new HashMap<>(pendingIndexes);
                    pendingIndexes.clear();
                    
                    for (Map.Entry<Long, List<String>> entry : batch.entrySet()) {
                        Document document = new Document();
                        document.setId(entry.getKey());
                        doIndexDocument(document, entry.getValue());
                    }
                } finally {
                    isIndexing.set(false);
                }
            });
        }
    }

    private void doIndexDocument(Document document, List<String> chunks) {
        try {
            // 删除旧索引
            indexWriter.deleteDocuments(new Term("id", document.getId().toString()));
            
            // 创建新文档
            org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
            doc.add(new StringField("id", document.getId().toString(), Field.Store.YES));
            doc.add(new TextField("title", document.getOriginalName(), Field.Store.YES));
            
            // 添加所有分块
            StringBuilder content = new StringBuilder();
            for (String chunk : chunks) {
                content.append(chunk).append(" ");
            }
            doc.add(new TextField("content", content.toString().toLowerCase(), Field.Store.NO));
            
            // 添加文档
            indexWriter.addDocument(doc);
            indexWriter.commit();
            
            log.debug("索引文档成功: {}", document.getId());
        } catch (IOException e) {
            log.error("索引文档失败", e);
        }
    }
} 