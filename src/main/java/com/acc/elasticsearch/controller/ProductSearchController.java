package com.acc.elasticsearch.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.acc.elasticsearch.entity.ProductDocument;
import com.acc.elasticsearch.serviceimpl.ProductDocumentService;

@RestController
@RequestMapping("/api/products")
public class ProductSearchController {

    @Autowired
    private ProductDocumentService productDocumentService;

    @GetMapping("/search")
    public List<ProductDocument> searchProducts(@RequestParam String query) {
        
        return productDocumentService.searchProducts(query);
    }
    @GetMapping("/products/all")
    public List<ProductDocument> getAllProducts() {
        return productDocumentService.getAllProducts();
    }
}
