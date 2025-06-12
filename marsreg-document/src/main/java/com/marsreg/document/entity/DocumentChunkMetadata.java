package com.marsreg.document.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
@Entity
@Table(name = "document_chunk_metadata")
public class DocumentChunkMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chunk_id")
    private DocumentChunk chunk;

    @Column(name = "key")
    private String key;

    @Column(name = "value")
    private String value;

    @Column(name = "type")
    private String type;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 将元数据转换为Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("key", key);
        map.put("value", value);
        map.put("type", type);
        map.put("description", description);
        return map;
    }

    /**
     * 从Map创建元数据
     */
    public static DocumentChunkMetadata fromMap(Map<String, Object> map) {
        DocumentChunkMetadata metadata = new DocumentChunkMetadata();
        metadata.setKey((String) map.get("key"));
        metadata.setValue((String) map.get("value"));
        metadata.setType((String) map.get("type"));
        metadata.setDescription((String) map.get("description"));
        return metadata;
    }
} 