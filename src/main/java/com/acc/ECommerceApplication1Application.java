package com.acc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.acc.elasticsearch.serviceimpl.ProductDocumentService;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.acc.repository")
@EnableElasticsearchRepositories(basePackages = "com.acc.elasticsearch.repository")

public class ECommerceApplication1Application {

    private static final Logger logger = LoggerFactory.getLogger(ECommerceApplication1Application.class);

    @Autowired
    private ProductDocumentService productDocumentService;

    public static void main(String[] args) {
        SpringApplication.run(ECommerceApplication1Application.class, args);
    }
    
    @Bean
    public CommandLineRunner synchronizeData() {
        return args -> {
            logger.info("Starting synchronization of product data to Elasticsearch...");
            productDocumentService.saveAllProductsToElasticsearch();
            logger.info("Synchronization complete.");
        };
    }
}
