package com.marsreg.search.model;

import com.marsreg.common.model.BaseSearchResponse;
import com.marsreg.common.model.Document;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentSearchResponse extends BaseSearchResponse {
    private List<Document> documents;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;
} 