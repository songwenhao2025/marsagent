package com.marsreg.search.service.impl;

import com.marsreg.search.model.SearchSuggestion;
import com.marsreg.search.repository.DocumentIndexRepository;
import com.marsreg.search.service.SearchSuggestionService;
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

    private static final String HOT_KEYWORDS_KEY = "search:hot:keywords";
    private static final int MAX_HOT_KEYWORDS = 100;

    @Override
    @Cacheable(value = "searchSuggestions", key = "#prefix + ':' + #size")
    public List<SearchSuggestion> getSuggestions(String prefix, int size) {
        List<SearchSuggestion> suggestions = new ArrayList<>();
        
        // 构建建议查询
        SuggestBuilder suggestBuilder = new SuggestBuilder()
            .addSuggestion("title_suggest",
                SuggestBuilders.completionSuggestion("title")
                    .prefix(prefix)
                    .size(size))
            .addSuggestion("tag_suggest",
                SuggestBuilders.completionSuggestion("tags")
                    .prefix(prefix)
                    .size(size));
        
        // 执行建议查询
        SearchResponse response = elasticsearchOperations.suggest(suggestBuilder, DocumentIndex.class);
        
        // 处理标题建议
        CompletionSuggestion titleSuggestion = response.getSuggest().getSuggestion("title_suggest");
        titleSuggestion.getEntries().forEach(entry -> {
            entry.getOptions().forEach(option -> {
                suggestions.add(SearchSuggestion.builder()
                    .text(option.getText().string())
                    .type(SearchSuggestion.SuggestionType.DOCUMENT)
                    .score(option.getScore())
                    .extraInfo(option.getHit().getId())
                    .build());
            });
        });
        
        // 处理标签建议
        CompletionSuggestion tagSuggestion = response.getSuggest().getSuggestion("tag_suggest");
        tagSuggestion.getEntries().forEach(entry -> {
            entry.getOptions().forEach(option -> {
                suggestions.add(SearchSuggestion.builder()
                    .text(option.getText().string())
                    .type(SearchSuggestion.SuggestionType.TAG)
                    .score(option.getScore())
                    .build());
            });
        });
        
        // 按分数排序并限制结果数量
        return suggestions.stream()
            .sorted(Comparator.comparing(SearchSuggestion::getScore).reversed())
            .limit(size)
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "hotSuggestions", key = "#size")
    public List<SearchSuggestion> getHotSuggestions(int size) {
        // 从Redis获取热门关键词
        Set<String> hotKeywords = redisTemplate.opsForZSet()
            .reverseRange(HOT_KEYWORDS_KEY, 0, size - 1);
        
        if (hotKeywords == null || hotKeywords.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 获取关键词的分数
        return hotKeywords.stream()
            .map(keyword -> {
                Double score = redisTemplate.opsForZSet().score(HOT_KEYWORDS_KEY, keyword);
                return SearchSuggestion.builder()
                    .text(keyword)
                    .type(SearchSuggestion.SuggestionType.KEYWORD)
                    .score(score != null ? score.floatValue() : 0.0f)
                    .build();
            })
            .collect(Collectors.toList());
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
} 