package com.marsreg.search.service;

import com.marsreg.search.model.SynonymGroup;
import java.util.List;
import java.util.Map;

public interface SynonymService {
    /**
     * 获取词的同义词
     * @param term 词
     * @return 同义词列表
     */
    List<String> getSynonyms(String term);
    
    /**
     * 获取多个词的同义词
     * @param terms 词列表
     * @return 词到同义词的映射
     */
    Map<String, List<String>> getSynonymsForTerms(List<String> terms);
    
    /**
     * 添加同义词组
     * @param group 同义词组
     * @return 同义词组ID
     */
    String addSynonymGroup(SynonymGroup group);
    
    /**
     * 更新同义词组
     * @param group 同义词组
     */
    void updateSynonymGroup(SynonymGroup group);
    
    /**
     * 删除同义词组
     * @param groupId 同义词组ID
     */
    void deleteSynonymGroup(String groupId);
    
    /**
     * 获取同义词组
     * @param groupId 同义词组ID
     * @return 同义词组
     */
    SynonymGroup getSynonymGroup(String groupId);
    
    /**
     * 获取所有同义词组
     * @return 同义词组列表
     */
    List<SynonymGroup> getAllSynonymGroups();
    
    /**
     * 按分类获取同义词组
     * @param category 分类
     * @return 同义词组列表
     */
    List<SynonymGroup> getSynonymGroupsByCategory(String category);
    
    /**
     * 重新加载同义词配置
     */
    void reloadSynonyms();
    
    /**
     * 批量添加同义词组
     * @param groups 同义词组列表
     */
    void addSynonymGroups(List<SynonymGroup> groups);
    
    /**
     * 批量删除同义词组
     * @param groupIds 同义词组ID列表
     */
    void deleteSynonymGroups(List<String> groupIds);
} 