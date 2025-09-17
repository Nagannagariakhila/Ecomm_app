package com.acc.serviceImpl;

import com.acc.dto.ProductDTO;
import com.acc.entity.Product;
import com.acc.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.Optional;

@Service
public class ProductKafkaConsumer {

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "bulk-upload.products", groupId = "product-processor-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void processProduct(String message, Acknowledgment acknowledgment) {
        System.out.println("Received message from Kafka: " + message);

        try {
            ProductDTO productDto = objectMapper.readValue(message, ProductDTO.class);
            
            
            String productName = productDto.getName().trim();
            productDto.setName(productName);
            
           
            Optional<Product> existingProduct = productRepository.findByName(productName);
            
            if (existingProduct.isPresent()) {
                System.out.println("Skipped duplicate record: A product with name '" + productName + "' already exists.");
               
                acknowledgment.acknowledge();
                return;
            }

            Product newProduct = new Product();
            BeanUtils.copyProperties(productDto, newProduct);
            newProduct.setImageUrlsList(productDto.getImages());

            productRepository.save(newProduct);
            
            System.out.println("New product saved successfully: " + productDto.getName());
            
            
            acknowledgment.acknowledge();
            
        } catch (IOException e) {
            System.err.println("Error deserializing message, skipping: " + message + " - " + e.getMessage());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            System.err.println("Error processing product from Kafka, will retry: " + message + " - " + e.getMessage());
           
        }
    }
}