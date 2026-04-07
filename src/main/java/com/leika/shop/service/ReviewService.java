package com.leika.shop.service;

import com.leika.shop.dto.*;
import com.leika.shop.entity.*;
import com.leika.shop.exception.BusinessException;
import com.leika.shop.exception.ResourceNotFoundException;
import com.leika.shop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get all approved reviews + average rating for a product
     */
    public ReviewSummaryDto getProductReviews(Integer productId) {
        List<Review> reviews = reviewRepository
                .findByProductProductIdAndIsApprovedTrueOrderByCreatedAtDesc(productId);

        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        Integer count = reviewRepository.countApprovedByProductId(productId);

        return ReviewSummaryDto.builder()
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .totalReviews(count != null ? count : 0)
                .reviews(reviews.stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    /**
     * Submit a review – one per user per product
     */
    @Transactional
    public ReviewDto submitReview(String userEmail, ReviewRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // One review per user per product
        if (reviewRepository.existsByProductProductIdAndUserUserId(
                product.getProductId(), user.getUserId())) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này rồi");
        }

        // Validate rating
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BusinessException("Đánh giá phải từ 1 đến 5 sao");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .isApproved(true) // auto-approve for now
                .build();

        review = reviewRepository.save(review);
        return toDto(review);
    }

    /**
     * Check if user already reviewed a product
     */
    public boolean hasUserReviewed(String userEmail, Integer productId) {
        return userRepository.findByEmail(userEmail)
                .map(user -> reviewRepository.existsByProductProductIdAndUserUserId(
                        productId, user.getUserId()))
                .orElse(false);
    }

    private ReviewDto toDto(Review r) {
        return ReviewDto.builder()
                .reviewId(r.getReviewId())
                .productId(r.getProduct().getProductId())
                .userId(r.getUser().getUserId())
                .userFullName(r.getUser().getFullName())
                .userAvatarUrl(r.getUser().getAvatarUrl())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
