package com.acc.serviceImpl;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acc.entity.Product;
import com.acc.entity.Review;
import com.acc.entity.Customer;
import com.acc.exception.ForbiddenException; // Added this import
import com.acc.repository.ProductRepository;
import com.acc.repository.ReviewRepository;
import com.acc.service.ReviewService;
import com.acc.repository.CustomerRepository;
import com.acc.util.SecurityUtils; // New Import

import jakarta.persistence.EntityNotFoundException;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SecurityUtils securityUtils; 

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository, 
                             CustomerRepository customerRepository, SecurityUtils securityUtils) { 
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.securityUtils = securityUtils;
    }
    
    @Override
    @Transactional
    public Review submitReview(Long productId, Long customerId, int rating, String reviewText) {
        Long authenticatedUserId = securityUtils.getAuthenticatedCustomerId();

        if (!customerId.equals(authenticatedUserId)) {
           
            throw new ForbiddenException("Authorization error: User ID mismatch. Cannot submit review for customer ID: " + customerId);
        }
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with ID: " + customerId));

        Review review = new Review();
        review.setProduct(product);
        review.setCustomer(customer);
        review.setRating(rating);
        review.setReviewText(reviewText);
        review.setCreatedAt(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);
        updateProductAverageRating(productId);

        return savedReview;
    }

    @Override
    public List<Review> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public void updateProductAverageRating(Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));

        if (reviews.isEmpty()) {
            product.setAverageRating(0.0);
            productRepository.save(product);
            return;
        }

        double totalRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .sum();

        double averageRating = totalRating / reviews.size();
        
        product.setAverageRating(averageRating);
        productRepository.save(product);
    }
}