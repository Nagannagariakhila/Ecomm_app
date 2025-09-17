package com.acc.controller;

import com.acc.dto.BulkUploadResponse;
import com.acc.dto.ProductDTO;

import com.acc.elasticsearch.entity.ProductDocument;
import com.acc.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;
   

    @PostMapping
    public ResponseEntity<ProductDTO> addProduct(@Validated @RequestBody ProductDTO productDTO) {
        ProductDTO newProduct = productService.addProduct(productDTO);
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long productId, @Validated @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(productDTO, productId);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProductByProductId(@PathVariable Long productId) {
        ProductDTO product = productService.getProductByProductId(productId);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Product and related cart items deleted successfully.");

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<BulkUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file. Please upload a non-empty CSV file.");
        }

        try {
            BulkUploadResponse response = productService.saveProductsFromCsv(file);

            String message = String.format("CSV import complete. Total records processed: %d, Added: %d, Skipped: %d.",
                response.getTotalRecordsProcessed(), response.getRecordsAdded(), response.getRecordsSkipped());
            response.setMessage(message);
            
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to import products: " + e.getMessage(), e);
        }
    }
    
    @PostMapping("/upload-kafka")
    public ResponseEntity<BulkUploadResponse> uploadProductsToKafka(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new BulkUploadResponse("Please select a file to upload.", 0, 0, 0, null));
        }

        try {
            BulkUploadResponse response = productService.saveProductsFromCsv(file);
            
            return ResponseEntity.accepted().body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BulkUploadResponse("Failed to process file: " + e.getMessage(), 0, 0, 1, null));
        }
    }}
    