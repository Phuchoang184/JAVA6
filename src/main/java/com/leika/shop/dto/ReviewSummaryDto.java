package com.leika.shop.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryDto {
    private Double averageRating;
    private Integer totalReviews;
    private List<ReviewDto> reviews;
}
