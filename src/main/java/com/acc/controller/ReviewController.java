package com.acc.controller;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acc.entity.Review;
import com.acc.service.ReviewService;
import com.acc.util.SecurityUtils;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils; 

   
    public ReviewController(ReviewService reviewService, SecurityUtils securityUtils) {
        this.reviewService = reviewService;
        this.securityUtils = securityUtils;
    }

    
    @PostMapping
    public ResponseEntity<String> handleBasePost() {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body("Product ID is required. Please use the endpoint format: /api/reviews/products/{productId}");
    }
    
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')") 

    public ResponseEntity<Review> submitReview(@PathVariable Long productId, @RequestBody Map<String, Object> requestBody) {
        
        
        Long customerId = securityUtils.getAuthenticatedCustomerId();
        Object ratingObject = requestBody.get("rating");
        String reviewText = (String) requestBody.get("reviewText");
        
        if (ratingObject == null || !(ratingObject instanceof Number) || reviewText == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Integer rating = ((Number) ratingObject).intValue(); 
        
        
        Review newReview = reviewService.submitReview(productId, customerId, rating, reviewText);
        
        return new ResponseEntity<>(newReview, HttpStatus.CREATED);
    }
    
    
    @GetMapping("/products/{productId}")
    public ResponseEntity<List<Review>> getReviewsByProduct(@PathVariable Long productId) {
        List<Review> reviews = reviewService.getReviewsByProductId(productId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
}