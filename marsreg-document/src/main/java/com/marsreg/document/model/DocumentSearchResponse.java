package com.marsreg.document.model;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;

@Data
@Accessors(chain = true)
public class DocumentSearchResponse {
    private List<MarsregDocument> documents;
    private long total;
    private int page;
    private int size;
} 