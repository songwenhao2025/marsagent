package com.marsreg.document.repository;

import com.marsreg.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("SELECT d FROM Document d WHERE " +
           "(:keyword IS NULL OR d.title LIKE %:keyword% OR d.description LIKE %:keyword% OR d.content LIKE %:keyword%) AND " +
           "(:category IS NULL OR d.category = :category) AND " +
           "(:author IS NULL OR d.author = :author) AND " +
           "(:language IS NULL OR d.language = :language) AND " +
           "(:source IS NULL OR d.source = :source) AND " +
           "(:tags IS NULL OR d.tags LIKE %:tags%)")
    Page<Document> search(@Param("keyword") String keyword,
                         @Param("category") String category,
                         @Param("author") String author,
                         @Param("language") String language,
                         @Param("source") String source,
                         @Param("tags") String tags,
                         Pageable pageable);
} 