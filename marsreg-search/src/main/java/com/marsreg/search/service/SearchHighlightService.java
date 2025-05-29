package com.marsreg.search.service;

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
} 