package com.marsreg.search.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsreg.search.model.SynonymGroup;
import com.marsreg.search.service.SynonymService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynonymServiceImpl implements SynonymService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String SYNONYM_GROUP_KEY = "search:synonym:group:";
    private static final String SYNONYM_TERM_KEY = "search:synonym:term:";
    private static final String SYNONYM_CATEGORY_KEY = "search:synonym:category:";
    
    // 内存缓存
    private final Map<String, List<String>> synonymCache = new ConcurrentHashMap<>();
    private final Map<String, SynonymGroup> groupCache = new ConcurrentHashMap<>();
    
    private static final int BATCH_SIZE = 100;
    private static final int CACHE_WARMUP_SIZE = 1000;
    
    @PostConstruct
    public void init() {
        warmupCache();
    }
    
    private void warmupCache() {
        log.info("开始预热同义词缓存...");
        try {
            List<SynonymGroup> groups = getAllSynonymGroups();
            int count = 0;
            
            for (SynonymGroup group : groups) {
                if (count >= CACHE_WARMUP_SIZE) {
                    break;
                }
                
                groupCache.put(group.getId(), group);
                for (String term : group.getTerms()) {
                    synonymCache.put(term, new ArrayList<>(group.getTerms()));
                }
                count++;
            }
            
            log.info("同义词缓存预热完成，共加载 {} 个同义词组", count);
        } catch (Exception e) {
            log.error("预热同义词缓存失败", e);
        }
    }
    
    @Override
    public List<String> getSynonyms(String term) {
        if (term == null || term.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 先从缓存获取
        List<String> cachedSynonyms = synonymCache.get(term);
        if (cachedSynonyms != null) {
            return cachedSynonyms;
        }
        
        // 从Redis获取
        String key = SYNONYM_TERM_KEY + term.toLowerCase();
        Set<String> synonyms = redisTemplate.opsForSet().members(key);
        
        if (synonyms == null) {
            return Collections.emptyList();
        }
        
        List<String> result = new ArrayList<>(synonyms);
        // 更新缓存
        synonymCache.put(term, result);
        
        return result;
    }
    
    @Override
    public Map<String, List<String>> getSynonymsForTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return terms.stream()
            .collect(Collectors.toMap(
                term -> term,
                this::getSynonyms
            ));
    }
    
    @Override
    public String addSynonymGroup(SynonymGroup group) {
        if (group == null || group.getTerms() == null || group.getTerms().isEmpty()) {
            throw new IllegalArgumentException("同义词组不能为空");
        }
        
        String groupId = UUID.randomUUID().toString();
        group.setId(groupId);
        
        // 保存同义词组
        redisTemplate.opsForValue().set(SYNONYM_GROUP_KEY + groupId, group.toString());
        
        // 更新词到同义词的映射
        for (String term : group.getTerms()) {
            String termKey = SYNONYM_TERM_KEY + term.toLowerCase();
            redisTemplate.opsForSet().add(termKey, group.getTerms().toArray(new String[0]));
        }
        
        // 更新分类索引
        if (group.getCategory() != null) {
            redisTemplate.opsForSet().add(SYNONYM_CATEGORY_KEY + group.getCategory(), groupId);
        }
        
        // 更新缓存
        groupCache.put(groupId, group);
        for (String term : group.getTerms()) {
            synonymCache.put(term, new ArrayList<>(group.getTerms()));
        }
        
        return groupId;
    }
    
    @Override
    public void updateSynonymGroup(SynonymGroup group) {
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("同义词组ID不能为空");
        }
        
        // 获取旧的同义词组
        SynonymGroup oldGroup = getSynonymGroup(group.getId());
        if (oldGroup == null) {
            throw new IllegalArgumentException("同义词组不存在");
        }
        
        // 删除旧的映射
        for (String term : oldGroup.getTerms()) {
            String termKey = SYNONYM_TERM_KEY + term.toLowerCase();
            redisTemplate.opsForSet().remove(termKey, oldGroup.getTerms().toArray(new String[0]));
        }
        
        // 更新同义词组
        redisTemplate.opsForValue().set(SYNONYM_GROUP_KEY + group.getId(), group.toString());
        
        // 更新新的映射
        for (String term : group.getTerms()) {
            String termKey = SYNONYM_TERM_KEY + term.toLowerCase();
            redisTemplate.opsForSet().add(termKey, group.getTerms().toArray(new String[0]));
        }
        
        // 更新分类索引
        if (!Objects.equals(oldGroup.getCategory(), group.getCategory())) {
            if (oldGroup.getCategory() != null) {
                redisTemplate.opsForSet().remove(SYNONYM_CATEGORY_KEY + oldGroup.getCategory(), group.getId());
            }
            if (group.getCategory() != null) {
                redisTemplate.opsForSet().add(SYNONYM_CATEGORY_KEY + group.getCategory(), group.getId());
            }
        }
        
        // 更新缓存
        groupCache.put(group.getId(), group);
        for (String term : group.getTerms()) {
            synonymCache.put(term, new ArrayList<>(group.getTerms()));
        }
    }
    
    @Override
    public void deleteSynonymGroup(String groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("同义词组ID不能为空");
        }
        
        SynonymGroup group = getSynonymGroup(groupId);
        if (group == null) {
            return;
        }
        
        // 删除词到同义词的映射
        for (String term : group.getTerms()) {
            String termKey = SYNONYM_TERM_KEY + term.toLowerCase();
            redisTemplate.opsForSet().remove(termKey, group.getTerms().toArray(new String[0]));
        }
        
        // 删除分类索引
        if (group.getCategory() != null) {
            redisTemplate.opsForSet().remove(SYNONYM_CATEGORY_KEY + group.getCategory(), groupId);
        }
        
        // 删除同义词组
        redisTemplate.delete(SYNONYM_GROUP_KEY + groupId);
        
        // 更新缓存
        groupCache.remove(groupId);
        for (String term : group.getTerms()) {
            synonymCache.remove(term);
        }
    }
    
    @Override
    public SynonymGroup getSynonymGroup(String groupId) {
        if (groupId == null) {
            return null;
        }
        
        // 先从缓存获取
        SynonymGroup cachedGroup = groupCache.get(groupId);
        if (cachedGroup != null) {
            return cachedGroup;
        }
        
        // 从Redis获取
        String groupStr = redisTemplate.opsForValue().get(SYNONYM_GROUP_KEY + groupId);
        if (groupStr == null) {
            return null;
        }
        
        // 解析同义词组
        SynonymGroup group = parseSynonymGroup(groupStr);
        if (group != null) {
            groupCache.put(groupId, group);
        }
        
        return group;
    }
    
    @Override
    public List<SynonymGroup> getAllSynonymGroups() {
        Set<String> groupIds = redisTemplate.keys(SYNONYM_GROUP_KEY + "*");
        if (groupIds == null) {
            return Collections.emptyList();
        }
        
        return groupIds.stream()
            .map(key -> key.substring(SYNONYM_GROUP_KEY.length()))
            .<SynonymGroup>map(this::getSynonymGroup)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<SynonymGroup> getSynonymGroupsByCategory(String category) {
        if (category == null) {
            return Collections.emptyList();
        }
        
        Set<String> groupIds = redisTemplate.opsForSet().members(SYNONYM_CATEGORY_KEY + category);
        if (groupIds == null) {
            return Collections.emptyList();
        }
        
        return groupIds.stream()
            .<SynonymGroup>map(this::getSynonymGroup)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    public void reloadSynonyms() {
        // 清空缓存
        synonymCache.clear();
        groupCache.clear();
        
        // 重新加载所有同义词组
        List<SynonymGroup> groups = getAllSynonymGroups();
        for (SynonymGroup group : groups) {
            groupCache.put(group.getId(), group);
            for (String term : group.getTerms()) {
                synonymCache.put(term, new ArrayList<>(group.getTerms()));
            }
        }
    }
    
    @Override
    public void addSynonymGroups(List<SynonymGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        
        // 分批处理
        for (int i = 0; i < groups.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, groups.size());
            List<SynonymGroup> batch = groups.subList(i, end);
            
            // 批量保存同义词组
            Map<String, String> groupMap = batch.stream()
                .collect(Collectors.toMap(
                    group -> SYNONYM_GROUP_KEY + group.getId(),
                    group -> {
                        String groupId = UUID.randomUUID().toString();
                        group.setId(groupId);
                        return group.toString();
                    }
                ));
            redisTemplate.opsForValue().multiSet(groupMap);
            
            // 批量更新词到同义词的映射
            for (SynonymGroup group : batch) {
                for (String term : group.getTerms()) {
                    String termKey = SYNONYM_TERM_KEY + term.toLowerCase();
                    redisTemplate.opsForSet().add(termKey, group.getTerms().toArray(new String[0]));
                }
                
                // 更新分类索引
                if (group.getCategory() != null) {
                    redisTemplate.opsForSet().add(SYNONYM_CATEGORY_KEY + group.getCategory(), group.getId());
                }
                
                // 更新缓存
                groupCache.put(group.getId(), group);
                for (String term : group.getTerms()) {
                    synonymCache.put(term, new ArrayList<>(group.getTerms()));
                }
            }
        }
    }
    
    @Override
    public void deleteSynonymGroups(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        
        // 分批处理
        for (int i = 0; i < groupIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, groupIds.size());
            List<String> batch = groupIds.subList(i, end);
            
            // 批量获取同义词组
            List<SynonymGroup> groups = batch.stream()
                .<SynonymGroup>map(this::getSynonymGroup)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            // 批量删除词到同义词的映射
            for (SynonymGroup group : groups) {
                for (String term : group.getTerms()) {
                    String termKey = SYNONYM_TERM_KEY + term.toLowerCase();
                    redisTemplate.opsForSet().remove(termKey, group.getTerms().toArray(new String[0]));
                }
                
                // 删除分类索引
                if (group.getCategory() != null) {
                    redisTemplate.opsForSet().remove(SYNONYM_CATEGORY_KEY + group.getCategory(), group.getId());
                }
                
                // 删除同义词组
                redisTemplate.delete(SYNONYM_GROUP_KEY + group.getId());
                
                // 更新缓存
                groupCache.remove(group.getId());
                for (String term : group.getTerms()) {
                    synonymCache.remove(term);
                }
            }
        }
    }
    
    private SynonymGroup parseSynonymGroup(String groupStr) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(groupStr, SynonymGroup.class);
        } catch (Exception e) {
            log.error("Failed to parse synonym group: {}", groupStr, e);
            return null;
        }
    }
} 