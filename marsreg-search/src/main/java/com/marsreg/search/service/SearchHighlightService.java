package com.marsreg.search.service;

import com.marsreg.search.model.SearchResult;
import java.util.List;
import java.util.Map;

public interface SearchHighlightService {
    /**
     * 为搜索结果添加高亮
     * @param content 原始内容
     * @param highlights 高亮信息
     * @return 添加高亮后的内容
     */
    String addHighlights(String content, Map<String, String[]> highlights);
    
    /**
     * 提取高亮片段
     * @param content 原始内容
     * @param highlights 高亮信息
     * @return 高亮片段列表
     */
    List<String> extractHighlightFragments(String content, Map<String, String[]> highlights);
    
    /**
     * 处理搜索结果的高亮和摘要
     * @param results 搜索结果列表
     * @param query 搜索查询
     * @return 处理后的搜索结果列表
     */
    List<SearchResult> processHighlights(List<SearchResult> results, String query);
    
    /**
     * 生成摘要
     * @param content 原始内容
     * @param query 搜索查询
     * @param maxLength 最大长度
     * @return 生成的摘要
     */
    String generateSummary(String content, String query, int maxLength);
} 