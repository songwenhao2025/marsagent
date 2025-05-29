package com.marsreg.search.service;

import java.util.List;
import java.util.Map;

public interface SearchExpansionService {
    /**
     * 扩展搜索查询
     * @param query 原始查询
     * @return 扩展后的查询列表
     */
    List<String> expandQuery(String query);
    
    /**
     * 批量扩展搜索查询
     * @param queries 原始查询列表
     * @return 查询到扩展查询的映射
     */
    Map<String, List<String>> expandQueries(List<String> queries);
    
    /**
     * 扩展搜索词
     * @param term 原始搜索词
     * @return 扩展后的搜索词列表
     */
    List<String> expandTerm(String term);
    
    /**
     * 批量扩展搜索词
     * @param terms 原始搜索词列表
     * @return 词到扩展词的映射
     */
    Map<String, List<String>> expandTerms(List<String> terms);
    
    /**
     * 获取查询的权重
     * @param query 查询
     * @return 权重
     */
    double getQueryWeight(String query);
    
    /**
     * 获取词的权重
     * @param term 词
     * @return 权重
     */
    double getTermWeight(String term);
} 