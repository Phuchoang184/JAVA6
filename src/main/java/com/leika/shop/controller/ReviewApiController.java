package com.leika.shop.controller;

import com.leika.shop.dto.*;
import com.leika.shop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    /**
     * GET /api/reviews/product/{productId} – get reviews + average rating (public)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<ReviewSummaryDto>> getProductReviews(
            @PathVariable Integer productId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getProductReviews(productId)));
    }

    /**
     * POST /api/reviews – submit a review (authenticated users only)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDto>> submitReview(
            @RequestBody ReviewRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        ReviewDto review = reviewService.submitReview(email, request);
        return ResponseEntity.ok(ApiResponse.ok("Đánh giá thành công!", review));
    }

    /**
     * GET /api/reviews/check/{productId} – check if current user already reviewed (authenticated)
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> hasReviewed(
            @PathVariable Integer productId,
            Authentication authentication) {
        String email = authentication.getName();
        boolean reviewed = reviewService.hasUserReviewed(email, productId);
        return ResponseEntity.ok(ApiResponse.ok(reviewed));
    }
}
