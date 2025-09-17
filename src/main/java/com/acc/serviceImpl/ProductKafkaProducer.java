package com.acc.serviceImpl;

import com.acc.dto.ProductDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ProductKafkaProducer.class);
    private static final String PRODUCT_UPLOAD_TOPIC = "bulk-upload.products";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProductKafkaProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    
    public void sendProduct(ProductDTO productDto) throws JsonProcessingException {
        try {
            String productJson = objectMapper.writeValueAsString(productDto);
            kafkaTemplate.send(PRODUCT_UPLOAD_TOPIC, productDto.getName(), productJson);
            log.info("Published product to Kafka topic '{}' with key: {}", PRODUCT_UPLOAD_TOPIC, productDto.getName());
        } catch (JsonProcessingException e) {
            log.error("Failed to convert ProductDTO to JSON for product: {}", productDto.getName(), e);
            throw e;
        }
    }
}