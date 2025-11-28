package com.acc.service;

import com.acc.entity.Review;
import java.util.List;

public interface ReviewService {
    Review submitReview(Long productId, Long customerId, int rating, String reviewText);
    List<Review> getReviewsByProductId(Long productId);
    void updateProductAverageRating(Long productId);
}