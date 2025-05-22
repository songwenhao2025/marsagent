package com.marsreg.document.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@Table(name = "document_chunk_metadata")
public class DocumentChunkMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "type")
    private String type;

    @Column(name = "description")
    private String description;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", insertable = false, updatable = false)
    private DocumentChunk chunk;

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