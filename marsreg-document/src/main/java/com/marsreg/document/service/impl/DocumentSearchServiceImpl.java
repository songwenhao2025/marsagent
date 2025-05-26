package com.marsreg.document.service.impl;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
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

    public DocumentSearchServiceImpl(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
        try {
            this.directory = FSDirectory.open(Paths.get("index"));
            this.analyzer = new StandardAnalyzer();
        } catch (IOException e) {
            log.error("初始化搜索服务失败", e);
        }
    }

    @Override
    public Page<Map<String, Object>> searchWithHighlight(String query, Pageable pageable) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建多字段查询
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            queryBuilder.add(new FuzzyQuery(new Term("title", query.toLowerCase())), BooleanClause.Occur.SHOULD);
            queryBuilder.add(new FuzzyQuery(new Term("content", query.toLowerCase())), BooleanClause.Occur.SHOULD);
            
            // 创建高亮器
            QueryScorer scorer = new QueryScorer(queryBuilder.build());
            Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, FRAGMENT_SIZE);
            org.apache.lucene.search.highlight.Formatter formatter = new SimpleHTMLFormatter("<em>", "</em>");
            Highlighter highlighter = new Highlighter(formatter, scorer);
            highlighter.setTextFragmenter(fragmenter);
            
            // 执行搜索
            TopDocs docs = searcher.search(queryBuilder.build(), pageable.getPageNumber() * pageable.getPageSize());
            ScoreDoc[] hits = docs.scoreDocs;
            
            // 收集结果
            List<Map<String, Object>> results = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), hits.length);
            
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                documentRepository.findById(Long.parseLong(doc.get("id")))
                        .ifPresent(document -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("document", document);
                            
                            // 添加高亮片段
                            Map<String, String[]> highlights = new HashMap<>();
                            for (String field : HIGHLIGHT_FIELDS) {
                                try {
                                    String text = doc.get(field);
                                    if (text != null) {
                                        String[] fragments = highlighter.getBestFragments(analyzer, field, text, MAX_NUM_FRAGMENTS);
                                        if (fragments.length > 0) {
                                            highlights.put(field, fragments);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("高亮处理失败: {}", field, e);
                                }
                            }
                            result.put("highlights", highlights);
                            
                            results.add(result);
                        });
            }
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<Map<String, Object>> searchContentWithHighlight(String query, Pageable pageable) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建内容查询
            Query q = new FuzzyQuery(new Term("content", query.toLowerCase()));
            
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
            
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                documentRepository.findById(Long.parseLong(doc.get("id")))
                        .ifPresent(document -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("document", document);
                            
                            // 添加高亮片段
                            try {
                                String text = doc.get("content");
                                if (text != null) {
                                    String[] fragments = highlighter.getBestFragments(analyzer, "content", text, MAX_NUM_FRAGMENTS);
                                    if (fragments.length > 0) {
                                        Map<String, String[]> highlights = new HashMap<>();
                                        highlights.put("content", fragments);
                                        result.put("highlights", highlights);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("高亮处理失败", e);
                            }
                            
                            results.add(result);
                        });
            }
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索内容失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<Map<String, Object>> searchTitleWithHighlight(String query, Pageable pageable) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建标题查询
            Query q = new FuzzyQuery(new Term("title", query.toLowerCase()));
            
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
            
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                documentRepository.findById(Long.parseLong(doc.get("id")))
                        .ifPresent(document -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("document", document);
                            
                            // 添加高亮片段
                            try {
                                String text = doc.get("title");
                                if (text != null) {
                                    String[] fragments = highlighter.getBestFragments(analyzer, "title", text, MAX_NUM_FRAGMENTS);
                                    if (fragments.length > 0) {
                                        Map<String, String[]> highlights = new HashMap<>();
                                        highlights.put("title", fragments);
                                        result.put("highlights", highlights);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("高亮处理失败", e);
                            }
                            
                            results.add(result);
                        });
            }
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索标题失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<Document> search(String query, Pageable pageable) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建多字段查询
            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            queryBuilder.add(new FuzzyQuery(new Term("title", query.toLowerCase())), BooleanClause.Occur.SHOULD);
            queryBuilder.add(new FuzzyQuery(new Term("content", query.toLowerCase())), BooleanClause.Occur.SHOULD);
            
            // 执行搜索
            TopDocs docs = searcher.search(queryBuilder.build(), pageable.getPageNumber() * pageable.getPageSize());
            ScoreDoc[] hits = docs.scoreDocs;
            
            // 收集结果
            List<Document> results = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), hits.length);
            
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                documentRepository.findById(Long.parseLong(doc.get("id")))
                        .ifPresent(results::add);
            }
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<Document> searchContent(String query, Pageable pageable) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建内容查询
            Query q = new FuzzyQuery(new Term("content", query.toLowerCase()));
            
            // 执行搜索
            TopDocs docs = searcher.search(q, pageable.getPageNumber() * pageable.getPageSize());
            ScoreDoc[] hits = docs.scoreDocs;
            
            // 收集结果
            List<Document> results = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), hits.length);
            
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                documentRepository.findById(Long.parseLong(doc.get("id")))
                        .ifPresent(results::add);
            }
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索内容失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<Document> searchTitle(String query, Pageable pageable) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // 创建标题查询
            Query q = new FuzzyQuery(new Term("title", query.toLowerCase()));
            
            // 执行搜索
            TopDocs docs = searcher.search(q, pageable.getPageNumber() * pageable.getPageSize());
            ScoreDoc[] hits = docs.scoreDocs;
            
            // 收集结果
            List<Document> results = new ArrayList<>();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), hits.length);
            
            for (int i = start; i < end; i++) {
                org.apache.lucene.document.Document doc = searcher.doc(hits[i].doc);
                documentRepository.findById(Long.parseLong(doc.get("id")))
                        .ifPresent(results::add);
            }
            
            return new PageImpl<>(results, pageable, docs.totalHits.value);
        } catch (IOException e) {
            log.error("搜索标题失败", e);
            return Page.empty(pageable);
        }
    }

    @Override
    public List<String> getSuggestions(String prefix, int limit) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            Set<String> suggestions = new HashSet<>();
            
            // 从标题字段获取建议
            suggestions.addAll(getFieldSuggestions(reader, "title", prefix, limit));
            
            // 从内容字段获取建议
            suggestions.addAll(getFieldSuggestions(reader, "content", prefix, limit));
            
            return suggestions.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("获取搜索建议失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getTitleSuggestions(String prefix, int limit) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            return getFieldSuggestions(reader, "title", prefix, limit);
        } catch (IOException e) {
            log.error("获取标题搜索建议失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getContentSuggestions(String prefix, int limit) {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            return getFieldSuggestions(reader, "content", prefix, limit);
        } catch (IOException e) {
            log.error("获取内容搜索建议失败", e);
            return Collections.emptyList();
        }
    }

    private List<String> getFieldSuggestions(IndexReader reader, String field, String prefix, int limit) throws IOException {
        Set<String> suggestions = new HashSet<>();
        org.apache.lucene.index.Terms terms = MultiTerms.getTerms(reader, field);
        if (terms == null) {
            return Collections.emptyList();
        }

        org.apache.lucene.index.TermsEnum termsEnum = terms.iterator();
        BytesRef prefixBytes = new BytesRef(prefix.toLowerCase());
        BytesRef term;
        
        // 查找以prefix开头的所有词条
        while ((term = termsEnum.next()) != null) {
            if (term.bytesEquals(prefixBytes) || term.utf8ToString().startsWith(prefix.toLowerCase())) {
                suggestions.add(term.utf8ToString());
                if (suggestions.size() >= limit) {
                    break;
                }
            }
        }

        return suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
} 