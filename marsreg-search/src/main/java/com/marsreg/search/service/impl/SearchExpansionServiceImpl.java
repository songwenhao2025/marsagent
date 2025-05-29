package com.marsreg.search.service.impl;

import com.marsreg.search.service.SearchExpansionService;
import com.marsreg.search.service.SynonymService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchExpansionServiceImpl implements SearchExpansionService {

    private final SynonymService synonymService;
    
    // 缓存扩展结果
    private final Map<String, List<String>> queryExpansionCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> termExpansionCache = new ConcurrentHashMap<>();
    private final Map<String, Double> weightCache = new ConcurrentHashMap<>();
    
    @Override
    public List<String> expandQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 先从缓存获取
        List<String> cachedExpansions = queryExpansionCache.get(query);
        if (cachedExpansions != null) {
            return cachedExpansions;
        }
        
        // 分词
        List<String> terms = Arrays.asList(query.toLowerCase().split("\\s+"));
        
        // 获取每个词的同义词
        Map<String, List<String>> termSynonyms = synonymService.getSynonymsForTerms(terms);
        
        // 生成扩展查询
        List<String> expansions = generateExpansions(terms, termSynonyms);
        
        // 更新缓存
        queryExpansionCache.put(query, expansions);
        
        return expansions;
    }
    
    @Override
    public Map<String, List<String>> expandQueries(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return queries.stream()
            .collect(Collectors.toMap(
                query -> query,
                this::expandQuery
            ));
    }
    
    @Override
    public List<String> expandTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 先从缓存获取
        List<String> cachedExpansions = termExpansionCache.get(term);
        if (cachedExpansions != null) {
            return cachedExpansions;
        }
        
        // 获取同义词
        List<String> synonyms = synonymService.getSynonyms(term.toLowerCase());
        
        // 更新缓存
        termExpansionCache.put(term, synonyms);
        
        return synonyms;
    }
    
    @Override
    public Map<String, List<String>> expandTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return terms.stream()
            .collect(Collectors.toMap(
                term -> term,
                this::expandTerm
            ));
    }
    
    @Override
    public double getQueryWeight(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 0.0;
        }
        
        // 先从缓存获取
        Double cachedWeight = weightCache.get(query);
        if (cachedWeight != null) {
            return cachedWeight;
        }
        
        // 计算权重
        double weight = calculateQueryWeight(query);
        
        // 更新缓存
        weightCache.put(query, weight);
        
        return weight;
    }
    
    @Override
    public double getTermWeight(String term) {
        if (term == null || term.trim().isEmpty()) {
            return 0.0;
        }
        
        // 先从缓存获取
        Double cachedWeight = weightCache.get(term);
        if (cachedWeight != null) {
            return cachedWeight;
        }
        
        // 计算权重
        double weight = calculateTermWeight(term);
        
        // 更新缓存
        weightCache.put(term, weight);
        
        return weight;
    }
    
    private List<String> generateExpansions(List<String> terms, Map<String, List<String>> termSynonyms) {
        List<List<String>> termExpansions = new ArrayList<>();
        
        // 获取每个词的扩展
        for (String term : terms) {
            List<String> synonyms = termSynonyms.getOrDefault(term, Collections.emptyList());
            List<String> expansions = new ArrayList<>();
            expansions.add(term);
            expansions.addAll(synonyms);
            termExpansions.add(expansions);
        }
        
        // 生成所有可能的组合
        return generateCombinations(termExpansions);
    }
    
    private List<String> generateCombinations(List<List<String>> termExpansions) {
        if (termExpansions.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> result = new ArrayList<>();
        generateCombinationsHelper(termExpansions, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void generateCombinationsHelper(List<List<String>> termExpansions, int index, 
                                          List<String> current, List<String> result) {
        if (index == termExpansions.size()) {
            result.add(String.join(" ", current));
            return;
        }
        
        for (String term : termExpansions.get(index)) {
            current.add(term);
            generateCombinationsHelper(termExpansions, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    private double calculateQueryWeight(String query) {
        // 基于查询长度、词频等因素计算权重
        double lengthWeight = Math.min(query.length() / 10.0, 1.0);
        double termWeight = Arrays.stream(query.split("\\s+"))
            .mapToDouble(this::getTermWeight)
            .average()
            .orElse(0.0);
        
        return (lengthWeight + termWeight) / 2.0;
    }
    
    private double calculateTermWeight(String term) {
        // 基于词长度、同义词数量等因素计算权重
        double lengthWeight = Math.min(term.length() / 5.0, 1.0);
        double synonymWeight = Math.min(expandTerm(term).size() / 5.0, 1.0);
        
        return (lengthWeight + synonymWeight) / 2.0;
    }
} 