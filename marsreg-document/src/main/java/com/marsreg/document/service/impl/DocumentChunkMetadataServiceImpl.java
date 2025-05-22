package com.marsreg.document.service.impl;

import com.marsreg.document.entity.DocumentChunkMetadata;
import com.marsreg.document.repository.DocumentChunkMetadataRepository;
import com.marsreg.document.service.DocumentChunkMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkMetadataServiceImpl implements DocumentChunkMetadataService {

    private final DocumentChunkMetadataRepository metadataRepository;

    @Override
    @Transactional
    public DocumentChunkMetadata save(DocumentChunkMetadata metadata) {
        return metadataRepository.save(metadata);
    }

    @Override
    @Transactional
    public List<DocumentChunkMetadata> saveAll(List<DocumentChunkMetadata> metadataList) {
        return metadataRepository.saveAll(metadataList);
    }

    @Override
    @Cacheable(value = "chunkMetadata", key = "#chunkId")
    public List<DocumentChunkMetadata> getByChunkId(Long chunkId) {
        return metadataRepository.findByChunkId(chunkId);
    }

    @Override
    @Cacheable(value = "documentMetadata", key = "#documentId")
    public List<DocumentChunkMetadata> getByDocumentId(Long documentId) {
        return metadataRepository.findByDocumentId(documentId);
    }

    @Override
    @Cacheable(value = "chunkMetadata", key = "#chunkId + '_' + #key")
    public DocumentChunkMetadata getByChunkIdAndKey(Long chunkId, String key) {
        return metadataRepository.findByChunkIdAndKey(chunkId, key);
    }

    @Override
    @Cacheable(value = "documentMetadata", key = "#documentId + '_' + #key")
    public List<DocumentChunkMetadata> getByDocumentIdAndKey(Long documentId, String key) {
        return metadataRepository.findByDocumentIdAndKey(documentId, key);
    }

    @Override
    @Cacheable(value = "chunkMetadataMap", key = "#chunkId")
    public Map<String, Object> getMetadataMapByChunkId(Long chunkId) {
        List<Map<String, Object>> metadataList = metadataRepository.findMetadataMapByChunkId(chunkId);
        return convertToMap(metadataList);
    }

    @Override
    @Cacheable(value = "documentMetadataMap", key = "#documentId")
    public Map<String, Object> getMetadataMapByDocumentId(Long documentId) {
        List<Map<String, Object>> metadataList = metadataRepository.findMetadataMapByDocumentId(documentId);
        return convertToMap(metadataList);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"chunkMetadata", "chunkMetadataMap"}, allEntries = true)
    public void delete(DocumentChunkMetadata metadata) {
        metadataRepository.delete(metadata);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"chunkMetadata", "chunkMetadataMap"}, allEntries = true)
    public void deleteByChunkId(Long chunkId) {
        metadataRepository.deleteByChunkId(chunkId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"documentMetadata", "documentMetadataMap"}, allEntries = true)
    public void deleteByDocumentId(Long documentId) {
        metadataRepository.deleteByDocumentId(documentId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"chunkMetadata", "chunkMetadataMap"}, allEntries = true)
    public void deleteByChunkIdAndKey(Long chunkId, String key) {
        metadataRepository.deleteByChunkIdAndKey(chunkId, key);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"documentMetadata", "documentMetadataMap"}, allEntries = true)
    public void deleteByDocumentIdAndKey(Long documentId, String key) {
        metadataRepository.deleteByDocumentIdAndKey(documentId, key);
    }

    /**
     * 将元数据列表转换为Map
     */
    private Map<String, Object> convertToMap(List<Map<String, Object>> metadataList) {
        return metadataList.stream()
                .collect(Collectors.toMap(
                        map -> (String) map.get("key"),
                        map -> map.get("value"),
                        (v1, v2) -> v2,
                        HashMap::new
                ));
    }
} 