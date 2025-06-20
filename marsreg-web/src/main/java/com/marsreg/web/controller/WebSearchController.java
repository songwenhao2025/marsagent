package com.marsreg.web.controller;

import com.marsreg.search.service.DocumentSearchFacade;
import com.marsreg.common.dto.DocumentQueryDTO;
import com.marsreg.search.model.DocumentSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class WebSearchController {

    @Autowired
    private DocumentSearchFacade documentSearchFacade;

    @GetMapping
    public ResponseEntity<DocumentSearchResponse> search(@RequestParam String q) {
        DocumentQueryDTO queryDTO = new DocumentQueryDTO();
        queryDTO.setKeyword(q);
        DocumentSearchResponse response = (DocumentSearchResponse) documentSearchFacade.search(queryDTO, Pageable.ofSize(10));
        return ResponseEntity.ok(response);
    }
} 