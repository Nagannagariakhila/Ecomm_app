package com.acc.elasticsearch.repository;

import com.acc.elasticsearch.entity.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, Long> {

    Page<ProductDocument> findByNameOrDescription(String name, String description, Pageable pageable);

    Page<ProductDocument> findByCategory(String category, Pageable pageable);

    
    List<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<ProductDocument> findByCategoryAndPriceBetween(String category, BigDecimal minPrice, BigDecimal maxPrice);

    
    List<ProductDocument> findByStockQuantityGreaterThan(Integer stockQuantity);
}
