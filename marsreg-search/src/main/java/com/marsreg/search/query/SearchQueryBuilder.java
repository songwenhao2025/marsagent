package com.marsreg.search.query;

import com.marsreg.search.model.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class SearchQueryBuilder {
    
    private static final String HIGHLIGHT_PRE_TAG = "<em>";
    private static final String HIGHLIGHT_POST_TAG = "</em>";
    
    public Query buildQuery(SearchRequest request) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        
        // 构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.multiMatchQuery(request.getQuery())
                .field("title", 3.0f)
                .field("content", 1.0f)
                .type(org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.BEST_FIELDS));
        
        // 添加过滤条件
        addFilters(boolQuery, request);
        
        queryBuilder.withQuery(boolQuery);
        
        // 添加排序条件
        addSorts(queryBuilder, request);
        
        // 添加高亮设置
        addHighlights(queryBuilder);
        
        // 设置分页
        queryBuilder.withPageable(org.springframework.data.domain.PageRequest.of(0, request.getSize()));
        
        return queryBuilder.build();
    }
    
    private void addFilters(BoolQueryBuilder boolQuery, SearchRequest request) {
        // 文档类型过滤
        if (request.getDocumentTypes() != null && !request.getDocumentTypes().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("documentType", request.getDocumentTypes()));
        }
        
        // 时间范围过滤
        if (request.getStartTime() != null || request.getEndTime() != null) {
            BoolQueryBuilder timeQuery = QueryBuilders.boolQuery();
            if (request.getStartTime() != null) {
                timeQuery.must(QueryBuilders.rangeQuery("createTime")
                    .gte(request.getStartTime()));
            }
            if (request.getEndTime() != null) {
                timeQuery.must(QueryBuilders.rangeQuery("createTime")
                    .lte(request.getEndTime()));
            }
            boolQuery.filter(timeQuery);
        }
        
        // 标签过滤
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("tags", request.getTags()));
        }
        
        // 自定义过滤条件
        if (request.getCustomFilters() != null && !request.getCustomFilters().isEmpty()) {
            request.getCustomFilters().forEach((field, value) -> {
                if (value instanceof List) {
                    boolQuery.filter(QueryBuilders.termsQuery(field, (List<?>) value));
                } else {
                    boolQuery.filter(QueryBuilders.termQuery(field, value));
                }
            });
        }
    }
    
    private void addSorts(NativeSearchQueryBuilder queryBuilder, SearchRequest request) {
        if (request.getSortFields() != null && !request.getSortFields().isEmpty()) {
            request.getSortFields().forEach(sortField -> {
                FieldSortBuilder sortBuilder = SortBuilders.fieldSort(sortField.getField())
                    .order(sortField.getOrder() == SearchRequest.SortField.SortOrder.ASC ? 
                        SortOrder.ASC : SortOrder.DESC);
                
                // 处理特殊字段的排序
                switch (sortField.getField()) {
                    case "_score":
                        // 相关度排序
                        sortBuilder.order(SortOrder.DESC);
                        break;
                    case "createTime":
                    case "updateTime":
                        // 时间排序
                        sortBuilder.order(sortField.getOrder() == SearchRequest.SortField.SortOrder.ASC ? 
                            SortOrder.ASC : SortOrder.DESC);
                        break;
                    case "viewCount":
                    case "searchCount":
                        // 热度排序
                        sortBuilder.order(SortOrder.DESC);
                        break;
                }
                
                queryBuilder.withSort(sortBuilder);
            });
        } else {
            // 默认按相关度排序
            queryBuilder.withSort(SortBuilders.scoreSort().order(SortOrder.DESC));
        }
    }
    
    private void addHighlights(NativeSearchQueryBuilder queryBuilder) {
        HighlightBuilder highlightBuilder = new HighlightBuilder()
            .preTags(HIGHLIGHT_PRE_TAG)
            .postTags(HIGHLIGHT_POST_TAG)
            .fragmentSize(150)
            .numOfFragments(3)
            .field("title")
            .field("content");
            
        queryBuilder.withHighlightBuilder(highlightBuilder);
    }
} 