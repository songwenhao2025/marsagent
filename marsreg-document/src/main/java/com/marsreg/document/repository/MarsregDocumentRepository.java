package com.marsreg.document.repository;

import com.marsreg.document.model.MarsregDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarsregDocumentRepository extends ElasticsearchRepository<MarsregDocument, String> {
    Optional<MarsregDocument> findById(String id);
    boolean existsById(String id);
    void deleteById(String id);
    MarsregDocument save(MarsregDocument document);
    List<MarsregDocument> saveAll(List<MarsregDocument> documents);
    void deleteAll();
} 