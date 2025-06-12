package com.marsreg.search.service;

import com.marsreg.common.dto.DocumentQueryDTO;
import com.marsreg.common.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentSearchFacade {
    Page<Document> search(DocumentQueryDTO queryDTO, Pageable pageable);
}