package com.marsreg.document.service.impl;

import com.marsreg.document.config.SearchCacheConfig;
import com.marsreg.document.entity.Document;
import com.marsreg.document.repository.DocumentRepository;
import com.marsreg.document.service.DocumentSearchService;
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
    private Directory directory;
    private Analyzer analyzer;
    private static final String[] HIGHLIGHT_FIELDS = {"title", "content"};
    private static final int FRAGMENT_SIZE = 150;
    private static final int MAX_NUM_FRAGMENTS = 3;
    private static final int BATCH_SIZE = 100;
    private static final Map<String, Set<String>> termCache = new ConcurrentHashMap<>();

    public DocumentSearchServiceImpl(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
        try {
            this.directory = FSDirectory.open(Paths.get("index"));
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
            
            // 执行搜索
            TopDocs docs = searcher.search(q, pageable.getPageNumber() * pageable.getPageSize());
            ScoreDoc[] hits = docs.scoreDocs;
            
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
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
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

    @CacheEvict(value = {
            SearchCacheConfig.SEARCH_RESULT_CACHE,
            SearchCacheConfig.SEARCH_SUGGESTION_CACHE,
            SearchCacheConfig.SEARCH_HIGHLIGHT_CACHE
    }, allEntries = true)
    public void clearCache() {
        log.debug("清除搜索缓存");
    }
} 