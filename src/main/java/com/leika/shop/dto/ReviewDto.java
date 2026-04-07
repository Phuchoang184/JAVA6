package com.leika.shop.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Integer reviewId;
    private Integer productId;
    private Integer userId;
    private String userFullName;
    private String userAvatarUrl;
    private Integer rating;
    private String title;
    private String body;
    private LocalDateTime createdAt;
}
