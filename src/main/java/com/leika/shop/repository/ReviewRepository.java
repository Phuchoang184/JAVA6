package com.leika.shop.repository;

import com.leika.shop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByProductProductIdAndIsApprovedTrueOrderByCreatedAtDesc(Integer productId);

    boolean existsByProductProductIdAndUserUserId(Integer productId, Integer userId);

    Optional<Review> findByProductProductIdAndUserUserId(Integer productId, Integer userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId AND r.isApproved = true")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.productId = :productId AND r.isApproved = true")
    Integer countApprovedByProductId(@Param("productId") Integer productId);
}
