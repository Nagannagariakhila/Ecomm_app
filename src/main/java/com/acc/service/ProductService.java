package com.acc.service;
import com.acc.dto.ProductDTO;
import java.util.List;
public interface ProductService {
    ProductDTO addProduct(ProductDTO productDto);
    ProductDTO updateProduct(Long id, ProductDTO productDto); 
    ProductDTO getProductById(Long id); 
    void deleteProduct(Long productId); 
    List<ProductDTO> getAllProducts();
	void deleteProduct(int productId);
	ProductDTO getProductByProductId(int productId);
	ProductDTO updateProduct(ProductDTO productDTO, int productId);
	ProductDTO updateProduct(ProductDTO productDto, Long productId);
	ProductDTO getProductByProductId(Long productId);
	ProductDTO createProduct(ProductDTO productDTO);
}