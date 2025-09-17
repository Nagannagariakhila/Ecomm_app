package com.acc.elasticsearch.serviceimpl;

import com.acc.elasticsearch.entity.ProductDocument;
import com.acc.elasticsearch.repository.ProductDocumentRepository;
import com.acc.repository.ProductRepository;
import com.acc.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.stream.StreamSupport;

@Service
public class ProductDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(ProductDocumentService.class);

    @Autowired
    private ProductDocumentRepository productDocumentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public void saveAllProductsToElasticsearch() {
        try {
            List<Product> allProducts = productRepository.findAll();
            List<ProductDocument> elasticProducts = allProducts.stream()
                .map(this::toProductDocument)
                .collect(Collectors.toList());

            productDocumentRepository.saveAll(elasticProducts);
            logger.info("Successfully synchronized {} products to Elasticsearch.", elasticProducts.size());
        } catch (Exception e) {
            logger.error("Error during product synchronization to Elasticsearch: {}", e.getMessage(), e);
        }
    }

    
    public List<ProductDocument> getAllProducts() {
        
        Iterable<ProductDocument> allProductsIterable = productDocumentRepository.findAll();
        List<ProductDocument> allProductsList = StreamSupport.stream(allProductsIterable.spliterator(), false)
                .collect(Collectors.toList());
        
        logger.info("Retrieved {} products from Elasticsearch.", allProductsList.size());
        return allProductsList;
    }

   
    public List<ProductDocument> searchProducts(String query) {
        PageRequest pageRequest = PageRequest.of(0, 100);
        return productDocumentRepository.findByNameOrDescription(query, query, pageRequest).getContent();
    }

    
    private ProductDocument toProductDocument(Product product) {
        ProductDocument elasticProduct = new ProductDocument();
        elasticProduct.setId(product.getId());
        elasticProduct.setName(product.getName());
        elasticProduct.setDescription(product.getDescription());
        if (product.getCategory() != null) {
            elasticProduct.setCategory(product.getCategory().name());
        }
        elasticProduct.setPrice(product.getPrice());
        elasticProduct.setStockQuantity(product.getStockQuantity());
        elasticProduct.setDiscountPercentage(product.getDiscountPercentage());
        return elasticProduct;
    }
}
