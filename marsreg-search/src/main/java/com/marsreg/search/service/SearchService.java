package com.marsreg.search.service;

import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResult;
import java.util.List;

public interface SearchService {
    /**
     * 执行检索
     *
     * @param request 检索请求
     * @return 检索结果列表
     */
    List<SearchResult> search(SearchRequest request);

    /**
     * 执行向量检索
     *
     * @param query 查询文本
     * @param size 返回结果数量
     * @param minSimilarity 最小相似度阈值
     * @return 检索结果列表
     */
    List<SearchResult> vectorSearch(String query, int size, float minSimilarity);

    /**
     * 执行关键词检索
     *
     * @param query 查询文本
     * @param size 返回结果数量
     * @return 检索结果列表
     */
    List<SearchResult> keywordSearch(String query, int size);

    /**
     * 执行混合检索
     *
     * @param query 查询文本
     * @param size 返回结果数量
     * @param minSimilarity 最小相似度阈值
     * @return 检索结果列表
     */
    List<SearchResult> hybridSearch(String query, int size, float minSimilarity);
} 