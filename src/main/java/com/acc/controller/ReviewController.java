package com.acc.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acc.entity.Review;
import com.acc.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/products/{productId}")
    public ResponseEntity<Review> submitReview(@PathVariable Long productId, @RequestBody Map<String, Object> requestBody) {
        // In a real application, you would get the customerId from the authenticated user's session.
        // For demonstration, we'll use a hardcoded value.
        // Example with Spring Security: Long customerId = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        Long customerId = 1L;

        // Extract rating and review text from the incoming JSON body
        Integer rating = (Integer) requestBody.get("rating");
        String reviewText = (String) requestBody.get("reviewText");

        if (rating == null || reviewText == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Review newReview = reviewService.submitReview(productId, customerId, rating, reviewText);
        return new ResponseEntity<>(newReview, HttpStatus.CREATED);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<Review>> getReviewsByProduct(@PathVariable Long productId) {
        // Assuming your ReviewService has a method to get reviews by product ID
        List<Review> reviews = reviewService.getReviewsByProductId(productId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
}
