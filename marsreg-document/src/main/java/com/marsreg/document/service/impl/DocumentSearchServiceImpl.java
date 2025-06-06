package com.marsreg.document.service.impl;

import com.marsreg.document.config.SearchCacheConfig;
import com.marsreg.document.entity.Document;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentSearchService;
import com.marsreg.vector.service.VectorizationService;
import com.marsreg.vector.service.VectorStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentSearchServiceImpl implements DocumentSearchService {

    private final DocumentRepository documentRepository;
    private final VectorizationService vectorizationService;
    private final VectorStorageService vectorStorageService;
    private Directory directory;
    private Analyzer analyzer;
    private static final String[] HIGHLIGHT_FIELDS = {"title", "content"};
    private static final int FRAGMENT_SIZE = 150;
    private static final int MAX_NUM_FRAGMENTS = 3;
    private static final int BATCH_SIZE = 100;
    private static final Map<String, Set<String>> termCache = new ConcurrentHashMap<>();
    private String indexPath = "index";

    public DocumentSearchServiceImpl(DocumentRepository documentRepository,
                                   VectorizationService vectorizationService,
                                   VectorStorageService vectorStorageService) {
        this(documentRepository, vectorizationService, vectorStorageService, "index");
    }

    public DocumentSearchServiceImpl(DocumentRepository documentRepository,
                                   VectorizationService vectorizationService,
                                   VectorStorageService vectorStorageService,
                                   String indexPath) {
        this.documentRepository = documentRepository;
        this.vectorizationService = vectorizationService;
        this.vectorStorageService = vectorStorageService;
        this.indexPath = indexPath;
        try {
            this.directory = FSDirectory.open(Paths.get(indexPath));
            this.analyzer = new StandardAnalyzer();
            // 预热缓存
            warmupCache();
        } catch (IOException e) {
            log.error("初始化搜索服务失败", e);
        }
    }

    private void warmupCache() {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            for (String field : HIGHLIGHT_FIELDS) {
                Terms terms = MultiTerms.getTerms(reader, field);
                if (terms != null) {
                    Set<String> fieldTerms = new HashSet<>();
                    TermsEnum termsEnum = terms.iterator();
                    BytesRef term;
                    while ((term = termsEnum.next()) != null) {
                        fieldTerms.add(term.utf8ToString());
                    }
                    termCache.put(field, fieldTerms);
                }
            }
        } catch (IOException e) {
            log.error("预热缓存失败", e);
        }
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_RESULT_CACHE, key = "#query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Document> search(String query, Pageable pageable) {
        return doSearch(query, pageable, null);
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_RESULT_CACHE, key = "'content-' + #query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Document> searchContent(String query, Pageable pageable) {
        return doSearch(query, pageable, "content");
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_RESULT_CACHE, key = "'title-' + #query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Document> searchTitle(String query, Pageable pageable) {
        return doSearch(query, pageable, "title");
    }

    private Page<Document> doSearch(String query, Pageable pageable, String field) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建查询
            Query q;
            if (field != null) {
                q = new FuzzyQuery(new Term(field, query.toLowerCase()));
            } else {
                BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
                queryBuilder.add(new FuzzyQuery(new Term("title", query.toLowerCase())), BooleanClause.Occur.SHOULD);
                queryBuilder.add(new FuzzyQuery(new Term("content", query.toLowerCase())), BooleanClause.Occur.SHOULD);
                q = queryBuilder.build();
            }
            
            // 确保numHits大于0
            int numHits = Math.max(1, pageable.getPageSize());
            // 使用numHits替代pageable.getPageSize()
            TopDocs topDocs = searcher.search(q, numHits);
            ScoreDoc[] hits = topDocs.scoreDocs;
            
            // 批量获取文档
            List<Document> results = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), hits.length);
            
            // 批量处理
            List<Long> docIds = new ArrayList<>();
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                docIds.add(Long.parseLong(doc.get("id")));
            }
            
            // 批量查询数据库
            Map<Long, Document> docMap = documentRepository.findAllById(docIds)
                    .stream()
                    .collect(Collectors.toMap(Document::getId, doc -> doc));
            
            // 按原始顺序组装结果
            for (Long id : docIds) {
                Document doc = docMap.get(id);
                if (doc != null) {
                    results.add(doc);
                }
            }
            
            return new PageImpl<>(results, pageable, topDocs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_HIGHLIGHT_CACHE, key = "#query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Map<String, Object>> searchWithHighlight(String query, Pageable pageable) {
        return doSearchWithHighlight(query, pageable, null);
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_HIGHLIGHT_CACHE, key = "'content-' + #query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Map<String, Object>> searchContentWithHighlight(String query, Pageable pageable) {
        return doSearchWithHighlight(query, pageable, "content");
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_HIGHLIGHT_CACHE, key = "'title-' + #query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Map<String, Object>> searchTitleWithHighlight(String query, Pageable pageable) {
        return doSearchWithHighlight(query, pageable, "title");
    }

    private Page<Map<String, Object>> doSearchWithHighlight(String query, Pageable pageable, String field) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建查询
            Query q;
            if (field != null) {
                q = new FuzzyQuery(new Term(field, query.toLowerCase()));
            } else {
                BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
                queryBuilder.add(new FuzzyQuery(new Term("title", query.toLowerCase())), BooleanClause.Occur.SHOULD);
                queryBuilder.add(new FuzzyQuery(new Term("content", query.toLowerCase())), BooleanClause.Occur.SHOULD);
                q = queryBuilder.build();
            }
            
            // 创建高亮器
            QueryScorer scorer = new QueryScorer(q);
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, FRAGMENT_SIZE);
            org.apache.lucene.search.highlight.Formatter formatter = new SimpleHTMLFormatter("<em>", "</em>");
            Highlighter highlighter = new Highlighter(formatter, scorer);
            highlighter.setTextFragmenter(fragmenter);
            
            // 执行搜索
            TopDocs docs = searcher.search(q, pageable.getPageNumber() * pageable.getPageSize());
            ScoreDoc[] hits = docs.scoreDocs;
            
            // 收集结果
            List<Map<String, Object>> results = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), hits.length);
            
            // 批量处理
            List<Long> docIds = new ArrayList<>();
            Map<Integer, org.apache.lucene.document.Document> luceneDocs = new HashMap<>();
            
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                docIds.add(Long.parseLong(doc.get("id")));
                luceneDocs.put(i, doc);
            }
            
            // 批量查询数据库
            Map<Long, Document> docMap = documentRepository.findAllById(docIds)
                    .stream()
                    .collect(Collectors.toMap(Document::getId, doc -> doc));
            
            // 处理结果
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document luceneDoc = luceneDocs.get(i);
                Long docId = Long.parseLong(luceneDoc.get("id"));
                Document document = docMap.get(docId);
                
                if (document != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("document", document);
                    
                    // 添加高亮片段
                    Map<String, String[]> highlights = new HashMap<>();
                    String[] fields = field != null ? new String[]{field} : HIGHLIGHT_FIELDS;
                    
                    for (String f : fields) {
                        try {
                            String text = luceneDoc.get(f);
                            if (text != null) {
                                String[] fragments = highlighter.getBestFragments(analyzer, f, text, MAX_NUM_FRAGMENTS);
                                if (fragments.length > 0) {
                                    highlights.put(f, fragments);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("高亮处理失败: {}", f, e);
                        }
                    }
                    
                    if (!highlights.isEmpty()) {
                        result.put("highlights", highlights);
                    }
                    
                    results.add(result);
                }
            }
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_SUGGESTION_CACHE, key = "#prefix + '-' + #limit")
    public List<String> getSuggestions(String prefix, int limit) {
        Set<String> suggestions = new HashSet<>();
        
        // 从缓存中获取建议
        for (String field : HIGHLIGHT_FIELDS) {
            Set<String> fieldTerms = termCache.get(field);
            if (fieldTerms != null) {
                suggestions.addAll(fieldTerms.stream()
                        .filter(term -> term.toLowerCase().startsWith(prefix.toLowerCase()))
                        .limit(limit)
                        .collect(Collectors.toSet()));
            }
        }
        
        return suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_SUGGESTION_CACHE, key = "'title-' + #prefix + '-' + #limit")
    public List<String> getTitleSuggestions(String prefix, int limit) {
        Set<String> fieldTerms = termCache.get("title");
        if (fieldTerms == null) {
            return Collections.emptyList();
        }
        
        return fieldTerms.stream()
                .filter(term -> term.toLowerCase().startsWith(prefix.toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_SUGGESTION_CACHE, key = "'content-' + #prefix + '-' + #limit")
    public List<String> getContentSuggestions(String prefix, int limit) {
        Set<String> fieldTerms = termCache.get("content");
        if (fieldTerms == null) {
            return Collections.emptyList();
        }
        
        return fieldTerms.stream()
                .filter(term -> term.toLowerCase().startsWith(prefix.toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_RESULT_CACHE, key = "'vector-' + #query + '-' + #limit + '-' + #minScore")
    public List<Map<String, Object>> vectorSearch(String query, int limit, float minScore) {
        try {
            // 将查询文本转换为向量
            float[] queryVector = vectorizationService.vectorize(query);
            
            // 执行向量搜索
            List<Map.Entry<String, Float>> searchResults = vectorStorageService.search(queryVector, limit, minScore);
            
            // 获取文档详情
            List<Long> docIds = searchResults.stream()
                    .map(result -> Long.parseLong(result.getKey()))
                    .collect(Collectors.toList());
            
            Map<Long, Document> docMap = documentRepository.findAllById(docIds)
                    .stream()
                    .collect(Collectors.toMap(Document::getId, doc -> doc));
            
            // 组装结果
            return searchResults.stream()
                    .map(result -> {
                        Map<String, Object> resultMap = new HashMap<>();
                        Long docId = Long.parseLong(result.getKey());
                        Document doc = docMap.get(docId);
                        if (doc != null) {
                            resultMap.put("documentId", docId);
                            resultMap.put("name", doc.getName());
                            resultMap.put("originalName", doc.getOriginalName());
                            resultMap.put("score", result.getValue());
                        }
                        return resultMap;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_RESULT_CACHE, key = "'hybrid-' + #query + '-' + #limit + '-' + #minScore")
    public List<Map<String, Object>> hybridSearch(String query, int limit, float minScore) {
        try {
            // 执行关键词搜索
            List<Map<String, Object>> keywordResults = doSearchWithHighlight(query, Pageable.ofSize(limit), null)
                    .getContent();
            
            // 执行向量搜索
            List<Map<String, Object>> vectorResults = vectorSearch(query, limit, minScore);
            
            // 合并结果
            Map<Long, Map<String, Object>> resultMap = new HashMap<>();
            
            // 处理关键词搜索结果
            keywordResults.forEach(result -> {
                Long docId = Long.parseLong(result.get("id").toString());
                result.put("keywordScore", result.get("score"));
                resultMap.put(docId, result);
            });
            
            // 处理向量搜索结果
            vectorResults.forEach(result -> {
                Long docId = Long.parseLong(result.get("documentId").toString());
                Map<String, Object> existingResult = resultMap.get(docId);
                if (existingResult != null) {
                    // 合并分数
                    double keywordScore = (double) existingResult.get("keywordScore");
                    double vectorScore = (double) result.get("score");
                    existingResult.put("score", (keywordScore + vectorScore) / 2);
                } else {
                    result.put("keywordScore", 0.0);
                    resultMap.put(docId, result);
                }
            });
            
            // 按分数排序
            return resultMap.values().stream()
                    .sorted((a, b) -> Double.compare(
                            (double) b.get("score"),
                            (double) a.get("score")))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("混合搜索失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_SUGGESTION_CACHE, key = "'personalized-' + #userId + '-' + #prefix + '-' + #limit")
    public List<String> getPersonalizedSuggestions(String userId, String prefix, int limit) {
        // 从用户历史搜索记录中获取建议
        Set<String> suggestions = new HashSet<>();
        for (String field : HIGHLIGHT_FIELDS) {
            Set<String> fieldTerms = termCache.get(field);
            if (fieldTerms != null) {
                suggestions.addAll(fieldTerms.stream()
                        .filter(term -> term.toLowerCase().startsWith(prefix.toLowerCase()))
                        .limit(limit)
                        .collect(Collectors.toSet()));
            }
        }
        return suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = SearchCacheConfig.SEARCH_SUGGESTION_CACHE, allEntries = true)
    public void recordSuggestionUsage(String suggestion, String userId) {
        // 记录搜索建议使用情况
        log.debug("用户 {} 使用了搜索建议: {}", userId, suggestion);
    }

    @Override
    @Cacheable(value = SearchCacheConfig.SEARCH_SUGGESTION_CACHE, key = "'hot-' + #limit")
    public List<String> getHotSuggestions(int limit) {
        // 获取热门搜索建议
        Set<String> suggestions = new HashSet<>();
        for (String field : HIGHLIGHT_FIELDS) {
            Set<String> fieldTerms = termCache.get(field);
            if (fieldTerms != null) {
                suggestions.addAll(fieldTerms);
            }
        }
        return suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {
            SearchCacheConfig.SEARCH_RESULT_CACHE,
            SearchCacheConfig.SEARCH_SUGGESTION_CACHE,
            SearchCacheConfig.SEARCH_HIGHLIGHT_CACHE
    }, allEntries = true)
    public void clearCache() {
        log.debug("清除搜索缓存");
    }
} 