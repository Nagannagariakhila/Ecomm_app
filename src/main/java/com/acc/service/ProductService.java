package com.acc.service;

import com.acc.dto.BulkUploadResponse;
import com.acc.dto.ProductDTO;
import com.acc.entity.Product;
import com.acc.elasticsearch.entity.ProductDocument;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    ProductDTO addProduct(ProductDTO productDto);
    ProductDTO updateProduct(ProductDTO productDto, Long productId);
    ProductDTO getProductByProductId(Long productId);
    void deleteProduct(Long productId);
    List<ProductDTO> getAllProducts();
    List<ProductDocument> searchProducts(String query);
    List<ProductDocument> searchProductsByCategory(String categoryName, String query);
    BulkUploadResponse saveProductsFromCsv(MultipartFile file) throws IOException;
    Optional<Product> findById(Long id);
    List<Product> findAll();
    Product save(Product product);
}
