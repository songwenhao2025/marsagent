package com.marsreg.document.repository;

import com.marsreg.document.entity.DocumentEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentEntity, Long> {
} 