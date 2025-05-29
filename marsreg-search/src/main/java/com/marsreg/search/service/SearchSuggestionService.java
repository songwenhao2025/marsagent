package com.marsreg.search.service;

import com.marsreg.search.model.SearchSuggestion;
import java.util.List;

public interface SearchSuggestionService {
    /**
     * 获取搜索建议
     *
     * @param prefix 搜索前缀
     * @param size 返回结果数量
     * @return 搜索建议列表
     */
    List<SearchSuggestion> getSuggestions(String prefix, int size);
    
    /**
     * 获取热门搜索建议
     *
     * @param size 返回结果数量
     * @return 热门搜索建议列表
     */
    List<SearchSuggestion> getHotSuggestions(int size);
    
    /**
     * 记录搜索关键词
     *
     * @param keyword 搜索关键词
     */
    void recordSearchKeyword(String keyword);
} 