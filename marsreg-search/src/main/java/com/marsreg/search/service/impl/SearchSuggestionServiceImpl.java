package com.marsreg.search.service.impl;

import com.marsreg.search.model.SearchSuggestion;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.SearchSuggestionService;
import com.marsreg.search.service.UserBehaviorService;
import com.marsreg.search.service.SynonymService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final DocumentIndexRepository documentIndexRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserBehaviorService userBehaviorService;
    private final SynonymService synonymService;

    private static final String SUGGESTION_KEY = "suggestion:";
    private static final String HOT_SUGGESTION_KEY = "suggestion:hot";
    private static final String USER_SUGGESTION_KEY = "suggestion:user:";
    private static final String QUERY_WEIGHT_KEY = "suggestion:weight:query:";
    private static final String TERM_WEIGHT_KEY = "suggestion:weight:term:";
    private static final String HOT_KEYWORDS_KEY = "search:hot:keywords";
    private static final int MAX_HOT_KEYWORDS = 100;

    @Override
    @Cacheable(value = "searchSuggestions", key = "#prefix + ':' + #size")
    public List<SearchSuggestion> getSuggestions(String prefix, int size) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return getHotSuggestions(size);
        }
        
        // 从 Redis 获取建议
        Set<ZSetOperations.TypedTuple<String>> suggestions = redisTemplate.opsForZSet()
            .reverseRangeByLexWithScores(SUGGESTION_KEY, "[" + prefix, "[" + prefix + "\\xff", 0, size);
            
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }
        
        return suggestions.stream()
            .map(tuple -> SearchSuggestion.builder()
                .text(tuple.getValue())
                .weight(tuple.getScore())
                .type(SearchSuggestion.SuggestionType.KEYWORD)
                .build())
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "hotSuggestions", key = "#size")
    public List<SearchSuggestion> getHotSuggestions(int size) {
        Set<ZSetOperations.TypedTuple<String>> hotSuggestions = redisTemplate.opsForZSet()
            .reverseRangeWithScores(HOT_SUGGESTION_KEY, 0, size - 1);
            
        if (hotSuggestions == null || hotSuggestions.isEmpty()) {
            return Collections.emptyList();
        }
        
        return hotSuggestions.stream()
            .map(tuple -> SearchSuggestion.builder()
                .text(tuple.getValue())
                .weight(tuple.getScore())
                .type(SearchSuggestion.SuggestionType.HOT)
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchSuggestion> getPersonalizedSuggestions(String userId, String prefix, int size) {
        if (userId == null || userId.trim().isEmpty()) {
            return getSuggestions(prefix, size);
        }
        
        // 获取用户个性化建议
        Set<ZSetOperations.TypedTuple<String>> userSuggestions = redisTemplate.opsForZSet()
            .reverseRangeByLexWithScores(USER_SUGGESTION_KEY + userId, "[" + prefix, "[" + prefix + "\\xff", 0, size);
            
        if (userSuggestions == null || userSuggestions.isEmpty()) {
            return getSuggestions(prefix, size);
        }
        
        return userSuggestions.stream()
            .map(tuple -> SearchSuggestion.builder()
                .text(tuple.getValue())
                .weight(tuple.getScore())
                .type(SearchSuggestion.SuggestionType.PERSONALIZED)
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public void recordSuggestionUsage(SearchSuggestion suggestion, String userId) {
        if (suggestion == null || suggestion.getText() == null) {
            return;
        }
        
        // 更新热门建议
        redisTemplate.opsForZSet().incrementScore(HOT_SUGGESTION_KEY, suggestion.getText(), 1.0);
        
        // 更新用户个性化建议
        if (userId != null && !userId.trim().isEmpty()) {
            redisTemplate.opsForZSet().incrementScore(USER_SUGGESTION_KEY + userId, suggestion.getText(), 1.0);
        }
    }

    @Override
    public void recordSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        // 增加关键词的分数
        redisTemplate.opsForZSet().incrementScore(HOT_KEYWORDS_KEY, keyword, 1.0);
        
        // 如果热门关键词数量超过限制，删除分数最低的
        Long size = redisTemplate.opsForZSet().size(HOT_KEYWORDS_KEY);
        if (size != null && size > MAX_HOT_KEYWORDS) {
            redisTemplate.opsForZSet().removeRange(HOT_KEYWORDS_KEY, 0, size - MAX_HOT_KEYWORDS - 1);
        }
    }

    @Override
    public List<String> expandQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 分词
        List<String> terms = Arrays.asList(query.trim().split("\\s+"));
        
        // 扩展每个词
        List<String> expandedTerms = terms.stream()
            .map(this::expandTerm)
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
            
        // 生成扩展查询
        List<String> expandedQueries = new ArrayList<>();
        expandedQueries.add(query); // 添加原始查询
        
        // 使用同义词替换生成新查询
        for (String term : terms) {
            List<String> synonyms = synonymService.getSynonyms(term);
            if (!synonyms.isEmpty()) {
                for (String synonym : synonyms) {
                    String expandedQuery = query.replace(term, synonym);
                    expandedQueries.add(expandedQuery);
                }
            }
        }
        
        return expandedQueries.stream()
            .distinct()
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, List<String>> expandQueries(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return queries.stream()
            .collect(Collectors.toMap(
                query -> query,
                this::expandQuery,
                (v1, v2) -> v1,
                HashMap::new
            ));
    }
    
    @Override
    public List<String> expandTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> expandedTerms = new ArrayList<>();
        expandedTerms.add(term); // 添加原始词
        
        // 获取同义词
        List<String> synonyms = synonymService.getSynonyms(term);
        expandedTerms.addAll(synonyms);
        
        // 获取相关词
        List<String> relatedTerms = getRelatedTerms(term);
        expandedTerms.addAll(relatedTerms);
        
        return expandedTerms.stream()
            .distinct()
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, List<String>> expandTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return terms.stream()
            .collect(Collectors.toMap(
                term -> term,
                this::expandTerm,
                (v1, v2) -> v1,
                HashMap::new
            ));
    }
    
    @Override
    public double getQueryWeight(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 0.0;
        }
        
        Double weight = redisTemplate.opsForValue().get(QUERY_WEIGHT_KEY + query);
        return weight != null ? weight : 1.0;
    }
    
    @Override
    public double getTermWeight(String term) {
        if (term == null || term.trim().isEmpty()) {
            return 0.0;
        }
        
        Double weight = redisTemplate.opsForValue().get(TERM_WEIGHT_KEY + term);
        return weight != null ? weight : 1.0;
    }
    
    private List<String> getRelatedTerms(String term) {
        // 从用户搜索历史中获取相关词
        Set<ZSetOperations.TypedTuple<String>> relatedTerms = redisTemplate.opsForZSet()
            .reverseRangeWithScores(SUGGESTION_KEY + term, 0, 10);
            
        if (relatedTerms == null || relatedTerms.isEmpty()) {
            return Collections.emptyList();
        }
        
        return relatedTerms.stream()
            .map(ZSetOperations.TypedTuple::getValue)
            .collect(Collectors.toList());
    }
} 