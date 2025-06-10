package com.marsreg.document.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Data
@Accessors(chain = true)
public class DocumentSearchRequest {
    private String query;
    private int page = 0;
    private int size = 10;
    private String type;
    private String status;

    public Pageable getPageable() {
        return PageRequest.of(page, size);
    }
} 