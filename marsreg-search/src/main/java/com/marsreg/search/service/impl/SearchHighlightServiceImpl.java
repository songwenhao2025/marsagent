package com.marsreg.search.service.impl;

import com.marsreg.search.model.SearchResult;
import com.marsreg.search.service.SearchHighlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHighlightServiceImpl implements SearchHighlightService {

    private final ElasticsearchOperations elasticsearchOperations;
    
    private static final String HIGHLIGHT_PRE_TAG = "<em>";
    private static final String HIGHLIGHT_POST_TAG = "</em>";
    private static final int DEFAULT_SUMMARY_LENGTH = 200;
    private static final int CONTEXT_LENGTH = 50;

    @Override
    public List<SearchResult> processHighlights(List<SearchResult> results, String query) {
        if (results == null || results.isEmpty() || query == null || query.trim().isEmpty()) {
            return results;
        }

        return results.stream()
            .map(result -> {
                // 处理标题高亮
                if (result.getHighlightedTitle() != null) {
                    result.setTitle(result.getHighlightedTitle());
                } else {
                    result.setTitle(highlightText(result.getTitle(), query));
                }

                // 处理内容高亮和摘要
                if (result.getHighlightedContents() != null && !result.getHighlightedContents().isEmpty()) {
                    result.setContent(String.join("...", result.getHighlightedContents()));
                } else {
                    String highlightedContent = highlightText(result.getContent(), query);
                    result.setContent(highlightedContent);
                }

                // 生成摘要
                result.setSummary(generateSummary(result.getContent(), query, DEFAULT_SUMMARY_LENGTH));

                return result;
            })
            .collect(Collectors.toList());
    }

    @Override
    public String generateSummary(String content, String query, int maxLength) {
        if (content == null || content.isEmpty() || query == null || query.trim().isEmpty()) {
            return content != null && content.length() > maxLength ? 
                content.substring(0, maxLength) + "..." : content;
        }

        // 查找关键词位置
        Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        
        if (!matcher.find()) {
            return content.length() > maxLength ? 
                content.substring(0, maxLength) + "..." : content;
        }

        // 获取关键词上下文
        int start = Math.max(0, matcher.start() - CONTEXT_LENGTH);
        int end = Math.min(content.length(), matcher.end() + CONTEXT_LENGTH);
        
        String summary = content.substring(start, end);
        
        // 如果摘要太长，进行截断
        if (summary.length() > maxLength) {
            int mid = maxLength / 2;
            summary = summary.substring(0, mid) + "..." + 
                     summary.substring(summary.length() - mid);
        }
        
        return summary;
    }

    private String highlightText(String text, String query) {
        if (text == null || text.isEmpty() || query == null || query.trim().isEmpty()) {
            return text;
        }

        Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        StringBuffer highlighted = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(highlighted, 
                HIGHLIGHT_PRE_TAG + matcher.group() + HIGHLIGHT_POST_TAG);
        }
        matcher.appendTail(highlighted);

        return highlighted.toString();
    }
} 