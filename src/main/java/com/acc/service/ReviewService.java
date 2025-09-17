package com.acc.service;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acc.entity.Product;
import com.acc.entity.Review;
import com.acc.entity.Customer;
import com.acc.repository.ProductRepository;
import com.acc.repository.ReviewRepository;
import com.acc.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;


@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository, CustomerRepository customerRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Review submitReview(Long productId, Long customerId, int rating, String reviewText) {
        // Removed the check for existing reviews to allow multiple submissions from the same user.
        // The repository method findByProductIdAndCustomerId is no longer needed in this service.

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

    /**
     * Retrieves a list of reviews for a given product ID.
     * This method is called by the ReviewController.
     */
    public List<Review> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    private void updateProductAverageRating(Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        if (reviews.isEmpty()) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
            product.setAverageRating(0.0);
            productRepository.save(product);
            return;
        }

        double totalRating = reviews.stream()
                                    .mapToDouble(Review::getRating)
                                    .sum();

        double averageRating = totalRating / reviews.size();

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productId));
        
        product.setAverageRating(averageRating);
        productRepository.save(product);
    }
}
