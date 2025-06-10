package com.marsreg.search.service;

import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResponse;
import java.util.Map;

public interface SearchService {
    /**
     * 执行搜索
     *
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchResponse search(SearchRequest request);

    /**
     * 执行关键词搜索
     *
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchResponse keywordSearch(SearchRequest request);

    /**
     * 执行向量搜索
     *
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchResponse vectorSearch(SearchRequest request);

    /**
     * 执行混合搜索
     *
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchResponse hybridSearch(SearchRequest request);

    /**
     * 获取搜索统计信息
     *
     * @return 搜索统计信息
     */
    Map<String, Object> getSearchStats();
} 