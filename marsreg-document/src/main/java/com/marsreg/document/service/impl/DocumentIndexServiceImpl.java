package com.marsreg.document.service.impl;

import com.marsreg.document.config.CacheConfig;
import com.marsreg.document.config.IndexConfig;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.exception.DocumentIndexException;
import com.marsreg.document.repository.MarsregDocumentRepository;
import com.marsreg.document.service.DocumentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexServiceImpl implements DocumentIndexService {

    private final IndexConfig indexConfig;
    private final MarsregDocumentRepository documentRepository;

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private final Map<Long, List<String>> pendingIndexes = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private final ReentrantLock indexLock = new ReentrantLock();
    private String indexPath = "index";
    private static final int BATCH_SIZE = 100;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void init() throws IOException {
        try {
            directory = FSDirectory.open(Paths.get(indexPath));
            analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            indexWriter = new IndexWriter(directory, config);
            log.info("索引服务初始化成功");
        } catch (IOException e) {
            log.error("索引服务初始化失败", e);
            throw new DocumentIndexException("DOC_INDEX_001", "索引服务初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() throws IOException {
        try {
            if (indexWriter != null) {
                indexWriter.close();
            }
            if (directory != null) {
                directory.close();
            }
            executorService.shutdown();
            log.info("索引服务关闭成功");
        } catch (IOException e) {
            log.error("索引服务关闭失败", e);
            throw new DocumentIndexException("DOC_INDEX_002", "索引服务关闭失败", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void indexDocument(DocumentEntity document, List<String> fields) {
        try {
            indexLock.lock();
            Document doc = createLuceneDocument(document);
            indexWriter.addDocument(doc);
            indexWriter.commit();
            log.info("文档索引成功: {}", document.getId());
        } catch (IOException e) {
            log.error("文档索引失败: {}", document.getId(), e);
            throw new DocumentIndexException("DOC_INDEX_003", "文档索引失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void batchIndex(Iterable<DocumentEntity> documents) {
        try {
            indexLock.lock();
            List<Document> docs = new ArrayList<>();
            for (DocumentEntity document : documents) {
                docs.add(createLuceneDocument(document));
                if (docs.size() >= BATCH_SIZE) {
                    indexWriter.addDocuments(docs);
                    docs.clear();
                }
            }
            if (!docs.isEmpty()) {
                indexWriter.addDocuments(docs);
            }
            indexWriter.commit();
            log.info("批量索引文档成功");
        } catch (IOException e) {
            log.error("批量索引文档失败", e);
            throw new DocumentIndexException("DOC_INDEX_004", "批量索引文档失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void updateIndex(DocumentEntity document, List<String> fields) {
        try {
            indexLock.lock();
            // 删除旧文档
            indexWriter.deleteDocuments(new Term("id", document.getId().toString()));

            // 创建新的Lucene文档
            Document doc = createLuceneDocument(document);
            indexWriter.addDocument(doc);
            indexWriter.commit();
            log.info("更新索引成功: {}", document.getId());
        } catch (IOException e) {
            log.error("更新索引失败: {}", document.getId(), e);
            throw new DocumentIndexException("DOC_INDEX_005", "更新索引失败", e);
        } finally {
            indexLock.unlock();
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
            indexLock.lock();
            indexWriter.deleteDocuments(new Term("id", documentId.toString()));
            indexWriter.commit();
            log.info("删除索引成功: {}", documentId);
        } catch (IOException e) {
            log.error("删除索引失败: {}", documentId, e);
            throw new DocumentIndexException("DOC_INDEX_006", "删除索引失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void indexDocuments(List<DocumentEntity> documents, Map<Long, List<String>> fieldsMap) {
        try {
            indexLock.lock();
            List<Document> docs = new ArrayList<>();
            for (DocumentEntity document : documents) {
                List<String> fields = fieldsMap.get(document.getId());
                if (fields != null) {
                    docs.add(createLuceneDocument(document));
                    if (docs.size() >= BATCH_SIZE) {
                        indexWriter.addDocuments(docs);
                        docs.clear();
                    }
                }
            }
            if (!docs.isEmpty()) {
                indexWriter.addDocuments(docs);
            }
            indexWriter.commit();
            log.info("批量索引文档成功");
        } catch (IOException e) {
            log.error("批量索引文档失败", e);
            throw new DocumentIndexException("DOC_INDEX_007", "批量索引文档失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheConfig.DOCUMENT_CACHE,
            CacheConfig.DOCUMENT_PROCESS_CACHE,
            CacheConfig.DOCUMENT_VERSION_CACHE
    }, allEntries = true)
    public void updateIndices(List<DocumentEntity> documents, Map<Long, List<String>> chunksMap) {
        try {
            indexLock.lock();
            List<Document> docs = new ArrayList<>();
            for (DocumentEntity document : documents) {
                // 删除旧文档
                indexWriter.deleteDocuments(new Term("id", document.getId().toString()));

                // 创建新的Lucene文档
                docs.add(createLuceneDocument(document));
                if (docs.size() >= BATCH_SIZE) {
                    indexWriter.addDocuments(docs);
                    docs.clear();
                }
            }
            if (!docs.isEmpty()) {
                indexWriter.addDocuments(docs);
            }
            indexWriter.commit();
            log.info("批量更新索引成功");
        } catch (IOException e) {
            log.error("批量更新索引失败", e);
            throw new DocumentIndexException("DOC_INDEX_008", "批量更新索引失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    @Override
    public void refreshIndex() {
        try {
            indexLock.lock();
            indexWriter.commit();
            log.info("刷新索引成功");
        } catch (IOException e) {
            log.error("刷新索引失败", e);
            throw new DocumentIndexException("DOC_INDEX_009", "刷新索引失败", e);
        } finally {
            indexLock.unlock();
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
            indexLock.lock();
            // 删除所有索引
            indexWriter.deleteAll();
            indexWriter.commit();

            // 重新索引所有文档
            List<DocumentEntity> allDocs = new ArrayList<>();
            documentRepository.findAll().forEach(doc -> allDocs.add(convertToDocumentEntity(doc)));
            if (!allDocs.isEmpty()) {
                Map<Long, List<String>> fieldsMap = new HashMap<>();
                for (DocumentEntity doc : allDocs) {
                    fieldsMap.put(doc.getId(), Collections.emptyList());
                }
                indexDocuments(allDocs, fieldsMap);
            }
            log.info("重建索引成功");
        } catch (IOException e) {
            log.error("重建索引失败", e);
            throw new DocumentIndexException("DOC_INDEX_010", "重建索引失败", e);
        } finally {
            indexLock.unlock();
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
            reader.close();
            log.info("获取索引统计信息成功");
        } catch (IOException e) {
            log.error("获取索引统计信息失败", e);
            throw new DocumentIndexException("DOC_INDEX_011", "获取索引统计信息失败", e);
        }
        return stats;
    }

    @Override
    public void optimizeIndex() {
        try {
            indexLock.lock();
            indexWriter.forceMerge(1);
            indexWriter.commit();
            log.info("优化索引成功");
        } catch (IOException e) {
            log.error("优化索引失败", e);
            throw new DocumentIndexException("DOC_INDEX_012", "优化索引失败", e);
        } finally {
            indexLock.unlock();
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
            reader.close();
            log.info("检查索引状态成功");
        } catch (IOException e) {
            log.error("检查索引状态失败", e);
            throw new DocumentIndexException("DOC_INDEX_013", "检查索引状态失败", e);
        }
        return status;
    }

    @Override
    public void clearDocumentCache(Long documentId) {
        // 缓存清除由注解处理
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
            indexLock.lock();
            for (Long documentId : documentIds) {
                indexWriter.deleteDocuments(new Term("id", documentId.toString()));
            }
            indexWriter.commit();
            log.info("批量删除索引成功");
        } catch (IOException e) {
            log.error("批量删除索引失败", e);
            throw new DocumentIndexException("DOC_INDEX_014", "批量删除索引失败", e);
        } finally {
            indexLock.unlock();
        }
    }

    @Override
    public boolean isIndexed(Long documentId) {
        try {
            IndexReader reader = DirectoryReader.open(directory);
            try {
                IndexSearcher searcher = new IndexSearcher(reader);
                TermQuery query = new TermQuery(new Term("id", documentId.toString()));
                TopDocs results = searcher.search(query, 1);
                return results.totalHits.value > 0;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            log.error("检查文档索引状态失败: {}", documentId, e);
            throw new DocumentIndexException("DOC_INDEX_015", "检查文档索引状态失败", e);
        }
    }

    private Document createLuceneDocument(DocumentEntity document) {
        Document doc = new Document();
        // 添加必填字段
        doc.add(new StringField("id", document.getId().toString(), Field.Store.YES));
        
        // 添加可选字段，并进行空值检查
        if (document.getName() != null) {
            doc.add(new TextField("name", document.getName(), Field.Store.YES));
        }
        if (document.getOriginalName() != null) {
            doc.add(new TextField("originalName", document.getOriginalName(), Field.Store.YES));
        }
        if (document.getContent() != null) {
            doc.add(new TextField("content", document.getContent(), Field.Store.YES));
        }
        if (document.getContentType() != null) {
            doc.add(new StringField("contentType", document.getContentType(), Field.Store.YES));
        }
        if (document.getSize() != null) {
            doc.add(new StringField("size", document.getSize().toString(), Field.Store.YES));
        }
        if (document.getObjectName() != null) {
            doc.add(new StringField("objectName", document.getObjectName(), Field.Store.YES));
        }
        if (document.getStoragePath() != null) {
            doc.add(new StringField("storagePath", document.getStoragePath(), Field.Store.YES));
        }
        if (document.getBucket() != null) {
            doc.add(new StringField("bucket", document.getBucket(), Field.Store.YES));
        }
        if (document.getMd5() != null) {
            doc.add(new StringField("md5", document.getMd5(), Field.Store.YES));
        }
        if (document.getStatus() != null) {
            doc.add(new StringField("status", document.getStatus().name(), Field.Store.YES));
        }
        if (document.getCreatedAt() != null) {
            doc.add(new StringField("createdAt", document.getCreatedAt().toString(), Field.Store.YES));
        }
        if (document.getUpdatedAt() != null) {
            doc.add(new StringField("updatedAt", document.getUpdatedAt().toString(), Field.Store.YES));
        }
        if (document.getCreatedBy() != null) {
            doc.add(new StringField("createdBy", document.getCreatedBy(), Field.Store.YES));
        }
        if (document.getUpdatedBy() != null) {
            doc.add(new StringField("updatedBy", document.getUpdatedBy(), Field.Store.YES));
        }
        if (document.getCategory() != null) {
            doc.add(new StringField("category", document.getCategory(), Field.Store.YES));
        }
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            for (String tag : document.getTags()) {
                doc.add(new StringField("tags", tag, Field.Store.YES));
            }
        }
        if (document.getErrorMessage() != null) {
            doc.add(new TextField("errorMessage", document.getErrorMessage(), Field.Store.YES));
        }
        return doc;
    }

    private DocumentEntity convertToDocumentEntity(Object doc) {
        DocumentEntity entity = new DocumentEntity();
        // 假设 doc 是 MarsregDocument 类型，进行类型转换
        if (doc instanceof DocumentEntity) {
            DocumentEntity marsregDoc = (DocumentEntity) doc;
            entity.setId(marsregDoc.getId());
            entity.setName(marsregDoc.getName());
            entity.setOriginalName(marsregDoc.getOriginalName());
            entity.setContentType(marsregDoc.getContentType());
            entity.setSize(marsregDoc.getSize());
            entity.setObjectName(marsregDoc.getObjectName());
            entity.setStoragePath(marsregDoc.getStoragePath());
            entity.setBucket(marsregDoc.getBucket());
            entity.setMd5(marsregDoc.getMd5());
            entity.setStatus(marsregDoc.getStatus());
            entity.setCreatedAt(marsregDoc.getCreatedAt());
            entity.setUpdatedAt(marsregDoc.getUpdatedAt());
            entity.setCreatedBy(marsregDoc.getCreatedBy());
            entity.setUpdatedBy(marsregDoc.getUpdatedBy());
            entity.setCategory(marsregDoc.getCategory());
            entity.setTags(marsregDoc.getTags());
            entity.setContent(marsregDoc.getContent());
            entity.setErrorMessage(marsregDoc.getErrorMessage());
            entity.setProcessedTime(marsregDoc.getProcessedTime());
        }
        return entity;
    }

    @Override
    public void updateDocumentIndex(DocumentEntity document) {
        // 实现更新文档索引的逻辑
    }
} 