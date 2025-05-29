package com.marsreg.search.service;

import com.marsreg.search.model.SearchSuggestion;
import java.util.List;
import java.util.Map;

public interface SearchSuggestionService {
    /**
     * 获取搜索建议
     *
     * @param prefix 搜索前缀
     * @param size 建议数量
     * @return 建议列表
     */
    List<SearchSuggestion> getSuggestions(String prefix, int size);
    
    /**
     * 获取热门搜索建议
     *
     * @param size 建议数量
     * @return 建议列表
     */
    List<SearchSuggestion> getHotSuggestions(int size);
    
    /**
     * 获取个性化搜索建议
     *
     * @param userId 用户ID
     * @param prefix 搜索前缀
     * @param size 建议数量
     * @return 建议列表
     */
    List<SearchSuggestion> getPersonalizedSuggestions(String userId, String prefix, int size);
    
    /**
     * 记录搜索建议的使用情况
     *
     * @param suggestion 被使用的建议
     * @param userId 用户ID
     */
    void recordSuggestionUsage(SearchSuggestion suggestion, String userId);
    
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