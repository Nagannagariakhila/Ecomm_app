package com.acc.serviceImpl;

import com.acc.dto.ProductDTO;
import com.acc.entity.Product;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.CartItemRepository;
import com.acc.repository.ProductRepository;
import com.acc.service.ProductService; // Ensure this is the correct interface
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    private ProductDTO convertToDto(Product product) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);
        dto.setImages(product.getImageUrlsList());  

        return dto;
    }
    private Product convertToEntity(ProductDTO productDto) {
        Product product = new Product();
        product.setId(productDto.getId()); 
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setStockQuantity(productDto.getStockQuantity());
        product.setCategory(productDto.getCategory());
        product.setImageUrlsList(productDto.getImages()); 
        return product;
    }

    @Override
    @Transactional
    public ProductDTO addProduct(ProductDTO productDto) {
        Product product = convertToEntity(productDto);
        product.setId(null); 
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(ProductDTO productDto, Long productId) {
        Product existingProduct = productRepository.findById(productId) 
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", productId));

        existingProduct.setName(productDto.getName());
        existingProduct.setDescription(productDto.getDescription());
        existingProduct.setPrice(productDto.getPrice());
        existingProduct.setStockQuantity(productDto.getStockQuantity());
        existingProduct.setCategory(productDto.getCategory());
        existingProduct.setImageUrlsList(productDto.getImages()); 

        Product updatedProduct = productRepository.save(existingProduct);
        return convertToDto(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", productId));
        return convertToDto(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", productId));

        product.setActive(false); 
        productRepository.save(product); 
        }



    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {

        return productRepository.findByActiveTrue().stream()
    			
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

	@Override
	public ProductDTO updateProduct(Long id, ProductDTO productDto) {
		return null;
	}

	@Override
	public ProductDTO getProductById(Long id) {
		return null;
	}

	@Override
	public void deleteProduct(int productId) {
		
	}

	@Override
	public ProductDTO getProductByProductId(int productId) {
		return null;
	}

	@Override
	public ProductDTO updateProduct(ProductDTO productDTO, int productId) {
		return null;
	}

	@Override
	public ProductDTO createProduct(ProductDTO productDTO) {
		return null;
	}
}