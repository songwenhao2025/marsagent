package com.marsreg.document.service.impl;

import com.marsreg.document.config.CacheConfig;
import com.marsreg.document.config.IndexConfig;
import com.marsreg.document.entity.Document;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentIndexService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexServiceImpl implements DocumentIndexService {

    private final IndexConfig indexConfig;
    private final DocumentRepository documentRepository;
    private final VectorizationService vectorizationService;
    private final VectorStorageService vectorStorageService;
    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private final Map<Long, List<String>> pendingIndexes = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private String indexPath = "index";
    private static final int BATCH_SIZE = 100;

    public DocumentIndexServiceImpl(IndexConfig indexConfig,
                                  DocumentRepository documentRepository,
                                  VectorizationService vectorizationService,
                                  VectorStorageService vectorStorageService) {
        this(indexConfig, documentRepository, vectorizationService, vectorStorageService, "index");
    }

    public DocumentIndexServiceImpl(IndexConfig indexConfig,
                                  DocumentRepository documentRepository,
                                  VectorizationService vectorizationService,
                                  VectorStorageService vectorStorageService,
                                  String indexPath) {
        this.indexConfig = indexConfig;
        this.documentRepository = documentRepository;
        this.vectorizationService = vectorizationService;
        this.vectorStorageService = vectorStorageService;
        this.indexPath = indexPath;
        this.executorService = Executors.newFixedThreadPool(indexConfig.getThreadPoolSize());
        try {
            this.directory = FSDirectory.open(Paths.get(indexPath));
            this.analyzer = new StandardAnalyzer();
        } catch (IOException e) {
            log.error("初始化索引服务失败", e);
        }
    }

    @PostConstruct
    public void init() throws IOException {
        // 初始化Lucene索引
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
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void indexDocument(Document document, List<String> chunks) {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(directory, config);

            // 创建Lucene文档
            org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
            luceneDoc.add(new StringField("id", document.getId().toString(), Field.Store.YES));
            luceneDoc.add(new TextField("name", document.getName(), Field.Store.YES));
            luceneDoc.add(new TextField("originalName", document.getOriginalName(), Field.Store.YES));
            luceneDoc.add(new StringField("contentType", document.getContentType(), Field.Store.YES));
            luceneDoc.add(new LongPoint("size", document.getSize()));
            luceneDoc.add(new StringField("storagePath", document.getStoragePath(), Field.Store.YES));
            luceneDoc.add(new StringField("md5", document.getMd5(), Field.Store.YES));
            luceneDoc.add(new StringField("status", document.getStatus().toString(), Field.Store.YES));
            luceneDoc.add(new StringField("bucket", document.getBucket(), Field.Store.YES));
            luceneDoc.add(new StringField("objectName", document.getObjectName(), Field.Store.YES));
            luceneDoc.add(new LongPoint("createTime", document.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC)));
            luceneDoc.add(new LongPoint("updateTime", document.getUpdateTime().toEpochSecond(java.time.ZoneOffset.UTC)));

            // 添加分块内容
            for (int i = 0; i < chunks.size(); i++) {
                luceneDoc.add(new TextField("chunk_" + i, chunks.get(i), Field.Store.YES));
            }

            // 添加文档到索引
            writer.addDocument(luceneDoc);

            // 向量化并存储
            float[] vector = vectorizationService.vectorize(document.getName() + " " + document.getOriginalName());
            vectorStorageService.store(document.getId().toString(), vector);

            writer.commit();
            writer.close();
        } catch (IOException e) {
            log.error("索引文档失败", e);
            throw new RuntimeException("索引文档失败", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void indexDocuments(List<Document> documents, Map<Long, List<String>> chunksMap) {
        try {
            // 分批处理文档
            List<List<Document>> batches = new ArrayList<>();
            for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
                batches.add(documents.subList(i, Math.min(i + BATCH_SIZE, documents.size())));
            }

            // 并行处理每个批次
            List<CompletableFuture<Void>> futures = batches.stream()
                    .map(batch -> CompletableFuture.runAsync(() -> {
                        try {
                            // 创建索引写入器
                            IndexWriterConfig config = new IndexWriterConfig(analyzer);
                            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                            IndexWriter writer = new IndexWriter(directory, config);

                            // 处理批次中的每个文档
                            for (Document doc : batch) {
                                List<String> chunks = chunksMap.get(doc.getId());
                                if (chunks != null) {
                                    // 创建Lucene文档
                                    org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
                                    luceneDoc.add(new StringField("id", doc.getId().toString(), Field.Store.YES));
                                    luceneDoc.add(new TextField("name", doc.getName(), Field.Store.YES));
                                    luceneDoc.add(new TextField("originalName", doc.getOriginalName(), Field.Store.YES));
                                    luceneDoc.add(new StringField("contentType", doc.getContentType(), Field.Store.YES));
                                    luceneDoc.add(new LongPoint("size", doc.getSize()));
                                    luceneDoc.add(new StringField("storagePath", doc.getStoragePath(), Field.Store.YES));
                                    luceneDoc.add(new StringField("md5", doc.getMd5(), Field.Store.YES));
                                    luceneDoc.add(new StringField("status", doc.getStatus().toString(), Field.Store.YES));
                                    luceneDoc.add(new StringField("bucket", doc.getBucket(), Field.Store.YES));
                                    luceneDoc.add(new StringField("objectName", doc.getObjectName(), Field.Store.YES));
                                    luceneDoc.add(new LongPoint("createTime", doc.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC)));
                                    luceneDoc.add(new LongPoint("updateTime", doc.getUpdateTime().toEpochSecond(java.time.ZoneOffset.UTC)));

                                    // 添加分块内容
                                    for (int i = 0; i < chunks.size(); i++) {
                                        luceneDoc.add(new TextField("chunk_" + i, chunks.get(i), Field.Store.YES));
                                    }

                                    // 添加文档到索引
                                    writer.addDocument(luceneDoc);

                                    // 向量化并存储
                                    float[] vector = vectorizationService.vectorize(doc.getName() + " " + doc.getOriginalName());
                                    vectorStorageService.store(doc.getId().toString(), vector);
                                }
                            }

                            // 提交更改
                            writer.commit();
                            writer.close();
                        } catch (Exception e) {
                            log.error("批量索引处理失败", e);
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // 等待所有批次处理完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("批量索引失败", e);
            throw new RuntimeException("批量索引失败", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void deleteIndex(Long documentId) {
        try {
            indexWriter.deleteDocuments(new Term("id", documentId.toString()));
            indexWriter.commit();
            vectorStorageService.delete(documentId.toString());
        } catch (IOException e) {
            log.error("删除索引失败", e);
            throw new RuntimeException("删除索引失败", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void deleteIndices(List<Long> documentIds) {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(directory, config);

            // 删除文档
            for (Long docId : documentIds) {
                writer.deleteDocuments(new Term("id", docId.toString()));
                vectorStorageService.delete(docId.toString());
            }

            writer.commit();
            writer.close();
        } catch (IOException e) {
            log.error("删除索引失败", e);
            throw new RuntimeException("删除索引失败", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void updateIndex(Document document, List<String> chunks) {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(directory, config);

            // 删除旧文档
            writer.deleteDocuments(new Term("id", document.getId().toString()));

            // 创建新的Lucene文档
            org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
            luceneDoc.add(new StringField("id", document.getId().toString(), Field.Store.YES));
            luceneDoc.add(new TextField("name", document.getName(), Field.Store.YES));
            luceneDoc.add(new TextField("originalName", document.getOriginalName(), Field.Store.YES));
            luceneDoc.add(new StringField("contentType", document.getContentType(), Field.Store.YES));
            luceneDoc.add(new LongPoint("size", document.getSize()));
            luceneDoc.add(new StringField("storagePath", document.getStoragePath(), Field.Store.YES));
            luceneDoc.add(new StringField("md5", document.getMd5(), Field.Store.YES));
            luceneDoc.add(new StringField("status", document.getStatus().toString(), Field.Store.YES));
            luceneDoc.add(new StringField("bucket", document.getBucket(), Field.Store.YES));
            luceneDoc.add(new StringField("objectName", document.getObjectName(), Field.Store.YES));
            luceneDoc.add(new LongPoint("createTime", document.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC)));
            luceneDoc.add(new LongPoint("updateTime", document.getUpdateTime().toEpochSecond(java.time.ZoneOffset.UTC)));

            // 添加分块内容
            for (int i = 0; i < chunks.size(); i++) {
                luceneDoc.add(new TextField("chunk_" + i, chunks.get(i), Field.Store.YES));
            }

            // 添加文档到索引
            writer.addDocument(luceneDoc);

            // 更新向量存储
            float[] vector = vectorizationService.vectorize(document.getName() + " " + document.getOriginalName());
            vectorStorageService.updateVector(document.getId().toString(), vector);

            writer.commit();
            writer.close();
        } catch (IOException e) {
            log.error("更新索引失败", e);
            throw new RuntimeException("更新索引失败", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void updateIndices(List<Document> documents, Map<Long, List<String>> chunksMap) {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(directory, config);

            for (Document doc : documents) {
                // 删除旧文档
                writer.deleteDocuments(new Term("id", doc.getId().toString()));

                // 创建新的Lucene文档
                org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
                luceneDoc.add(new StringField("id", doc.getId().toString(), Field.Store.YES));
                luceneDoc.add(new TextField("name", doc.getName(), Field.Store.YES));
                luceneDoc.add(new TextField("originalName", doc.getOriginalName(), Field.Store.YES));
                luceneDoc.add(new StringField("contentType", doc.getContentType(), Field.Store.YES));
                luceneDoc.add(new LongPoint("size", doc.getSize()));
                luceneDoc.add(new StringField("storagePath", doc.getStoragePath(), Field.Store.YES));
                luceneDoc.add(new StringField("md5", doc.getMd5(), Field.Store.YES));
                luceneDoc.add(new StringField("status", doc.getStatus().toString(), Field.Store.YES));
                luceneDoc.add(new StringField("bucket", doc.getBucket(), Field.Store.YES));
                luceneDoc.add(new StringField("objectName", doc.getObjectName(), Field.Store.YES));
                luceneDoc.add(new LongPoint("createTime", doc.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC)));
                luceneDoc.add(new LongPoint("updateTime", doc.getUpdateTime().toEpochSecond(java.time.ZoneOffset.UTC)));

                // 添加分块内容
                List<String> chunks = chunksMap.get(doc.getId());
                if (chunks != null) {
                    for (int i = 0; i < chunks.size(); i++) {
                        luceneDoc.add(new TextField("chunk_" + i, chunks.get(i), Field.Store.YES));
                    }
                }

                // 添加文档到索引
                writer.addDocument(luceneDoc);

                // 更新向量存储
                float[] vector = vectorizationService.vectorize(doc.getName() + " " + doc.getOriginalName());
                vectorStorageService.updateVector(doc.getId().toString(), vector);
            }

            writer.commit();
            writer.close();
        } catch (IOException e) {
            log.error("批量更新索引失败", e);
            throw new RuntimeException("批量更新索引失败", e);
        }
    }

    @Override
    public List<Long> search(String query, int page, int size) {
        try {
            // 创建查询
            Query q = new FuzzyQuery(new Term("name", query.toLowerCase()));
            
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
    public void refreshIndex() {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(directory, config);
            writer.commit();
            writer.close();
        } catch (IOException e) {
            log.error("刷新索引失败", e);
            throw new RuntimeException("刷新索引失败", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void rebuildIndex() {
        try {
            // 删除所有索引
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(directory, config);
            writer.deleteAll();
            writer.commit();
            writer.close();

            // 重新索引所有文档
            Page<Document> allDocs = documentRepository.findAll(Pageable.unpaged());
            if (allDocs.hasContent()) {
                Map<Long, List<String>> chunksMap = new HashMap<>();
                for (Document doc : allDocs.getContent()) {
                    chunksMap.put(doc.getId(), Collections.emptyList());
                }
                indexDocuments(allDocs.getContent(), chunksMap);
            }
        } catch (IOException e) {
            log.error("重建索引失败", e);
            throw new RuntimeException("重建索引失败", e);
        }
    }

    @Override
    public Map<String, Object> getIndexStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            IndexReader reader = DirectoryReader.open(directory);
            stats.put("numDocs", reader.numDocs());
            stats.put("maxDoc", reader.maxDoc());
            stats.put("numDeletedDocs", reader.numDeletedDocs());
            stats.put("hasDeletions", reader.hasDeletions());
            stats.put("isOptimized", reader.numDocs() == reader.maxDoc());
            
            // 获取字段统计信息
            Map<String, Long> fieldStats = new HashMap<>();
            for (LeafReaderContext context : reader.leaves()) {
                LeafReader leafReader = context.reader();
                FieldInfos fieldInfos = leafReader.getFieldInfos();
                for (FieldInfo fieldInfo : fieldInfos) {
                    String fieldName = fieldInfo.name;
                    Terms terms = leafReader.terms(fieldName);
                    if (terms != null) {
                        fieldStats.merge(fieldName, terms.size(), Long::sum);
                    }
                }
            }
            stats.put("fieldStats", fieldStats);
            
            reader.close();
        } catch (IOException e) {
            log.error("获取索引统计信息失败", e);
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void optimizeIndex() {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(directory, config);
            writer.forceMerge(1); // 合并所有段
            writer.close();
            log.info("索引优化完成");
        } catch (IOException e) {
            log.error("索引优化失败", e);
            throw new RuntimeException("索引优化失败", e);
        }
    }

    @Override
    public Map<String, Object> checkIndexStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            IndexReader reader = DirectoryReader.open(directory);
            status.put("numDocs", reader.numDocs());
            status.put("maxDoc", reader.maxDoc());
            status.put("numDeletedDocs", reader.numDeletedDocs());
            status.put("hasDeletions", reader.hasDeletions());
            status.put("isOptimized", reader.numDocs() == reader.maxDoc());
            reader.close();
        } catch (IOException e) {
            log.error("检查索引状态失败", e);
            status.put("error", e.getMessage());
        }
        return status;
    }
} 