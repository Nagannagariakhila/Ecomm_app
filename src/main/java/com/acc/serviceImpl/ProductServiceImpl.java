package com.acc.serviceImpl;

import com.acc.dto.BulkUploadResponse;
import com.acc.dto.ProductDTO;
import com.acc.elasticsearch.entity.ProductDocument;
import com.acc.entity.Category;
import com.acc.entity.Product;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.CartItemRepository;
import com.acc.repository.ProductRepository;
import com.acc.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PRODUCT_UPLOAD_TOPIC = "bulk-upload.products";


    private ProductDTO convertToDto(Product product) {
        log.debug("Converting product entity to DTO: {}", product.getId());
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);
        dto.setImages(product.getImageUrlsList());
        dto.setDiscountedPrice(product.getDiscountedPrice());
        return dto;
    }

    private Product convertToEntity(ProductDTO productDto) {
        log.debug("Converting product DTO to entity: {}", productDto.getName());
        Product product = new Product();
        BeanUtils.copyProperties(productDto, product);
        product.setImageUrlsList(productDto.getImages());
        return product;
    }

    @Override
    @Transactional
    public ProductDTO addProduct(ProductDTO productDto) {
        log.info("Adding a new product: {}", productDto.getName());
        Product product = convertToEntity(productDto);
        product.setId(null);
        Product savedProduct = productRepository.save(product);
        log.info("Successfully added product with ID: {}", savedProduct.getId());
        return convertToDto(savedProduct);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(ProductDTO productDto, Long productId) {
        log.info("Attempting to update product with ID: {}", productId);
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product with ID {} not found for update.", productId);
                    return new ResourceNotFoundException("Product", "ID", productId);
                });
        BeanUtils.copyProperties(productDto, existingProduct);
        existingProduct.setImageUrlsList(productDto.getImages());
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Successfully updated product with ID: {}", updatedProduct.getId());
        return convertToDto(updatedProduct);
    }

    @Override
    public ProductDTO getProductByProductId(Long productId) {
        log.info("Fetching product with ID: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product with ID {} not found.", productId);
                    return new ResourceNotFoundException("Product", "ID", productId);
                });
        log.debug("Found product: {}", product.getName());
        return convertToDto(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        log.warn("Attempting to delete product with ID: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product with ID {} not found for deletion.", productId);
                    return new ResourceNotFoundException("Product", "ID", productId);
                });
        cartItemRepository.deleteByProduct(product);
        productRepository.delete(product);
        log.info("Successfully deleted product with ID: {}", productId);
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        log.info("Fetching all active products.");
        List<Product> products = productRepository.findByActiveTrue();
        log.info("Found {} active products.", products.size());
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BulkUploadResponse saveProductsFromCsv(MultipartFile file) throws IOException {
        log.info("Starting bulk product upload from CSV file: {}", file.getOriginalFilename());
        Set<String> existingUniqueKeys = productRepository.findAll().stream()
                .map(p -> (p.getName().trim() + "::" + p.getCategory().name()).toLowerCase())
                .collect(Collectors.toSet());
        log.debug("Loaded {} existing product unique keys from the database.", existingUniqueKeys.size());

        int totalRecords = 0;
        int publishedCount = 0;
        int skippedCount = 0;

        try (
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CSVReader csvReader = new CSVReader(reader)
        ) {
            String[] row;
            csvReader.readNext(); 
            log.info("Skipped CSV header row.");

            while ((row = csvReader.readNext()) != null) {
                if (row.length < 8 || row[0].trim().isEmpty()) {
                    log.warn("Skipping empty or malformed row: {}", Arrays.toString(row));
                    continue;
                }

                totalRecords++;

                try {
                    String name = row[0].trim();
                    String categoryStr = row[6].trim().toUpperCase();
                    log.debug("Processing record #{}: {}", totalRecords, name);

                    Category category;
                    try {
                        category = Category.valueOf(categoryStr);
                    } catch (IllegalArgumentException e) {
                        log.error("Skipping record due to invalid category '{}': {}", categoryStr, Arrays.toString(row));
                        skippedCount++;
                        continue;
                    }

                    String uniqueKey = (name + "::" + category.name()).toLowerCase();
                    if (existingUniqueKeys.contains(uniqueKey)) {
                        log.warn("Skipping duplicate record based on name and category: {}", name);
                        skippedCount++;
                        continue;
                    }

                    String priceStr = row[2].trim().replaceAll("[^\\d.]", "");
                    if (priceStr.isEmpty()) {
                        log.error("Skipping record due to invalid price: {}", Arrays.toString(row));
                        skippedCount++;
                        continue;
                    }

                    ProductDTO productDto = new ProductDTO();
                    productDto.setName(name);
                    productDto.setDescription(row[1].trim());
                    productDto.setPrice(new BigDecimal(priceStr));

                    String discountStr = row[3].trim();
                    double discount = discountStr.isEmpty() ? 0.0 : Double.parseDouble(discountStr);
                    productDto.setDiscountPercentage(discount);

                    String stockStr = row[4].trim();
                    productDto.setStockQuantity(Integer.parseInt(stockStr));

                    List<String> images = Arrays.stream(row[5].trim().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    productDto.setImages(images);
                    
                    productDto.setCategory(category);
                    productDto.setActive("ACTIVE".equalsIgnoreCase(row[7].trim()));

                    String productJson = objectMapper.writeValueAsString(productDto);
                    kafkaTemplate.send(PRODUCT_UPLOAD_TOPIC, name, productJson);
                    
                    log.info("Product '{}' published to Kafka topic '{}' successfully.", name, PRODUCT_UPLOAD_TOPIC);

                    publishedCount++;
                    existingUniqueKeys.add(uniqueKey);
                } catch (Exception e) {
                    log.error("Skipping record due to parsing error: {} - {}", Arrays.toString(row), e.getMessage(), e);
                    skippedCount++;
                }
            }

        } catch (CsvValidationException e) {
            log.error("CSV validation error occurred during bulk upload.", e);
            throw new IOException("CSV parsing error", e);
        }

        String message = String.format(
                "CSV import complete. Total records processed: %d, Published to Kafka: %d, Skipped: %d.",
                totalRecords, publishedCount, skippedCount
        );
        log.info(message);

        return new BulkUploadResponse(message, totalRecords, publishedCount, skippedCount, null);
    }

	@Override
	public Optional<Product> findById(Long id) {
		
		return Optional.empty();
	}

	@Override
	public List<Product> findAll() {
		return null;
	}

	

	@Override
	public Product save(Product product) {
		return null;
	}

	@Override
	public List<ProductDocument> searchProducts(String query) {
		return null;
	}

	@Override
	public List<ProductDocument> searchProductsByCategory(String categoryName, String query) {
		return null;
	}

	

	
}