package com.marsreg.search.repository;

import com.marsreg.search.model.DocumentIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, String> {
    // 可以添加自定义查询方法
} 